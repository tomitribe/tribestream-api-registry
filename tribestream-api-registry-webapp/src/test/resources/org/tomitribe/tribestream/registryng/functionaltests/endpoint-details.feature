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

  Scenario: Search page should show endpoint "GET "

    Then I should see the endpoint "GET" "/pets" in the application "Swagger Petstore"
