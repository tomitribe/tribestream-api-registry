/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.tomitribe.tribestream.registryng.service.search;

import com.tomitribe.tribestream.registryng.domain.CloudItem;
import com.tomitribe.tribestream.registryng.domain.SearchPage;
import com.tomitribe.tribestream.registryng.domain.SearchResult;
import com.tomitribe.tribestream.registryng.entities.Endpoint;
import com.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import com.tomitribe.tribestream.registryng.repository.Repository;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.openejb.loader.SystemInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomitribe.util.Duration;
import org.tomitribe.util.Files;
import org.tomitribe.util.IO;
import org.tomitribe.util.Join;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.apache.lucene.facet.DrillDownQuery.term;

@Startup
@Singleton
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SearchEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngine.class);

    private static final String DEPLOYABLE_ID_FIELD = "deployableId";
    private static final String APPLICATION_ID_FIELD = "applicationId";
    private static final String ENDPOINT_ID_FIELD = "endpointId";
    public static final String VERB = "verb";
    private static final String HTTP_METHOD = "httpMethod";
    private static final String PATH = "path";
    private static final String DOC = "doc";
    private static final String SUMMARY = "summary";

    private final String directoryPath;
    private final Duration duration;
    private final Repository repository;

    @Resource
    private SessionContext ctx;

    private volatile Directory indexDir;
    private volatile Directory indexFacetsDir;
//    private OnTheFlyIndexation observer;
    private DirectoryReader reader;
    private IndexSearcher searcher;
    private IndexWriter writer;
    private DirectoryTaxonomyWriter taxonomyWriter;
    private DirectoryTaxonomyReader taxonomyReader;
    private FacetsConfig facetsConfig = new FacetsConfig();

    private final BlockingQueue<Future<?>> pendingAdd = new LinkedBlockingQueue<>();
    private final BlockingQueue<Future<?>> pendingRemove = new LinkedBlockingQueue<>();
    private volatile boolean closed;
    private Analyzer analyzer;

    @Inject
    public SearchEngine(//@Config(name = {"tribe.registry.search.lucene.directory", "registry.search.lucene.directory"}, defaultValue = "") final String directoryPath,
                            //@Config(name = {"tribe.registry.search.lucene.future-timeout", "registry.search.lucene.future-timeout"}, defaultValue = "10 seconds")
                            //final Duration timeout,
                            final Repository repository) {
        this.directoryPath = "";//directoryPath;
        this.duration = new Duration(10, TimeUnit.SECONDS); // timeout;
        this.repository = repository;
    }

    protected SearchEngine() {
        this(null);
    }

    public SearchPage search(final SearchRequest request) {
        final int pageSize = request.getCount();
        try {
            final IndexSearcher indexSearcher = searcher();
            if (indexSearcher == null) { // no index
                return new SearchPage(
                        new ArrayList<SearchResult>(), 0, 0,
                        new ArrayList<CloudItem>(), new ArrayList<CloudItem>(),
                        new ArrayList<CloudItem>(), new ArrayList<CloudItem>());
            }

            final BooleanQuery query = new BooleanQuery();
            for (final Query q : asList(
                    createQuery(request.getQuery()),
                    drillDownFor("category", request.getCategories()),
                    drillDownFor("tag", request.getTags()),
                    drillDownFor("role", request.getRoles())
            )) {
                if (q != null) {
                    query.add(q, BooleanClause.Occur.MUST);
                }
            }

            if (request.getApps() != null && request.getApps().size() > 0) {
                BooleanQuery appFilter = new BooleanQuery();
                appFilter.setMinimumNumberShouldMatch(1);
                for (String s : request.getApps()) {
                    appFilter.add(new TermQuery(new Term("context", s)), BooleanClause.Occur.SHOULD);
                    appFilter.add(new TermQuery(new Term("applicationNameVersion", s)), BooleanClause.Occur.SHOULD);
                }
                query.add(appFilter, BooleanClause.Occur.MUST);
            }

            final TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create((request.getPage() + 1) * pageSize, true);
            final FacetsCollector facetsCollector = new FacetsCollector(true);
            indexSearcher.search(query, MultiCollector.wrap(topScoreDocCollector, facetsCollector));
            final TopDocs searchResult = topScoreDocCollector.topDocs(request.getPage() * pageSize, pageSize);

            final Facets facets = new FastTaxonomyFacetCounts(taxonomyReader, facetsConfig, facetsCollector);
            final FacetResult category = facets.getTopChildren(10, "category");
            final FacetResult tag = facets.getTopChildren(10, "tag");
            final FacetResult role = facets.getTopChildren(10, "role");
            final FacetResult context = facets.getTopChildren(10, "context");

            final List<SearchResult> results = new LinkedList<>();
            for (final ScoreDoc sd : searchResult.scoreDocs) {
                final Document doc = searcher.doc(sd.doc);
//                final Endpoint endpoint = findEndpoint(doc);
//                final String applicationId = Repository.getApplicationId(endpoint.getApplication().getSwagger());
                results.add(new SearchResult(
                        doc.get(ENDPOINT_ID_FIELD),
                        doc.get(APPLICATION_ID_FIELD),
                        doc.get(ENDPOINT_ID_FIELD),
                        doc.get("applicationNameVersion"),
                        doc.get(VERB),
                        doc.get(PATH),
                        doc.get(SUMMARY),
                        new HashSet<String>(), // Consumes,
                        new HashSet<String>(), // Produces,
                        false, //!endpoint.getSecurity().getRolesAllowed().isEmpty(),
                        false, //rateLimited,
                        sd.score));
            }
            return new SearchPage(
                    results, searchResult.totalHits, request.getPage(),
                    toCloudItems(context), toCloudItems(category), toCloudItems(tag), toCloudItems(role));
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Collection<CloudItem> toCloudItems(final FacetResult result) {
        final List<CloudItem> cloudItems = new ArrayList<>();
        if (result != null) {
            for (final LabelAndValue labelAndValue : result.labelValues) {
                cloudItems.add(new CloudItem(labelAndValue.label, labelAndValue.value.intValue()));
            }
        }
        return cloudItems;
    }

    private BooleanQuery drillDownFor(final String key, final List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        final BooleanQuery query = new BooleanQuery(true);
        for (final String value : values) { // DrillDownQuery uses SHOULD here, we want MUST
            final String indexFieldName = facetsConfig.getDimConfig(key).indexFieldName;
            query.add(new TermQuery(term(indexFieldName, key, value)), BooleanClause.Occur.MUST);
        }
        return query;
    }

    private Query createQuery(final String query) throws ParseException {
        if (query == null || query.isEmpty()) {
            return null;
        }
        final QueryParser queryParser = new ComplexPhraseQueryParser("search", analyzer);
        queryParser.setAllowLeadingWildcard(true);
        queryParser.setDefaultOperator(QueryParser.Operator.AND);


        final int pathStart = query.indexOf("path:");
        if (pathStart >= 0) {
            final int pathEnd = Math.max(query.indexOf(" ", pathStart), query.length());
            // QueryParser.escape() escapes * etc so just escapes / for now
            final String escapedPath = "path:" + query.substring(pathStart + "path:".length(), pathEnd).replace("/", "\\/") + query.substring(pathEnd);
            return queryParser.parse(escapedPath);
        }

        return queryParser.parse(query);
    }

    // returns null if doesn't exist anymore (== undeployed)
    private Endpoint findEndpoint(final Document doc) {
        return repository.findEndpointById(doc.get(ENDPOINT_ID_FIELD));
    }

    @PostConstruct
    private void createIndex() {
        if (directoryPath.isEmpty()) {
            indexDir = new RAMDirectory();
            indexFacetsDir = new RAMDirectory();
            LOGGER.info("Using Lucene RAMDirectory, it is recommended to set registry.search.lucene.directory property to use a disk index");
        } else {
            File index = new File(directoryPath, "index");
            File facet = new File(directoryPath, "facet");
            if (!index.isAbsolute()) {
                index = new File(SystemInstance.get().getHome().getDirectory(), directoryPath + "/index");
            }
            if (!facet.isAbsolute()) {
                facet = new File(SystemInstance.get().getHome().getDirectory(), directoryPath + "/facet");
            }
            if (!index.exists()) {
                Files.mkdirs(index);
            }
            if (!facet.exists()) {
                Files.mkdirs(facet);
            }

            try {
                indexDir = FSDirectory.open(index);
                indexFacetsDir = FSDirectory.open(facet);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        final Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
        final KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
        fieldAnalyzers.put(DEPLOYABLE_ID_FIELD, keywordAnalyzer);
        fieldAnalyzers.put(APPLICATION_ID_FIELD, keywordAnalyzer);
        fieldAnalyzers.put(ENDPOINT_ID_FIELD, keywordAnalyzer);
        fieldAnalyzers.put("category", keywordAnalyzer);
        fieldAnalyzers.put("tag", keywordAnalyzer);
        fieldAnalyzers.put("role", keywordAnalyzer);
        fieldAnalyzers.put("context", keywordAnalyzer);
        fieldAnalyzers.put("path", keywordAnalyzer);
        fieldAnalyzers.put("httpMethod", keywordAnalyzer);
        fieldAnalyzers.put("application", keywordAnalyzer);
        fieldAnalyzers.put("applicationNameVersion", keywordAnalyzer);
        // host, doc, search
        fieldAnalyzers.put(DOC, keywordAnalyzer);
        fieldAnalyzers.put(SUMMARY, keywordAnalyzer);

        analyzer = new PerFieldAnalyzerWrapper(new EnglishAnalyzer(), fieldAnalyzers);
        try {
            final IndexWriterConfig writerConfig = new IndexWriterConfig(Version.LATEST, analyzer);
            writer = new IndexWriter(indexDir, writerConfig);
            taxonomyWriter = new DirectoryTaxonomyWriter(indexFacetsDir);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }

        facetsConfig.setMultiValued("category", true);
        facetsConfig.setMultiValued("tag", true);
        facetsConfig.setMultiValued("role", true);

        closed = false;

        final SearchEngine self = ctx.getBusinessObject(SearchEngine.class);
        // do it asynchronously to not slow down the boot
        pendingAdd.add(self.doIndex());

        // on the fly indexation
//        observer = new OnTheFlyIndexation(self, pendingAdd, pendingRemove);
//        SystemInstance.get().addObserver(observer);
    }

    public void resetIndex() {
        waitForWrites();

        final Future<?> future = ctx.getBusinessObject(SearchEngine.class).doReindex();
        // doReindex() does both
        pendingRemove.add(future);
        pendingAdd.add(future);

    }

    @Asynchronous
    @Lock(LockType.WRITE)
    public Future<?> doReindex() {
        removeIndex();
        doIndex();
        return new AsyncResult<Void>(null);
    }

    public int pendingTasks() {
        return pendingAdd.size() + pendingRemove.size();
    }

    // created lazily to ensure we read something up to date
    // we can desire to use a @Schedule to do isCurrent() check instead of the caller thread
    private IndexSearcher searcher() { // normally we create it less often than a reader but both can't live together
        if (searcher == null) { // create it if needed
            try {
                if (!DirectoryReader.indexExists(indexDir)) {
                    return null;
                }
            } catch (final IOException e) {
                return null;
            }

            synchronized (this) {
                if (searcher == null) {
                    try {
                        reader = DirectoryReader.open(indexDir);
                        searcher = new IndexSearcher(reader);
                        taxonomyReader = new DirectoryTaxonomyReader(taxonomyWriter);
                    } catch (final IndexNotFoundException infe) {
                        return null;
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        try { // recreate it if something was written
            if (!reader.isCurrent()) {
                synchronized (this) {
                    if (!reader.isCurrent()) {
                        final DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
                        if (newReader != null) {
                            reader.close();
                            reader = newReader;
                        }
                        searcher = new IndexSearcher(reader);
                        taxonomyReader = new DirectoryTaxonomyReader(taxonomyWriter);
                    }
                }
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return searcher;
    }

    @Asynchronous
    @Lock(LockType.WRITE)
    public Future<?> doIndex() {

        if (indexDir == null) {
            return new AsyncResult<Object>(true);
        }

        if (writer == null) {
            return new AsyncResult<Object>(true);
        }

        // don't close the writer, it is too costly + it cleans up the data with in memory usage
        // locking usage should be enough for us
        Collection<Endpoint> allEndpoints = repository.findAllEndpoints();
        LOGGER.info("FOUND {} endpoints", allEndpoints.size());
        for (Endpoint endpoint: allEndpoints) {
            LOGGER.info("Index {} {}", endpoint.getVerb(), endpoint.getPath());
            final String webCtx = endpoint.getApplication().getSwagger().getBasePath();
            try {
                final Document eDoc = createDocument(endpoint, webCtx);
                addFacets(endpoint, webCtx != null ? webCtx : "/", eDoc);
                writer.addDocument(facetsConfig.build(taxonomyWriter, eDoc));
                writer.commit(); // flush by app
                writer.waitForMerges();
            } catch (final Exception ioe) {
                LOGGER.error("Can't flush index for application " + webCtx, ioe);
            }
        }
        return new AsyncResult<Object>(true);
    }

    private void addFacets(final Endpoint endpoint, final String web, final Document doc) {
//        addFacetFields(doc, endpoint.getMetadata().getCategories(), "category");
        addFacetFields(doc, endpoint.getOperation().getTags(), "tag");
//        addFacetFields(doc, endpoint.getSecurity().getRolesAllowed(), "role");
        if (!web.isEmpty()) {
            doc.add(new FacetField("context", web));
        }
    }

    private void addFacetFields(final Document doc, final Collection<String> set, final String name) {
        if (set != null) {
            for (final String value : set) {
                doc.add(new FacetField(name, value));
            }
        }
    }

    private Document createDocument(final Endpoint endpoint, final String webCtx) {
        final OpenApiDocument application = endpoint.getApplication();

        final Document eDoc = new Document();

        // id
        eDoc.add(field(DEPLOYABLE_ID_FIELD, application.getId(), true));
        eDoc.add(field(APPLICATION_ID_FIELD, application.getId(), true));
        eDoc.add(field(ENDPOINT_ID_FIELD, endpoint.getId(), true));

        eDoc.add(field("applicationNameVersion", Repository.getApplicationId(endpoint.getApplication().getSwagger()), true));

        // deployable
        if (webCtx != null && !webCtx.isEmpty()) {
            eDoc.add(field("context", webCtx, true));
        }
        if (application.getSwagger().getHost() != null) {
            eDoc.add(field("host", application.getSwagger().getHost(), true));
        }

        final String defaultDoc = application.getSwagger().getInfo().getDescription();
        if (defaultDoc != null && !defaultDoc.isEmpty()) {
            eDoc.add(field("applicationDoc", defaultDoc, false));
        }

        // endpoint
        eDoc.add(field(PATH, endpoint.getPath(), true)); // shorter
        eDoc.add(field(HTTP_METHOD, endpoint.getVerb()));
        eDoc.add(field(VERB, endpoint.getVerb(), true));

//        for (final String value : endpoint.getMetadata().getCategories()) {
//            eDoc.add(field("category", value));
//        }
        if (endpoint.getOperation().getTags() != null) {
            for (final String value : endpoint.getOperation().getTags()) {
                eDoc.add(field("tag", value));
            }
        }
//        for (final String value : endpoint.getSecurity().getRolesAllowed()) {
//            eDoc.add(field("role", value));
//        }
        final String summary = endpoint.getOperation().getSummary();
        if (summary != null && !summary.isEmpty()) {
            eDoc.add(field(SUMMARY, summary, true));
        }

        final String doc = endpoint.getOperation().getDescription();
        if (doc != null && !doc.isEmpty()) {
            eDoc.add(field(DOC, doc));
        }

        // compute subwords for the URI only to make searching easier
        String[] split = endpoint.getPath().split("[-_:/]");
        final List<String> pathSplit = new ArrayList<>();
        for (String s: split) {
            if (s.length() == 0) continue;
            // add the word anyway
            pathSplit.add(s);
            for (int i = 0; i < s.length() - 1; i++) { // more than 2 char combinations
                for (int j = 2; j <= s.length() - i; j++) { // more than 2 char combinations
                    final String sub = s.substring(i, i + j);
                    if (!s.equals(sub)) {
                        pathSplit.add(sub);
                    }
                }
            }
        }

        final String tags = endpoint.getOperation().getTags() == null ? "" : Join.join(",", endpoint.getOperation().getTags());
        // where search is done if not explicit
        final String search = endpoint.getVerb() + " "
                + endpoint.getPath() + " "
                + Join.join(",", pathSplit) + " "
//                + Join.join(",", endpoint.getMetadata().getCategories()) + " "
//                + Join.join(",", endpoint.getSecurity().getRolesAllowed()) + " "
                + tags + " "
                + (doc == null ? "" : doc) + " "
                + webCtx;
        eDoc.add(field("search", search));

        return eDoc;
    }

    public void waitForWrites() {
        flushTasks(pendingAdd);
        flushTasks(pendingRemove);
    }

    @Asynchronous
    @Lock(LockType.WRITE)
    public Future<?> removeIndex() {
        flushTasks(pendingAdd);

        final IndexWriter w = writer;
        if (w == null) {
            return new AsyncResult<Object>(true);
        }

        try {
            w.deleteAll();
            //w.deleteDocuments(new Term(DEPLOYABLE_ID_FIELD, deployable.getId()));
            w.deleteUnusedFiles();
        } catch (final Exception e) {
            LOGGER.error("Can't clear index", e);
        } finally {
            try {
                w.commit();
            } catch (final IOException e) {
                // no-op
            }
        }
        return new AsyncResult<Object>(true);
    }

    @PreDestroy
    private void releaseIndex() {
        closed = true;

//        if (observer != null) {
//            SystemInstance.get().removeObserver(observer);
//        }

        waitForWrites();
        flushTasks(pendingRemove);

        if (reader != null) {
            IO.close(reader);
            reader = null;
        }
        if (writer != null) {
            IO.close(writer);
            writer = null;
        }
        if (taxonomyWriter != null) {
            IO.close(taxonomyWriter);
            taxonomyWriter = null;
        }
        if (taxonomyReader != null) {
            IO.close(taxonomyReader);
            taxonomyReader = null;
        }

        if (indexDir != null) {
            final Directory tmp = indexDir;
            indexDir = null;
            try {
                tmp.close();
            } catch (final IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        if (indexFacetsDir != null) {
            final Directory tmp = indexFacetsDir;
            indexFacetsDir = null;
            try {
                tmp.close();
            } catch (final IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }

    private void flushTasks(final BlockingQueue<Future<?>> tasks) {
        final Collection<Future<?>> list = new ArrayList<>(tasks.size());
        tasks.drainTo(list);

        for (final Future<?> task : list) {
            try {
                final long iterations = duration.getUnit().toMillis(duration.getTime()) / 250;
                for (long i = 0; i < iterations; i++) { // 1mn
                    while (!task.isDone() && !task.isCancelled()) {
                        task.get(250, TimeUnit.MILLISECONDS); // check regularly if we can go out of this loop and don't wait the get(x, m) duration
                    }
                }
            } catch (final InterruptedException e) {
                Thread.interrupted();
            } catch (final Exception e) {
                // no-op
            }
        }
    }

    private static IndexableField field(final String name, final String value, final boolean store) {
        return new StringField(name, value, store ? Field.Store.YES : Field.Store.NO);
    }

    private static IndexableField field(final String name, final String value) {
        return new StringField(name, value, Field.Store.NO);
    }
/*
    public static class OnTheFlyIndexation {
        private final SearchEngine indexer;
        private final Collection<Future<?>> addQueue;
        private final Collection<Future<?>> removeQueue;

        private OnTheFlyIndexation(final SearchEngine self, Collection<Future<?>> pendingAdd, final Collection<Future<?>> pendingRemove) {
            this.indexer = self;
            this.addQueue = pendingAdd;
            this.removeQueue = pendingRemove;
        }

        public void install(@Observes final DeployableInfoCreated event) {
            if (indexer.isClosed()) {
                return;
            }
            addQueue.add(indexer.doIndex(singletonList(event.getDeployable())));
        }

        public void deinstall(@Observes final DeployableInfoDestroyed event) {
            if (indexer.isClosed()) {
                return;
            }
            removeQueue.add(indexer.removeIndex(event.getDeployable()));
        }
    }
    */
}
