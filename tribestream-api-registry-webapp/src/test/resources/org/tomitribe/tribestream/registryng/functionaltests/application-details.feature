# Tomitribe Confidential
#
# Copyright Tomitribe Corporation. 2016
#
# The source code for this program is not published or otherwise divested
# of its trade secrets, irrespective of what has been deposited with the
# U.S. Copyright Office.

Feature: Tribestream :: Registry :: Application Details

  Background:
    Given I am logged in as "admin" with password "admin"

  Scenario: Search page should show application "Swagger Petstore"

    Then I should see the application "Swagger Petstore"

  Scenario: Create a new application

    When I create a new application
    And set the application name to "My cool app" and version to "1.0.0"
    And hit the create button
    And go back to the home page
    Then I should see the application "My cool app 1.0.0"
