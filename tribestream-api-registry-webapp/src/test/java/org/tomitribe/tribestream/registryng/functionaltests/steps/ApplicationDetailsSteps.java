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
import cucumber.api.java.en.When;
import org.jboss.arquillian.graphene.page.Page;
import org.tomitribe.tribestream.registryng.functionaltests.pages.ApplicationDetailsPage;
import org.tomitribe.tribestream.registryng.functionaltests.pages.SearchPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApplicationDetailsSteps extends StepBase {

    @Page
    private SearchPage searchPage;

    @Page
    private ApplicationDetailsPage applicationDetailsPage;

    private String applicationName;

    private String applicationVersion;

    @When("^I create a new application$")
    public void i_create_a_new_application() throws Throwable {
        clearState();
        searchPage.clickCreateApplicationButton();
    }

    @When("^I create a new application with title \"(.*?)\" and version \"(.*?)\"$")
    public void i_create_a_new_application(final String applicationName, final String applicationVersion) throws Throwable {
        clearState();
        searchPage.clickCreateApplicationButton();
        set_the_application_name_to_and_version_to(applicationName, applicationVersion);
        hit_the_save_button();
        go_back_to_the_home_page();
    }

    private void clearState() {
        this.applicationName = null;
        this.applicationVersion = null;
    }

    @When("^(?:[Ii] )?set the application name to \"([^\"]*)\" and version to \"([^\"]*)\"$")
    public void set_the_application_name_to_and_version_to(final String applicationName, final String version) throws Throwable {

        this.applicationName = applicationName;
        this.applicationVersion = version;

        applicationDetailsPage.enterApplicationName(applicationName);
        applicationDetailsPage.enterVersion(version);

    }

    @When("^set the description to \"([^\"]*)\"$")
    public void set_the_description_to(final String description) throws Throwable {
        applicationDetailsPage.enterDescription(description);

    }

    @When("^hit the save button$")
    public void hit_the_save_button() throws Throwable {
        applicationDetailsPage.clickSaveApplicationButton();
    }

    @When("^hit the create endpoint button$")
    public void hit_the_create_endpoint_button() throws Throwable {
        applicationDetailsPage.clickCreateEndpointButton();
    }


    @When("^go back to the home page$")
    public void go_back_to_the_home_page() throws Throwable {
        applicationDetailsPage.clickHomeButton();
    }

    @When("I select this application")
    public void i_select_this_application() {
        assertTrue(searchPage.isVisible());

        if (applicationName == null || applicationVersion == null) {
            throw new IllegalStateException("Application name or version not set");
        }

        searchPage.selectApplication(applicationName, applicationVersion);
    }

    @Then("^the application details page shows these properties$")
    public void the_application_details_page_shows_these_properties() throws Throwable {

        i_select_this_application();

        assertEquals(applicationName, applicationDetailsPage.getApplicationName());

        assertEquals(applicationVersion, applicationDetailsPage.getApplicationVersion());

        // TODO: test description if set
//        if (description != null) {
//            assertEquals(description, applicationDetailsPage.getDescription());
//        }
    }


}
