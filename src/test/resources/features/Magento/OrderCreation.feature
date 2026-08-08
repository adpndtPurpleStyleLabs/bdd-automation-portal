Feature: Order Creation check

#  @sanity @CustomerDetailPage
#  Scenario: Populate address for all customer types
#    Given User is on the Dashboard page
#    And Logged in user opens order creation
#    Then User should be on customer details page
#    Then User validates all customer types

#    @orderCreation @CartPage @sanity
#  Scenario: Add items in the cart
#      Given User is on the login page
#      When User enters valid "gaurav.dubey@purplestylelabs.com" and "1qaz1qaz"
#      Then User should be redirected to the homepage
#      And Logged in user opens order creation
#      Then User should be on customer details page
#      Then User fills customer using "Random" flow and clicks next
#      Then User navigate to sales information Page
#      When User fill sales information and click next
#        | leadBy     | Gaurav Dubey |
#        | createdBy  | Gaurav Dubey |
#        | source     | Mail         |
#        | orderType  | Online Order |
#        | slipNumber | 888888       |
#      Then User navigate to Item Detail page
#      When User fill item details using testData for Store User
#      Then Product must be added to cart with correct price
#
#  @orderCreation @payment
#  Scenario Outline: All payment method is working fine
#    Given User is on the login page
#    When User enters valid "gaurav.dubey@purplestylelabs.com" and "1qaz1qaz"
#    Then User should be redirected to the homepage
#    And Logged in user opens order creation
#    When User fills customer using "Random" flow and clicks next
#    Then User navigate to sales information Page
#    When User fill sales information and click next
#      | leadBy     | Athira       |
#      | createdBy  | Athira       |
#      | source     | Mail         |
#      | orderType  | Online Order |
#      | slipNumber | 888888       |
#    Then User navigate to Item Detail page
#    When User fill item details using testData for Store User
#    Then Product must be added to cart
#    When User navigate to Payment Detail page
#    Then User must be on Payment Detail Page
#    When User makes payment using "<PaymentMethod>"
#    Then Order must placed successfully with correct Payment Method
#
#    Examples:
#      | PaymentMethod  |
#      | GPay           |
#      | Stripe Payment |

  @IndianStoreOrder @orderCreation
  Scenario Outline: Order creation check for Indian store user
    Given User is on the login page
    When User enters valid "ppusdefcol" and "1qaz2wsx"
    Then User should be redirected to the homepage
    And Logged in user opens order creation
    When User fills customer using "<CustomerType>" with "<AddressType>" address and clicks next
    Then User navigate to sales information Page
    When User fill sales information and click next
      | leadBy     | gaurav dubey |
      | createdBy  | gaurav dubey |
      | source     | Mail         |
      | orderType  | <OrderType>  |
      | slipNumber | 888888       |
    Then User navigate to Item Detail page
    When User fill item details using testData for Store User
    Then Product must be added to cart with correct price
    And Shipping charges must be added on basis of Cart value and AddressType
    And Grand total must be calculated correctly
    When User navigate to Payment Detail page
    Then User must be on Payment Detail Page
    When User makes payment using "<PaymentMethod>"
    Then Order must placed successfully with correct Data

    Examples:
      | CustomerType | AddressType       | PaymentMethod | OrderType |
      | Random       | New-International | GPay          | Delhi     |
      | Random       | New-IN            | GPay          | Delhi     |
      | Random       | Store             | GPay          | Delhi     |
      | Existing     | New-IN            | GPay          | Delhi     |
      | Existing     | New-International | GPay          | Delhi     |
      | Existing     | Store             | GPay          | Delhi     |
      | Existing     | Existing          | GPay          | Delhi     |
      | Dummy        | New-IN            | GPay          | Delhi     |
      | Dummy        | New-International | GPay          | Delhi     |
      | Dummy        | Store             | GPay          | Delhi     |

  @NYCStoreOrder @orderCreation
    Scenario Outline: Order creation check for NYC store user
      Given User is on the login page
      When User enters valid "sachin.mahara@purplestylelabs.com" and "zaq1xsw2"
      Then User should be redirected to the homepage
      And Logged in user opens order creation
      Then User should be on customer details page
      When User fills customer using "<CustomerType>" with "<AddressType>" address and clicks next
      Then User navigate to sales information Page
      When User fill sales information and click next
        | leadBy     | Sachin Mahara |
        | createdBy  | Sachin Mahara |
        | source     | Mail          |
        | orderType  | <OrderType>   |
        | slipNumber | 888888        |
    Then User navigate to Item Detail page
      When User fill item details using testData for Store User
      Then Product must be added to cart with correct price
      And Shipping charges must be added on basis of Cart value and AddressType
      And Sales tax must be calculated
      And Grand total must be calculated correctly
      When User navigate to Payment Detail page
      Then User must be on Payment Detail Page
      When User makes payment using "<PaymentMethod>"
      Then Order must placed successfully with correct Data

    Examples:
      | CustomerType | AddressType       | PaymentMethod | OrderType |
      | Random-NYC   | New-International | GPay          | PPUS NYC  |
      | Random-NYC   | New-IN            | GPay          | PPUS NYC  |
      | Random-NYC   | Store-NYC         | GPay          | PPUS NYC  |
      | Existing     | New-IN            | GPay          | PPUS NYC  |
      | Existing     | New-International | GPay          | PPUS NYC  |
      | Existing     | Store-NYC         | GPay          | PPUS NYC  |
      | Existing     | Existing          | GPay          | PPUS NYC  |
      | Dummy-NYC    | New-IN            | GPay          | PPUS NYC  |
      | Dummy-NYC    | New-International | GPay          | PPUS NYC  |
      | Dummy-NYC    | Store-NYC         | GPay          | PPUS NYC  |


  @LondonStoreOrder @orderCreation
  Scenario Outline: Order creation check for London store user
    Given User is on the login page
      When User enters valid "ppuslondonauto" and "1qaz2wsx"
      Then User should be redirected to the homepage
      And Logged in user opens order creation
      Then User should be on customer details page
      When User fills customer using "<CustomerType>" with "<AddressType>" address and clicks next
      Then User navigate to sales information Page
      When User fill sales information and click next
        | leadBy     | Automation Testing |
        | createdBy  | Automation Testing |
        | source     | Mail               |
        | orderType  | <OrderType>        |
        | slipNumber | 888888             |
    Then User navigate to Item Detail page
      When User fill item details using testData for Store User
      Then Product must be added to cart with correct price
      And Shipping charges must be added on basis of Cart value and AddressType
      And VAT must be calculated
      And Grand total must be calculated correctly
      When User navigate to Payment Detail page
      Then User must be on Payment Detail Page
      When User makes payment using "<PaymentMethod>"
      Then Order must placed successfully with correct Data

    Examples:
      | CustomerType  | AddressType       | PaymentMethod | OrderType   |
      | Random-London | New-International | GPay          | PPUS London |
      | Random-London | New-IN            | GPay          | PPUS London |
      | Random-London | Store-London      | GPay          | PPUS London |
      | Existing      | New-IN            | GPay          | PPUS London |
      | Existing      | New-International | GPay          | PPUS London |
      | Existing      | Store-London      | GPay          | PPUS London |
      | Existing      | Existing          | GPay          | PPUS London |
      | Dummy-London  | New-IN            | GPay          | PPUS London |
      | Dummy-London  | New-International | GPay          | PPUS London |
      | Dummy-London  | Store-London      | GPay          | PPUS London |



  @OnlineOrder @orderCreation
  Scenario Outline: Order creation check for Online Order user
    Given User is on the login page
    When User enters valid "gaurav.dubey@purplestylelabs.com" and "1qaz1qaz"
    Then User should be redirected to the homepage
    And Logged in user opens order creation
    When User select client location "<ClientLocation>"
    Then User should be on customer details page
    When User fills customer using "<CustomerType>" with "<AddressType>" address and clicks next
    Then User navigate to sales information Page
    When User fill sales information and click next
      | leadBy     | gaurav dubey |
      | createdBy  | gaurav dubey |
      | source     | Mail         |
      | orderType  | <OrderType>  |
      | slipNumber | 888888       |
    Then User navigate to Item Detail page
    When User fill item details using testData for Store User
    Then Product must be added to cart with correct price on the basis of client location
    And Shipping charges must be added on basis of Cart value and AddressType
    And Grand total must be calculated correctly
    When User navigate to Payment Detail page
    Then User must be on Payment Detail Page
    When User makes payment using "<PaymentMethod>"
    Then Order must placed successfully with correct Data

    Examples:
      | CustomerType  | AddressType       | PaymentMethod | OrderType    | ClientLocation |
      | Random        | New-International | GPay          | Online Order | IN             |
      | Random        | New-IN            | GPay          | Online Order | IN             |
      | Random        | Store-Online      | GPay          | Online Order | IN             |
      | Existing      | New-IN            | GPay          | Online Order | IN             |
      | Existing      | New-International | GPay          | Online Order | IN             |
#      | Existing      | Store-Online      | GPay          | Online Order | IN             |
      | Existing      | Existing          | GPay          | Online Order | IN             |
      | Dummy         | New-IN            | GPay          | Online Order | IN             |
      | Dummy         | New-International | GPay          | Online Order | IN             |
      | Dummy         | Store-Online      | GPay          | Online Order | IN             |
      | Random        | New-International | GPay          | Online Order | US             |
      | Random        | New-IN            | GPay          | Online Order | US             |
      | Random        | Store-Online      | GPay          | Online Order | US             |
      | Existing      | New-IN            | GPay          | Online Order | US             |
      | Existing      | New-International | GPay          | Online Order | US             |
#      | Existing      | Store-Online      | GPay          | Online Order | US             |
      | Existing      | Existing          | GPay          | Online Order | US             |
      | Dummy         | New-IN            | GPay          | Online Order | US             |
      | Dummy         | New-International | GPay          | Online Order | US             |
      | Dummy         | Store-Online      | GPay          | Online Order | US             |
      | Random        | New-International | GPay          | Online Order | ROW            |
      | Random        | New-IN            | GPay          | Online Order | ROW            |
      | Random        | Store-Online      | GPay          | Online Order | ROW            |
      | Existing      | New-IN            | GPay          | Online Order | ROW            |
      | Existing      | New-International | GPay          | Online Order | ROW            |
#      | Existing      | Store-Online      | GPay          | Online Order | ROW            |
      | Existing      | Existing          | GPay          | Online Order | ROW            |
      | Dummy         | New-IN            | GPay          | Online Order | ROW            |
      | Dummy         | New-International | GPay          | Online Order | ROW            |
      | Dummy         | Store-Online      | GPay          | Online Order | ROW            |

