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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.tomitribe.tribestream.registryng.functionaltests.pages.EndpointDetailsPage;

import static org.jboss.arquillian.graphene.Graphene.waitGui;

public class EndpointDetailsSteps extends StepBase {

    @Page
    private EndpointDetailsPage endpointDetailsPage;

    @When("^set the verb to \"(.*?)\" and the path to \"(.*?)\"$")
    public void set_the_verb_to_and_the_path_to(final String verb, final String path) throws Exception {

        assertVisible("Endpointdetails page", endpointDetailsPage.getBody());
/*
        verbSingleSelect.click();
        waitGui();
        verbSingleSelect.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE, verb, Keys.RETURN);
        waitGui();

        createScreenshot("target/afterSetVerb.png");
*/
        endpointDetailsPage.enterVerb(verb);
        createScreenshot("target/afterSetVerb.png");
        endpointDetailsPage.enterPath(path);
        createScreenshot("target/afterSetPath.png");
    }

    @When("^hit the save endpoint button$")
    public void hit_the_save_button() throws Throwable {
        endpointDetailsPage.clickSaveEndpointButton();
    }

}
