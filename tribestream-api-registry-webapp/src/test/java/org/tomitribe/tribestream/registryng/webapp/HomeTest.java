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
package org.tomitribe.tribestream.registryng.webapp;

import org.apache.openejb.testing.Application;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.tomitribe.tribestream.registryng.test.Registry;
import org.tomitribe.tribestream.registryng.test.retry.Retry;
import org.tomitribe.tribestream.registryng.test.selenium.WebAppTesting;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.By.cssSelector;

public class HomeTest extends WebAppTesting {
    @Application
    private Registry registry;

    @FindBy(css = "div[x-ng-repeat='application in applications']")
    private List<WebElement> applications;

    @Test
    @Retry
    public void ensureHomeListsDefaultApps() {
        waitingDriver.until(() -> applications.size() >= 3, "ensureHomeListsDefaultApps :: applications.size() >= 3");

        final Map<String, Collection<String>> expectedApplications = new HashMap<String, Collection<String>>() {{
            put("Swagger Petstore 1.0.0(4)", new HashSet<String>() {{
                add("GET /pets");
                add("GET /pets/:id");
                add("DELETE /pets/:id");
                add("POST /pets");
            }});
            put("Partners App 1.0.0(2)", new HashSet<String>() {{
                add("GET /workNotifications/:region/:function");
                add("GET /partners/:countryCode");
            }});
            put("Uber API 1.0.0(5)", new HashSet<String>() {{
                add("GET /estimates/pricePrice Estimates");
                add("GET /historyUser Activity");
                add("GET /estimates/timeTime Estimates");
                add("GET /meUser Profile");
                add("GET /productsProduct Types");
            }});
        }};

        applications.stream()
                .filter(webElement -> {
                    try {
                        // Filter out the applications without endpoints that were created by other tests. (Yes, test cases are not isolated)
                        return !webElement.findElements(cssSelector("div[x-ng-repeat='endpoint in application.endpoints']")).isEmpty();
                    } catch (Throwable th) {
                        return false;
                    }
                })
                .forEach(app -> assertEquals(
                        expectedApplications.remove(app.findElement(cssSelector("h2")).getText() /*app name + version + number of endpoints*/),
                        app.findElements(cssSelector("div[x-ng-repeat='endpoint in application.endpoints']")).stream().map(WebElement::getText).collect(toSet())));

        // we saw all applications we expected
        assertTrue(expectedApplications.keySet().toString(), expectedApplications.isEmpty());
    }
}
