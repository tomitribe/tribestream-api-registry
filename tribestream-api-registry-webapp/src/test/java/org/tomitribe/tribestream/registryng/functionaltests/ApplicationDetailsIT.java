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
package org.tomitribe.tribestream.registryng.functionaltests;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.runtime.arquillian.CukeSpace;
import cucumber.runtime.arquillian.api.Glues;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.FindBy;
import org.tomitribe.tribestream.registryng.functionaltests.pages.SearchPage;
import org.tomitribe.tribestream.registryng.functionaltests.steps.ApplicationDetailsSteps;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.junit.Assert.assertTrue;

@RunWith(CukeSpace.class)
@Glues(ApplicationDetailsSteps.class)
public class ApplicationDetailsIT {

    @Deployment(testable = false, name = "console")
    public static WebArchive app() {
        WebArchive war = ShrinkWrap.createFromZipFile(WebArchive.class, new File(System.getProperty("warfilename")));

        System.out.println(war.toString(true));

        return war;
    }

    @Drone
    private PhantomJSDriver /* WebDriver */ driver;

    @ArquillianResource
    private URL url;

    @FindBy(css = "form[x-ng-submit='login()']")
    private WebElement loginForm;

    @FindBy(css = "input[x-ng-model='username']")
    private WebElement usernameField;

    @FindBy(css = "input[x-ng-model='password']")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElement submit;

    @FindBy(className = "fa-sign-out")
    private WebElement logout;

    @Page
    private SearchPage searchPage;

    @Given("^I am logged in as \"(.*?)\" with password \"(.*?)\"$")
    public void i_am_logged_in_as_admin_with_password_admin(final String username, final String password) throws Throwable {

        driver.manage().window().maximize();

        driver.navigate().to(url);

        Thread.sleep(5000);

        assertTrue(loginForm.isDisplayed());
        assertTrue(submit.isDisplayed());

        usernameField.clear();
        usernameField.sendKeys(username);
        passwordField.clear();
        passwordField.sendKeys(password);

        guardAjax(submit).click();

    }

    @Then("^I should see the application \"(.*?)\"$")
    public void i_should_see_the_application(final String applicationName) throws Throwable {

        List<String> applicationNames = searchPage.getApplications();

        assertTrue(
                String.format("Visible applications %s don't contain %s", applicationNames, applicationName),
                applicationNames.stream().filter(name -> name.contains(applicationName)).findFirst().isPresent());
    }
}
