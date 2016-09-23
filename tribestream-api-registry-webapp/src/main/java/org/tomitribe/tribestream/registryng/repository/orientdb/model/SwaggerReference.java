package org.tomitribe.tribestream.registryng.repository.orientdb.model;

import io.swagger.models.Swagger;

public class SwaggerReference {
    private String id;
    private Swagger swagger;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Swagger getSwagger() {
        return swagger;
    }

    public void setSwagger(final Swagger swagger) {
        this.swagger = swagger;
    }
}
