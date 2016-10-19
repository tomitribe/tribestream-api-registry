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
package org.tomitribe.tribestream.registryng.functionaltests.pages;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardNoRequest;
import static org.junit.Assert.assertThat;

public class SearchPage {

    @Drone
    private PhantomJSDriver driver;

    @FindBy(css = "div[data-app-endpoints-list='data-app-endpoints-list']")
    private WebElement searchPage;

    @FindBy(css = "div[x-ng-repeat='application in applications']")
    private List<WebElement> applications;

    @FindBy(css = "input[type='text']")
    private WebElement searchField;

    @FindBy(linkText = "Create Application")
    private WebElement createApplicationButton;

    public List<String> getApplications() {
        return applications.stream()
                .map(appElem -> appElem.findElement(By.tagName("h2")))
                .map(WebElement::getText)
                .collect(toList());
    }

    public void clickCreateApplicationButton() {
        guardNoRequest(createApplicationButton).click();
    }

    public void selectApplication(final String applicationName, final String version) {

        WebElement element = driver.findElementByLinkText(applicationName + " " + version);

        guardAjax(element).click();

    }

    public boolean isVisible() {

        return searchPage.isDisplayed();

    }

    public void refresh() {
        guardNoRequest(searchField).sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE, Keys.RETURN);
    }

    public void assertHasEndpoint(final String application, final String verb, final String path) {

        WebElement applicationElement = applications.stream()
                .filter(appElem -> appElem.findElement(By.tagName("h2")).getText().startsWith(application))
                .findFirst()
                .orElseThrow(() -> new AssertionError("There is no application with name " + application +". Available applications are " + getApplications()));


        List<String> endpoints = applicationElement.findElements(By.cssSelector("div[x-ng-repeat='endpoint in application.endpoints'] a")).stream()
                .map(endpointElem -> endpointElem.getText())
                .collect(toList());

        assertThat(endpoints, hasItem(verb + " " + path));
    }
}
