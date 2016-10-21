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

import static org.jboss.arquillian.graphene.Graphene.guardNoRequest;
import static org.jboss.arquillian.graphene.Graphene.waitGui;

public class EndpointDetailsPage {

    @Drone
    private WebDriver driver;

    @FindBy(css = "div[data-selected-option='endpoint.httpMethod']")
    private WebElement verbSingleSelect;

    @FindBy(css = "div[data-value='endpoint.path']")
    private WebElement pathTextField;

    @FindBy(css = "i.fa.fa-check")
    private WebElement checkButton;

    @FindBy(css = "article.app-ep-details-body")
    private WebElement endpointDetailsBody;

    public WebElement getBody() {
        return endpointDetailsBody;
    }

    public void enterVerb(final String newVerb) throws IOException {
        verbSingleSelect.click();
        waitGui();
        WebElement textField = verbSingleSelect.findElement(By.cssSelector("input[type='text']"));
        guardNoRequest(textField).sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE, newVerb, Keys.ENTER);
    }

    public void enterPath(final String newPath) throws IOException {
        pathTextField.click();
        waitGui();
        WebElement textField = pathTextField.findElement(By.cssSelector("input[type='text']"));
        textField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        textField.sendKeys(newPath);
        guardNoRequest(checkButton).click();
    }

}
