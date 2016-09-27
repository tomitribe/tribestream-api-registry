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
package org.tomitribe.tribestream.registryng.service.search;

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
import org.tomitribe.tribestream.registryng.domain.CloudItem;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.SearchResult;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.lucene.JPADirectoryFactory;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.util.Duration;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.lucene.facet.DrillDownQuery.term;
import static org.tomitribe.tribestream.registryng.domain.TribestreamOpenAPIExtension.PROP_CATEGORIES;
import static org.tomitribe.tribestream.registryng.domain.TribestreamOpenAPIExtension.PROP_ROLES;
import static org.tomitribe.tribestream.registryng.domain.TribestreamOpenAPIExtension.VENDOR_EXTENSION_KEY;

@Startup
@Singleton
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SearchEngine {
    private static final Logger LOGGER = Logger.getLogger(SearchEngine.class.getName());

    private static final String APPLICATION_ID_FIELD = "applicationId";
    private static final String ENDPOINT_ID_FIELD = "endpointId";
    private static final String VERB = "verb";
    private static final String HTTP_METHOD = "httpMethod";
    private static final String PATH = "path";
    private static final String DOC = "doc";
    private static final String SUMMARY = "summary";

    private final Duration duration;
    private final Repository repository;
    private final Directory indexDir;
    private final Directory indexFacetsDir;

    @Resource
    private SessionContext ctx;

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

    private Duration writeTimeout = new Duration("1 minute"); // TODO: config

    @Inject
    public SearchEngine(final Repository repository,
                        final JPADirectoryFactory directory) {
        this.duration = new Duration(10, TimeUnit.SECONDS); // timeout;
        this.repository = repository;
        this.indexDir = directory.newInstance("index");
        this.indexFacetsDir = directory.newInstance("facets");
    }

    protected SearchEngine() {
        this(null, null);
    }

    public SearchPage search(final SearchRequest request) {
        final int pageSize = request.getCount();
        try {
            final IndexSearcher indexSearcher = searcher();
            if (indexSearcher == null) { // no index
                return new SearchPage(
                        new ArrayList<>(), 0, 0,
                        new ArrayList<>(), new ArrayList<>(),
                        new ArrayList<>(), new ArrayList<>());
            }

            final BooleanQuery.Builder query = new BooleanQuery.Builder();
            Stream.of(
                    createQuery(request.getQuery()),
                    drillDownFor("category", request.getCategories()),
                    drillDownFor("tag", request.getTags()),
                    drillDownFor("role", request.getRoles())
            ).filter(q -> q != null).forEach(q -> query.add(q, BooleanClause.Occur.MUST));

            if (request.getApps() != null && request.getApps().size() > 0) {
                BooleanQuery.Builder appFilter = new BooleanQuery.Builder();
                appFilter.setMinimumNumberShouldMatch(1);
                for (String s : request.getApps()) {
                    appFilter.add(new TermQuery(new Term("context", s)), BooleanClause.Occur.SHOULD);
                    appFilter.add(new TermQuery(new Term("applicationName", s)), BooleanClause.Occur.SHOULD);
                }
                query.add(appFilter.build(), BooleanClause.Occur.MUST);
            }

            final TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create((request.getPage() + 1) * pageSize);
            final FacetsCollector facetsCollector = new FacetsCollector(true);
            indexSearcher.search(query.build(), MultiCollector.wrap(topScoreDocCollector, facetsCollector));
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
                        doc.get("applicationName"),
                        doc.get("applicationVersion"),
                        doc.get(VERB),
                        doc.get(PATH),
                        doc.get(SUMMARY),
                        new HashSet<>(), // Consumes,
                        new HashSet<>(), // Produces,
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
        final BooleanQuery.Builder query = new BooleanQuery.Builder().setDisableCoord(true);
        for (final String value : values) { // DrillDownQuery uses SHOULD here, we want MUST
            final String indexFieldName = facetsConfig.getDimConfig(key).indexFieldName;
            query.add(new TermQuery(term(indexFieldName, key, value)), BooleanClause.Occur.MUST);
        }
        return query.build();
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

    @PostConstruct
    private void createIndex() {
        final Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
        final KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
        fieldAnalyzers.put(APPLICATION_ID_FIELD, keywordAnalyzer);
        fieldAnalyzers.put(ENDPOINT_ID_FIELD, keywordAnalyzer);
        fieldAnalyzers.put("category", keywordAnalyzer);
        fieldAnalyzers.put("tag", keywordAnalyzer);
        fieldAnalyzers.put("role", keywordAnalyzer);
        fieldAnalyzers.put("context", keywordAnalyzer);
        fieldAnalyzers.put("path", keywordAnalyzer);
        fieldAnalyzers.put("httpMethod", keywordAnalyzer);
        fieldAnalyzers.put("application", keywordAnalyzer);
        fieldAnalyzers.put("applicationName", keywordAnalyzer);
        fieldAnalyzers.put("applicationVersion", keywordAnalyzer);
        // host, doc, search
        fieldAnalyzers.put(DOC, keywordAnalyzer);
        fieldAnalyzers.put(SUMMARY, keywordAnalyzer);

        analyzer = new PerFieldAnalyzerWrapper(new EnglishAnalyzer(), fieldAnalyzers);
        try {
            final IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
            writer = new IndexWriter(indexDir, writerConfig);
            taxonomyWriter = new DirectoryTaxonomyWriter(indexFacetsDir);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }

        facetsConfig.setMultiValued("category", true);
        facetsConfig.setMultiValued("tag", true);
        facetsConfig.setMultiValued("role", true);

        closed = false;
    }

    public Future<?> resetIndex() {
        waitForWrites();

        final Future<?> future = ctx.getBusinessObject(SearchEngine.class).doReindex();
        // doReindex() does both
        pendingRemove.add(future);
        pendingAdd.add(future);
        return future;

    }

    @Asynchronous
    @Lock(LockType.WRITE)
    public Future<?> doReindex() {
        try {
            removeIndex().get();
            doIndex().get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.WARNING, "Unexpected exception while reindexing", e);
        }
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

        if (taxonomyWriter == null) {
            return new AsyncResult<Object>(true);
        }

        // don't close the writer, it is too costly + it cleans up the data with in memory usage
        // locking usage should be enough for us
        Collection<Endpoint> allEndpoints = repository.findAllEndpoints();
        LOGGER.info(() -> String.format("FOUND %s endpoints", allEndpoints.size()));
        for (Endpoint endpoint : allEndpoints) {
            indexEndpoint(endpoint, false); // sync here!
        }
        return new AsyncResult<Object>(true);
    }

    public void indexEndpoint(final Endpoint endpoint, final boolean update) {
        LOGGER.info(() -> String.format("Indexing %s %s", endpoint.getVerb(), endpoint.getPath()));
        final String webCtx = endpoint.getApplication().getSwagger().getBasePath();
        try {
            final Document eDoc = createDocument(endpoint, webCtx);
            addFacets(endpoint, webCtx != null ? webCtx : "/", eDoc);
            final Document document = facetsConfig.build(taxonomyWriter, eDoc);
            if (update) {
                writer.addDocument(document);
            } else {
                writer.updateDocument(/*todo: have a real id*/new Term(ENDPOINT_ID_FIELD, endpoint.getId()), document.getFields());
            }
            writer.commit(); // flush by app
            waitWrite();
        } catch (final Exception ioe) {
            LOGGER.log(Level.WARNING, ioe, () -> String.format("Can't flush index for application %s", webCtx));
        }
    }

    public void deleteEndpoint(final Endpoint endpoint) {
        LOGGER.info(() -> String.format("Deleting Index %s %s", endpoint.getVerb(), endpoint.getPath()));
        final String webCtx = endpoint.getApplication().getSwagger().getBasePath();
        try {
            writer.deleteDocuments(endpointQuery(endpoint));
            writer.commit(); // flush by app
            waitWrite();
        } catch (final Exception ioe) {
            LOGGER.log(Level.WARNING, ioe, () -> String.format("Can't flush index for application %s", webCtx));
        }
    }

    private BooleanQuery endpointQuery(final Endpoint endpoint) {
        return new BooleanQuery.Builder()
                .add(new TermQuery(new Term(APPLICATION_ID_FIELD, endpoint.getApplication().getId())), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(ENDPOINT_ID_FIELD, endpoint.getId())), BooleanClause.Occur.MUST)
                .build();
    }

    private void waitWrite() throws InterruptedException {
        final long pauseDuration = 200;
        long remaining = writeTimeout.getTime(MILLISECONDS);
        while (writer.hasPendingMerges() && remaining > 0) {
            sleep(pauseDuration);
            remaining -= pauseDuration;
        }
        if (remaining <= 0) {
            throw new IllegalStateException("Can't save the index properly, you should check what happens");
        }
    }

    private void addFacets(final Endpoint endpoint, final String web, final Document doc) {
        addFacetFields(doc, getExtensionProperty(endpoint, PROP_CATEGORIES, Collections::emptyList), "category");
        addFacetFields(doc, endpoint.getOperation().getTags(), "tag");
        addFacetFields(doc, getExtensionProperty(endpoint, PROP_ROLES, Collections::emptyList), "role");
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
        eDoc.add(field(APPLICATION_ID_FIELD, application.getId(), true));
        eDoc.add(field(ENDPOINT_ID_FIELD, endpoint.getId(), true));

        eDoc.add(field("applicationName", endpoint.getApplication().getSwagger().getInfo().getTitle(), true));
        eDoc.add(field("applicationVersion", endpoint.getApplication().getSwagger().getInfo().getVersion(), true));

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


        for (final String value : getExtensionProperty(endpoint, PROP_CATEGORIES, Collections::<String>emptyList)) {
            eDoc.add(field("category", value));
        }
        if (endpoint.getOperation().getTags() != null) {
            for (final String value : endpoint.getOperation().getTags()) {
                eDoc.add(field("tag", value));
            }
        }
        for (final String value : getExtensionProperty(endpoint, PROP_ROLES, Collections::<String>emptyList)) {
            eDoc.add(field("role", value));
        }
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
        for (String s : split) {
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
                + Join.join(",", (List<String>) getExtensionProperty(endpoint, PROP_CATEGORIES, Collections::<String>emptyList)) + " "
                + Join.join(",", (List<String>) getExtensionProperty(endpoint, PROP_ROLES, Collections::<String>emptyList)) + " "
                + tags + " "
                + (doc == null ? "" : doc) + " "
                + webCtx;
        eDoc.add(field("search", search));

        return eDoc;
    }

    private <T> T getExtensionProperty(final Endpoint endpoint, final String extensionPropertyName, final Supplier<T> defaultSupplier) {
        return (T) Optional.ofNullable(endpoint.getOperation().getVendorExtensions())
                .map((Map<String, Object> vendorExtensions) -> (Map<String, Object>) vendorExtensions.get(VENDOR_EXTENSION_KEY))
                .map((Map<String, Object> tapirExtension) -> tapirExtension.get(extensionPropertyName))
                .orElseGet(defaultSupplier);
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
            LOGGER.log(Level.WARNING, "Can't clear index", e);
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
        if (closed) {
            return;
        }
        closed = true;

        waitForWrites();

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
            try {
                indexDir.close();
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
        if (indexFacetsDir != null) {
            try {
                indexFacetsDir.close();
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
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
                LOGGER.warning("Interrupted while waiting for tasks to finish");
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
}
