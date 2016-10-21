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

import cucumber.runtime.arquillian.CukeSpace;
import cucumber.runtime.arquillian.api.Features;
import cucumber.runtime.arquillian.api.Glues;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.tomitribe.tribestream.registryng.functionaltests.steps.ApplicationDetailsSteps;
import org.tomitribe.tribestream.registryng.functionaltests.steps.AuthorizationSteps;
import org.tomitribe.tribestream.registryng.functionaltests.steps.SearchPageSteps;

import java.io.File;

import static org.apache.openejb.loader.JarLocation.jarLocation;

@Ignore(
        "these tests rely on sleep() or no wait which is just no way to be deterministic (use until())" +
                "and duplicate WebAppTesting setup so we have to choose")
@RunWith(CukeSpace.class)
@Glues({
        AuthorizationSteps.class,
        SearchPageSteps.class,
        ApplicationDetailsSteps.class
})
@Features({
        "org/tomitribe/tribestream/registryng/functionaltests/application-details.feature",
        "org/tomitribe/tribestream/registryng/functionaltests/endpoint-details.feature"
})
public class FunctionalTestsIT {

    @Deployment(testable = false, name = "console")
    public static WebArchive app() {
        WebArchive war =
                ShrinkWrap.createFromZipFile(
                        WebArchive.class,
                        jarLocation(FunctionalTestsIT.class).getParentFile()
                                .listFiles((File file) -> file.getName().matches("^tribestream-api-registry-.*\\.war$"))[0]
                );

        System.out.println(war.toString(true));

        return war;
    }

    @Drone
    private PhantomJSDriver /* WebDriver */ driver;


}
