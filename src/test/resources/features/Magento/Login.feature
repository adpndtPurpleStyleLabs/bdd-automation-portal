Feature: User Login
  Scenario: Successful login with valid credentials
    Given User is on the login page
    When User enters valid "gaurav.dubey@purplestylelabs.com" and "1qaz1qaz"
    Then User should be redirected to the homepage
