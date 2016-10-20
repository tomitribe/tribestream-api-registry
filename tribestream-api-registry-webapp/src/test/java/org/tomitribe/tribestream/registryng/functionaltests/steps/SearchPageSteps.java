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
package org.tomitribe.tribestream.registryng.functionaltests.steps;

import cucumber.api.java.en.Then;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.tomitribe.tribestream.registryng.functionaltests.pages.SearchPage;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class SearchPageSteps {

    @Drone
    private PhantomJSDriver /* WebDriver */ driver;

    @Page
    private SearchPage searchPage;

    @Then("^I should see the application \"(.*?)\"$")
    public void i_should_see_the_application(final String applicationName) throws Throwable {

        searchPage.refresh();

        List<String> applicationNames = searchPage.getApplications();

        assertTrue(
                String.format("Visible applications %s don't contain %s", applicationNames, applicationName),
                applicationNames.stream().filter(name -> name.contains(applicationName)).findFirst().isPresent());
    }

    @Then("^I should see the endpoint \"(.*?)\" \"(.*?)\" in the application \"(.*?)\"$")
    public void i_should_see_the_endpoint(final String verb, final String path, final String application) {

        searchPage.refresh();

        searchPage.assertHasEndpoint(application, verb, path);


    }
}
