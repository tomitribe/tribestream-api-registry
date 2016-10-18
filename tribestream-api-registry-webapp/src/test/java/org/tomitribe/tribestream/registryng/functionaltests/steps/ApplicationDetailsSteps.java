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

import cucumber.api.java.en.When;
import org.jboss.arquillian.graphene.page.Page;
import org.tomitribe.tribestream.registryng.functionaltests.pages.ApplicationDetailsPage;
import org.tomitribe.tribestream.registryng.functionaltests.pages.SearchPage;

public class ApplicationDetailsSteps {

    @Page
    private SearchPage searchPage;

    @Page
    private ApplicationDetailsPage applicationDetailsPage;

    @When("^I create a new application$")
    public void i_create_a_new_application() throws Throwable {
        searchPage.clickCreateApplicationButton();
    }

    @When("^set the application name to \"([^\"]*)\" and version to \"([^\"]*)\"$")
    public void set_the_application_name_to_and_version_to(String applicationName, String version) throws Throwable {
        applicationDetailsPage.enterApplicationName(applicationName);
        applicationDetailsPage.enterVersion(version);

    }

    @When("^hit the create button$")
    public void hit_the_create_button() throws Throwable {
        applicationDetailsPage.clickCreateApplicationButton();
    }

    @When("^go back to the home page$")
    public void go_back_to_the_home_page() throws Throwable {
        applicationDetailsPage.clickHomeButton();
    }

}
