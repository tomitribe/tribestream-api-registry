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
package org.tomitribe.tribestream.registryng.domain;

import java.util.Collection;

public class ParameterDetail {
    private String name;
    private String style;
    private String defaultValue;
    private String type;
    private String doc;
    private boolean repeating;
    private boolean required;
    private Collection<String> sampleValues;

    public ParameterDetail() {
        // no-op
    }

    public ParameterDetail(final String name, final String style, final String defaultValue, final String type, final String doc, final boolean repeating, final boolean required,
                           final Collection<String> sampleValues) {
        setName(name);
        setStyle(style);
        setDefaultValue(defaultValue);
        setType(type);
        setDoc(doc);
        setRepeating(repeating);
        setRequired(required);
        setSampleValues(sampleValues);
    }

    public Collection<String> getSampleValues() {
        return sampleValues;
    }

    public void setSampleValues(final Collection<String> sampleValues) {
        this.sampleValues = sampleValues;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(final String style) {
        this.style = style;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(final String doc) {
        this.doc = doc;
    }

    public boolean isRepeating() {
        return repeating;
    }

    public void setRepeating(final boolean repeating) {
        this.repeating = repeating;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }
}
