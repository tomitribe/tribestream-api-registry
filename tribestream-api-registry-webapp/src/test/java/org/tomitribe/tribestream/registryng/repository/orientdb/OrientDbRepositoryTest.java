package org.tomitribe.tribestream.registryng.repository.orientdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.PathParameter;
import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.tribestream.registryng.repository.orientdb.model.SwaggerReference;
import org.tomitribe.tribestream.registryng.resources.Registry;
import org.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(TomEEEmbeddedSingleRunner.class)
public class OrientDbRepositoryTest {
    @Application
    private Registry registry;

    @Inject
    private OrientDbRepository repository;

    @Inject
    @Named(SwaggerJsonMapperProducer.SWAGGER_OBJECT_MAPPER_NAME)
    private ObjectMapper objectMapper;

    @Test
    public void crud() throws IOException {
        Stream.of("api-with-examples", "seed-db/uber").forEach(p -> {
            final Swagger swagger;
            try {
                swagger = objectMapper.readValue(getClass().getResourceAsStream("/" + p + ".json"), Swagger.class);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }

            final SwaggerReference reference = new SwaggerReference();
            reference.setSwagger(swagger);
            reference.setId(p);

            final SwaggerReference saved = repository.save(reference);
            assertNotNull(saved);

            final SwaggerReference lookup = repository.find(p);
            assertNotNull(lookup);

            Stream.of(saved, lookup).forEach(ref -> assertEquals(swagger, ref.getSwagger()));
        });
        // delete after to ensure we run previous part of the test with multiple entries
        Stream.of("api-with-examples", "seed-db/uber").forEach(p -> {
            final SwaggerReference reference = new SwaggerReference();
            reference.setId(p);

            repository.delete(reference);
            assertNull(repository.find(p));
        });
    }

    @Test
    public void conflictingTypes() throws IOException {
        for (int i = 0; i < 3; i++) {
            final PathParameter parameter = new PathParameter();
            parameter.setName("param");
            if (i == 0) {
                parameter.setDefault(1);
                parameter.setType("integer");
                parameter.setFormat("int32");
            } else if (i == 1) {
                parameter.setDefault("test");
                parameter.setType("string");
            } else {
                parameter.setDefault(true);
                parameter.setType("boolean");
            }

            final Operation get = new Operation();
            get.parameter(parameter);

            final Path path = new Path();
            path.get(get);

            final Swagger swagger = new Swagger();
            swagger.setInfo(new Info());
            swagger.getInfo().setTitle("ref_" + i);
            swagger.getInfo().setVersion("1.0.0");
            swagger.path("/test", path);

            final SwaggerReference ref = new SwaggerReference();
            ref.setId("ref" + i);
            ref.setSwagger(swagger);
            repository.save(ref);
        }

        for (int i = 0; i < 3; i++) {
            final SwaggerReference ref = repository.find("ref" + i);
            assertNotNull(ref);

            final Object theDefault = PathParameter.class.cast(ref.getSwagger().getPath("/test").getGet().getParameters().get(0)).getDefault();
            if (i == 0) {
                assertEquals(1L /*ok not the int i sent but good enough*/, theDefault);
            } else if (i == 1) {
                assertEquals("test", theDefault);
            } else {
                assertEquals(true, theDefault);
            }

            repository.delete(ref);
        }
    }
}
