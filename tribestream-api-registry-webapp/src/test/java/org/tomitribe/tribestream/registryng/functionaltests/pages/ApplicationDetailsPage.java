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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.io.IOException;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardNoRequest;
import static org.jboss.arquillian.graphene.Graphene.waitGui;

public class ApplicationDetailsPage {

    @Drone
    private WebDriver driver;

    @FindBy(css = "div[data-value='swagger.info.title'] div.text")
    private WebElement titleField;

    @FindBy(css = "div[data-value='swagger.info.version'] div.text")
    private WebElement versionField;

    @FindBy(partialLinkText = "Create Endpoint")
    private WebElement createEndpointButton;

    @FindBy(css = "div[x-ng-click='save()']")
    private WebElement saveButton;

    @FindBy(css = "div[data-value='swagger.info.description'] div.main")
    private WebElement descriptionField;

    @FindBy(css = "div[data-value='swagger.info.description'] textarea")
    private WebElement descriptionEditor;

    @FindBy(css = "div.tribe-field-actions-body div[x-ng-click='confirm()']")
    private WebElement confirmButton;


    @FindBy(css = "a[href='.']")
    private WebElement homeLink;

    @FindBy(css = "i.fa.fa-check")
    private WebElement checkButton;


    public void enterApplicationName(final String newTitle) throws IOException {
        titleField.click();

        waitGui();

        WebElement textField = titleField.findElement(By.cssSelector("input[type='text']"));
        // No Ctrl+A because on Mac it's probably COMMAND+A
        textField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        textField.sendKeys(newTitle);
        guardNoRequest(checkButton).click();


    }


    public void enterVersion(final String newVersion) throws IOException {
        versionField.click();
        waitGui();
        WebElement textField = versionField.findElement(By.cssSelector("input[type='text']"));
        textField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        textField.sendKeys(newVersion);
        guardNoRequest(checkButton).click();
    }

    public void enterDescription(final String newDescription) throws IOException {

        // TODO: No idea how to edit the markdown component. SeleniumIDE does not see any actions as well... :-(
//        assertTrue(descriptionField.isDisplayed());
//        Actions actions = new Actions(driver).moveToElement(descriptionField);
//        createScreenshot("target/aftermove.png");
//        actions.click();
//        createScreenshot("target/afterclick.png");

    }

    public String getApplicationName() {
        return titleField.getText();
    }

    public String getApplicationVersion() {
        return versionField.getText();
    }

    public void clickCreateApplicationButton() throws Exception {
        saveButton.click();

        waitGui();
    }

    public void clickCreateEndpointButton() throws Exception {
        createEndpointButton.click();

        waitGui();
    }

    public void clickSaveApplicationButton() throws Exception {
        guardAjax(saveButton).click();
    }

    public void clickHomeButton() {
        guardAjax(homeLink).click();
    }

}
