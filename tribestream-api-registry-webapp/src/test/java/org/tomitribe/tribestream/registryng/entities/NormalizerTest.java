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
package org.tomitribe.tribestream.registryng.entities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class NormalizerTest {
    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] values() {
        return new String[][]{
                // some string tests
                {"simple", "simple"}, // noop
                {"SIMPLE", "SIMPLE"}, // uppercase is respected
                {"SimplE", "SimplE"}, // mixed case
                {"with space", "with-space"}, // space
                {"with accént", "with-accent"}, // accents
                {"/with/slash", "/with/slash"}, // slash
                {"/with/{id}", "/with/:id"}, // parameter in path
                {"/with/{id}/{name}", "/with/:id/:name"} // parameter*s* in path
        };
    }

    @Parameterized.Parameter
    public String from;

    @Parameterized.Parameter(1)
    public String to;

    @Test
    public void normalize() {
        assertEquals(to, Normalizer.normalize(from));
    }
}
