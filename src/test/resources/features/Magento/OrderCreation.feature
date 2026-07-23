Feature: Order Creation check

#  @sanity @CustomerDetailPage
#  Scenario: Populate address for all customer types
#    Given User is on the Dashboard page
#    And Logged in user opens order creation
#    Then User should be on customer details page
#    Then User validates all customer types

    @orderCreation @CartPage @sanity
  Scenario: Add items in the cart
      Given User is on the login page
      When User enters valid "gaurav.dubey@purplestylelabs.com" and "1qaz1qaz"
      Then User should be redirected to the homepage
      And Logged in user opens order creation
      Then User should be on customer details page
      Then User fills customer using "Random" flow and clicks next
      Then User navigate to sales information Page
      When User fill sales information and click next
        | leadBy     | Gaurav Dubey |
        | createdBy  | Gaurav Dubey |
        | source     | Mail         |
        | orderType  | Online Order |
        | slipNumber | 888888       |
      Then User navigate to Item Detail page
      When User fill item details using testData
      Then Product must be added to cart with correct price

  @orderCreation @payment
  Scenario Outline: All payment method is working fine
    Given User is on the login page
    When User enters valid "gaurav.dubey@purplestylelabs.com" and "1qaz1qaz"
    Then User should be redirected to the homepage
    And Logged in user opens order creation
    When User fills customer using "Random" flow and clicks next
    Then User navigate to sales information Page
    When User fill sales information and click next
      | leadBy     | Athira       |
      | createdBy  | Athira       |
      | source     | Mail         |
      | orderType  | Online Order |
      | slipNumber | 888888       |
    Then User navigate to Item Detail page
    When User fill item details using testData
    Then Product must be added to cart
    When User navigate to Payment Detail page
    Then User must be on Payment Detail Page
    When User makes payment using "<PaymentMethod>"
    Then Order must placed successfully with correct Payment Method

    Examples:
      | PaymentMethod  |
      | GPay           |
      | Stripe Payment |

  @IndianStoreOrder @orderCreation
  Scenario Outline: Order creation check for Indian store user
    Given User is on the login page
    When User enters valid "gaurav.dubey@purplestylelabs.com" and "1qaz1qaz"
    Then User should be redirected to the homepage
    And Logged in user opens order creation
    When User fills customer using "<CustomerType>" flow and clicks next
    Then User navigate to sales information Page
    When User fill sales information and click next
      | leadBy     | Gaurav Dubey |
      | createdBy  | Gaurav Dubey |
      | source     | Mail         |
      | orderType  | <OrderType>  |
      | slipNumber | 888888       |
    Then User navigate to Item Detail page
    When User fill item details using testData
    Then Product must be added to cart with correct price
    When User navigate to Payment Detail page
    Then User must be on Payment Detail Page
    When User makes payment using "<PaymentMethod>"
    Then Order must placed successfully with correct Payment Method

    Examples:
      | CustomerType | PaymentMethod | OrderType |
      | Random       | GPay          | Delhi     |
      | Existing     | GPay          | Delhi     |
      | Dummy        | GPay          | Delhi     |

  @NYCStoreOrder
    Scenario Outline: Order creation check for NYC store user
      Given User is on the login page
      When User enters valid "sachin.mahara@purplestylelabs.com" and "zaq1xsw2"
      Then User should be redirected to the homepage
      And Logged in user opens order creation
      Then User should be on customer details page
      Then User fills customer using "<CustomerType>" flow and clicks next
      Then User navigate to sales information Page
      When User fill sales information and click next
        | leadBy     | Bindi Pandya |
        | createdBy  | Bindi Pandya |
        | source     | Mail         |
        | orderType  | <OrderType>  |
        | slipNumber | 888888       |
    Then User navigate to Item Detail page
      When User fill item details using testData
      Then Product must be added to cart with correct price
      When User navigate to Payment Detail page
      Then User must be on Payment Detail Page
      When User makes payment using "<PaymentMethod>"
      Then Order must placed successfully with correct Payment Method

      Examples:
        | CustomerType | PaymentMethod | OrderType |
        | Random-NYC   | GPay          | PPUS NYC  |
        | Dummy-NYC    | GPay          | PPUS NYC  |
        | Existing     | GPay          | PPUS NYC  |


