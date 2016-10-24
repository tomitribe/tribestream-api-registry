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

import cucumber.api.java.en.Given;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.net.URL;
import java.util.List;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.junit.Assert.assertTrue;

public class AuthorizationSteps extends StepBase {

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

    @Given("^I am logged in as \"(.*?)\" with password \"(.*?)\"$")
    public void i_am_logged_in_as_admin_with_password_admin(final String username, final String password) throws Throwable {

        driver.manage().window().maximize();

        driver.navigate().to(url);

        waitGui();
        Thread.sleep(5000);

        List<WebElement> logoutButtons = driver.findElements(By.className("fa-sign-out"));
        if (logoutButtons.size() > 0) {
            guardHttp(logout).click();
        }

        assertTrue(loginForm.isDisplayed());
        assertTrue(submit.isDisplayed());

        usernameField.clear();
        usernameField.sendKeys(username);
        passwordField.clear();
        passwordField.sendKeys(password);

        guardAjax(submit).click();
    }
}
