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
package org.tomitribe.tribestream.registry.test.selenium;

import com.google.common.base.Predicate;
import lombok.experimental.Delegate;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.tomitribe.tribestream.registry.test.Registry;
import org.w3c.tidy.Tidy;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.junit.Assert.fail;
import static org.junit.rules.RuleChain.outerRule;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;

// integrates FluentLenium with our webdriver in a smooth way for end tests
public abstract class WebAppTesting implements WebDriver, JavascriptExecutor {
    /* needs 7.0.2 to work, workaround is to capture it in the child ATM
    @Application
    private Registry registry;
     */

    @Delegate
    protected WebDriver driver;

    @Delegate
    protected JavascriptExecutor executor;

    @Delegate
    protected J8WebDriverWait waitingDriver;

    @FindBy(css = "form[x-ng-submit='login()']")
    private WebElement loginForm;

    @FindBy(css = "input[x-ng-model='username']")
    private WebElement username;

    @FindBy(css = "input[x-ng-model='password']")
    private WebElement password;

    @FindBy(css = "button[type='submit']")
    private WebElement submit;

    @FindBy(className = "fa-sign-out")
    private WebElement logout;

    @Rule // dump the dom on error, will avoid some round trips
    public final TestRule debugRule = outerRule(new TomEEEmbeddedSingleRunner.Rule(this))
            .around(new TestRule() {
                @Override
                public Statement apply(final Statement base, final Description description) {
                    return new Statement() {
                        @Override
                        public void evaluate() throws Throwable {
                            final Registry registry = findRegistry();
                            driver = registry.getWebDriver();
                            executor = JavascriptExecutor.class.cast(driver);
                            waitingDriver = new J8WebDriverWait(driver, Integer.getInteger("test.registry.web.wait", 60));

                            driver.manage().window().maximize();
                            driver.get(registry.root());
                            PageFactory.initElements(driver, WebAppTesting.this);
                            doLogin();
                            try {
                                base.evaluate();
                            } catch (final Exception e) {
                                ofNullable(driver).ifPresent(d -> {
                                    final String source = d.getPageSource();

                                    // format the output (default is all but readable)
                                    final Tidy tidy = new Tidy();
                                    tidy.setShowErrors(0);
                                    tidy.setForceOutput(true);
                                    tidy.setSmartIndent(true);
                                    tidy.setSpaces(2);
                                    final StringWriter formatted = new StringWriter();
                                    tidy.parse(new StringReader(source), formatted);

                                    // and dump it to let the tester inspect what he did wrong
                                    System.out.println();
                                    System.out.println();
                                    System.out.println("DOM for " + description.getDisplayName());
                                    System.out.println();
                                    System.out.println(formatted.toString());
                                    System.out.println();
                                    System.out.println();
                                });
                                throw e;
                            } finally {
                                if (driver != null && needsLogin()) {
                                    try {
                                        logout.click();
                                        waitingDriver.until(new Predicate<WebDriver>() {
                                            @Override
                                            public boolean apply(final WebDriver webDriver) {
                                                return webDriver.getCurrentUrl().endsWith("/login");
                                            }
                                        });
                                    } catch (final Exception e) {
                                        // swallow this one and throw the other one
                                    }
                                }
                            }
                        }
                    };
                }
            });

    private void doLogin() {
        if (!needsLogin()) {
            return;
        }
        waitingDriver.ignoring(NoSuchElementException.class).until(visibilityOf(loginForm));
        username.clear();
        username.sendKeys(Registry.TESTUSER);
        password.clear();
        password.sendKeys(Registry.TESTPASSWORD);
        submit.click();
        waitingDriver.ignoring(NoSuchElementException.class).until(visibilityOf(logout));
    }

    // here to be overriden if one page needs to test the auth or that it is public
    // if really use we can add @Public
    protected boolean needsLogin() {
        return true;
    }

    // see comment at the top of that class
    private Registry findRegistry() {
        for (final Field f : getClass().getDeclaredFields()) {
            if (f.getType() == Registry.class) {
                f.setAccessible(true);
                try {
                    return (Registry) f.get(this);
                } catch (IllegalAccessException e) {
                    fail(e.getMessage());
                }
            }
        }
        fail("Didn't find @Application Registry registry; in " + this);
        throw new IllegalStateException(); // we'll not hit it but needed to make compilation happy
    }

    public static final class J8WebDriverWait extends WebDriverWait {
        private J8WebDriverWait(WebDriver driver, long timeOutInSeconds) {
            super(driver, timeOutInSeconds);
        }

        public void until(final Supplier<Boolean> isTrue) {
            super.until(new Predicate<WebDriver>() {
                @Override
                public boolean apply(final WebDriver webDriver) {
                    return isTrue.get();
                }
            });
        }
    }
}
