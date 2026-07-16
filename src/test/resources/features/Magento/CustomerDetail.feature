Feature: Order Creation check

  @sanity @CustomerDetailPage
  Scenario: Populate address for all customer types
    Given User is on the Dashboard page
    Then Logged in user opens order creation
    Then User should be on customer details page
    Then User validates all customer types