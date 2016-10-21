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

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.SystemClock;

import java.io.FileOutputStream;
import java.io.IOException;

public class StepBase {

    @Drone
    protected WebDriver driver;

    protected void assertVisible(String s, WebElement elem) {
        if (!elem.isDisplayed()) {
            throw new AssertionError(s + " not visible in " + driver.getPageSource());
        }
    }

    public void createScreenshot(final String filename) throws IOException {
        try (FileOutputStream fout = new FileOutputStream(filename)) {
            fout.write(((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES));
        }

    }

    protected void retry(int timeoutInSecs, FailableAction action) throws Throwable {

        Throwable finalError = null;
        long end = System.currentTimeMillis() + timeoutInSecs * 1000;
        while (System.currentTimeMillis() < end) {
            try {
                action.run();
                return;
            } catch (Throwable th) {
                finalError = th;
            }
        }
        throw finalError;
    }

    @FunctionalInterface
    interface FailableAction {
        public void run() throws Throwable;
    }

}
