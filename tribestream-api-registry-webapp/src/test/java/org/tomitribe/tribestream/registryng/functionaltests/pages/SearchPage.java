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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jboss.arquillian.graphene.Graphene.guardAjax;

public class SearchPage {

    @FindBy(css = "div[x-ng-repeat='application in applications']")
    private List<WebElement> applications;

    @FindBy(linkText = "Create Application")
    private WebElement createApplicationButton;

    public List<String> getApplications() {

        return applications.stream()
                .map(appElem -> appElem.findElement(By.tagName("h2")))
                .map(WebElement::getText)
                .collect(toList());
    }

    public void clickCreateApplicationButton() {
        guardAjax(createApplicationButton).click();
    }
}
