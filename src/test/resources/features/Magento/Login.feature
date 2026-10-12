Feature: User Login
  Scenario: Successful login with valid credentials
    Given User is on the login page
    When User enters valid credentials for "NYC"
    Then User should be redirected to the homepage

  Scenario: Successful login with valid credentials
    Given User is on the login page
    When User enters valid credentials for "London"
    Then User should be redirected to the homepage

  Scenario: Successful login with valid credentials
    Given User is on the login page
    When User enters valid credentials for "India"
    Then User should be redirected to the homepage

  Scenario: Successful login with valid credentials
    Given User is on the login page
    When User enters valid credentials for "Online"
    Then User should be redirected to the homepage