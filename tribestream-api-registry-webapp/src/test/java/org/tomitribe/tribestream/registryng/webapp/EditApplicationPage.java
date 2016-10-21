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

import com.google.common.base.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.WebDriverWait;

public class EditApplicationPage {

    @FindBy(css = "div[data-value='swagger.info.title'] div.text")
    private WebElement titleField;

    @FindBy(css = "div[data-value='swagger.info.version'] div.text")
    private WebElement versionField;

    @FindBy(xpath = "//button[contains(.,'Create')]")
    private WebElement createButton;

    @FindBy(linkText = "Create Endpoint")
    private WebElement createEndpointButton;

    @FindBy(xpath = "//button[contains(.,'Save')]")
    private WebElement saveButton;

    @FindBy(css = "div[data-value='swagger.info.description']")
    private WebElement descriptionField;

    @FindBy(css = "div.tribe-field-actions-body div[x-ng-click='confirm()']")
    private WebElement confirmButton;

    public boolean isVisible() {

        try {
            return titleField.isDisplayed() && versionField.isDisplayed();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }


    public WebElement getTitleField() {
        return titleField;
    }

    public WebElement getVersionField() {
        return versionField;
    }

    public WebElement getDescriptionField() {
        return descriptionField.findElement(By.tagName("p"));
    }


    public void enterName(final WebDriver driver, final String newTitle) {
        titleField.click();
        new WebDriverWait(driver, 5000).until((Function<WebDriver, Boolean>)(dontcare -> titleField.findElement(By.cssSelector("input[type='text']")).isDisplayed()));
        WebElement textField = titleField.findElement(By.cssSelector("input[type='text']"));
        textField.sendKeys(Keys.END);
        System.out.println("TEXT: " + textField.getText());
        while (textField.getText().length() > 0) {
            textField.sendKeys(Keys.BACK_SPACE);
            System.out.println("TEXT: " + textField.getText());
        }
        textField.sendKeys(newTitle, "\n");
    }

    public void enterVersion(final String newVersion) {
        versionField.click();
        WebElement textField = versionField.findElement(By.cssSelector("input[type='text']"));
        textField.sendKeys(newVersion, "\n");
    }

    public void enterDescription(final WebDriver driver, final String newDescription) {
        System.out.println("Visible: " + descriptionField.findElement(By.cssSelector("div.preview p")).isDisplayed());
        System.out.println("Description: " + descriptionField.findElement(By.cssSelector("div.preview p")).getText());
        descriptionField.findElement(By.cssSelector("div.preview p")).click();
        System.out.println(driver.getPageSource());
        new WebDriverWait(driver, 5000).until((Function<WebDriver, Boolean>)(dontcare -> confirmButton.isDisplayed()));
        descriptionField.findElement(By.tagName("p")).sendKeys(newDescription);
        confirmButton.click();
    }

    public WebElement getCreateButton() {
        return createButton;
    }

    public WebElement getSaveButton() {
        return saveButton;
    }

    public WebElement getCreateEndpointButton() {
        return createEndpointButton;
    }
}
