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

public final class Normalizer {
    private Normalizer() {
        // no-op
    }

    public static String normalize(final String name) {
        return name == null ? "" : java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFKD)
                // kind of cheap transliteration
                // we could use a lib (there are several on github or IBM has a good one)
                // but this operation needs to stay cheap
                .replace("\u00c4", "Ae")
                .replace("\u00e4", "ae")
                .replace("\u00d6", "Oe")
                .replace("\u00f6", "oe")
                .replace("\u00dc", "Ue")
                .replace("\u00fc", "ue")
                .replace("\u00df", "ss")
                .replace("\u011e", "g")
                .replace("\u011f", "g")
                .replace("\u0130", "i")
                .replace("\u0131", "i")
                .replace("\u015e", "s")
                .replace("\u015f", "s")
                .replace("\u0410", "A")
                .replace("\u0411", "B")
                .replace("\u0412", "V")
                .replace("\u0413", "G")
                .replace("\u0414", "D")
                .replace("\u0415", "E")
                .replace("\u0416", "Zh")
                .replace("\u0417", "Z")
                .replace("\u0418", "I")
                .replace("\u0419", "J")
                .replace("\u041a", "K")
                .replace("\u041b", "L")
                .replace("\u041c", "M")
                .replace("\u041d", "N")
                .replace("\u041e", "O")
                .replace("\u041f", "P")
                .replace("\u0420", "R")
                .replace("\u0421", "S")
                .replace("\u0422", "T")
                .replace("\u0423", "U")
                .replace("\u0424", "F")
                .replace("\u0425", "H")
                .replace("\u0426", "Ts")
                .replace("\u0427", "Ch")
                .replace("\u0428", "Sh")
                .replace("\u0429", "Shch")
                .replace("\u042b", "Y")
                .replace("\u042d", "E")
                .replace("\u042e", "Yu")
                .replace("\u042f", "Ya")
                .replace("\u0430", "a")
                .replace("\u0431", "b")
                .replace("\u0432", "v")
                .replace("\u0433", "g")
                .replace("\u0434", "d")
                .replace("\u0435", "e")
                .replace("\u0436", "yo")
                .replace("\u0437", "z")
                .replace("\u0438", "i")
                .replace("\u0439", "j")
                .replace("\u043a", "k")
                .replace("\u043b", "l")
                .replace("\u043c", "m")
                .replace("\u043d", "n")
                .replace("\u043e", "o")
                .replace("\u043f", "p")
                .replace("\u0440", "r")
                .replace("\u0441", "s")
                .replace("\u0442", "t")
                .replace("\u0443", "u")
                .replace("\u0444", "f")
                .replace("\u0445", "h")
                .replace("\u0446", "ts")
                .replace("\u0447", "ch")
                .replace("\u0448", "sh")
                .replace("\u0449", "shch")
                .replace("\u044b", "y")
                .replace("\u044d", "e")
                .replace("\u044e", "yu")
                .replace("\u044f", "ya")
                .replace("\u0141", "L")
                .replace("\u0142", "l")
                // parameter in url shouldnt use braces so let's replace {doo} by :foo
                .replaceAll("\\{(\\w+)\\}", ":$1")
                // non ascii so not url friendly
                .replaceAll("[^\\p{ASCII}]+", "")
                .replaceAll("(?:[^\\w+/:]|\\s|\\+)+", "-")
                // leading/training - is useless and ugly
                .replaceAll("^-|-$", "")
                // if any unicode block remains strip it
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
