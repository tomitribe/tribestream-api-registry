# Tomitribe Confidential
#
# Copyright Tomitribe Corporation. 2016
#
# The source code for this program is not published or otherwise divested
# of its trade secrets, irrespective of what has been deposited with the
# U.S. Copyright Office.

Feature: Tribestream :: Registry :: Endpoint Details

  Background:
    Given I am logged in as "admin" with password "admin"

  Scenario: Search page should show endpoint "GET /pets"

    Then I should see the endpoint "GET" "/pets" in the application "Swagger Petstore"

  Scenario: Create a new application with a new endpoint

    Given I create a new application with title "New endpoint test app" and version "42"
    When I select this application
    And hit the create endpoint button
    And set the verb to "OPTIONS" and the path to "/a/nice/path/with/a/:placeholder"
    # And hit the save endpoint button
    # And go back to the home page
    # Then I should see the endpoint "OPTIONS" "/a/nice/path/with/a/:placeholder" in the application "New endpoint test app"
