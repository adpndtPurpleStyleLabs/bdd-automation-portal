package com.bdd.portal.engine.magento.stepDefination;

import com.bdd.portal.config.SpringContext;
import com.bdd.portal.engine.DriverManager;
import com.bdd.portal.engine.magento.customer.service.CustomerService;
import com.bdd.portal.engine.magento.pages.*;
import com.bdd.portal.engine.magento.utils.*;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import com.bdd.portal.service.TestEnvironmentService;
import java.util.List;
import java.util.Map;

@Slf4j
public class OrderCreation {
    private TestEnvironmentService getTestEnvironmentService() {
        return SpringContext.getBean(TestEnvironmentService.class);
    }

    private final DashboardPage dashboardPage = new DashboardPage();
    private final LoginPage loginPage = new LoginPage();
    private final CustomerDetailPage customerDetailPage = new CustomerDetailPage();
    private final SalesInformationPage salesInformationPage = new SalesInformationPage();
    private final ItemDetailsPage itemDetailsPage = new ItemDetailsPage();
    private final PaymentDetailPage paymentDetailPage = new PaymentDetailPage();
    private final OrderConfirmationPage orderConfirmationPage = new OrderConfirmationPage();
    private final OrderContext orderContext = new OrderContext();
    private final CustomerService customerService = new CustomerService();

    @And("Logged in user opens order creation")
    public void logged_in_user_opens_order_creation() {
        dashboardPage.openCreateOrderPage();
    }

    @Then("User should be on customer details page")
    public void user_should_be_on_customer_details_page() {
        Assertions.assertTrue(customerDetailPage.isOncustomerDetailPage());
//        customerDetailPage.ignoreCountrySelection();
    }

    @Then("User validates all customer types")
    public void userValidatesAllCustomerTypes() throws Exception {

        List<CustomerData> customers = TestDataReader.getAllCustomers();
        for (CustomerData customer : customers) {

            log.info("Executing {}", customer);

            switch (customer.getType()) {

                case "ExistingCustomer", "NonExistingCustomer":
                    customerDetailPage.fillCustomerEmail(customer.getEmail());
                    customerDetailPage.dummyClick();

                    break;

                case "DummyCustomer":

                    customerDetailPage.selectDummyCustomer();
                    break;
            }

            validateCustomer(customer);
            DriverManager.getDriver().navigate().refresh();
            customerDetailPage.ignoreCountrySelection();
        }
    }

    @Then("User navigate to sales information Page")
    public void userNavigateToSalesInformationPage() {
        Assertions.assertTrue(salesInformationPage.isOnSalesInformatioPage());
    }

    @When("User fill sales information and click next")
    public void userFillSalesInformationAndClickNext(DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        String orderType = data.get("orderType");
        orderContext.setOrderType(orderType);
        //salesInformationPage.setOrderType(data.get("orderType"));
        salesInformationPage.setLeadBy(data.get("leadBy"));
        salesInformationPage.setCreatedBy(data.get("createdBy"));
        salesInformationPage.setSourceBy(data.get("source"));
        salesInformationPage.setSlipNumber(data.get("slipNumber"));
        salesInformationPage.clickNext();
    }

    @Then("User navigate to Item Detail page")
    public void userNavigateToItemDetailPage() {
        itemDetailsPage.isOnItemDetailsPage();
    }

    @When("User fill item details using testData for Store User")
    public void userFillItemDetailsUsingTestDataforStoreUser() throws Exception{

        List<ItemData> items = TestDataReader.getAllItem();
        orderContext.setItems(items);

        String currencyCode = PriceUtil.getCurrencyCode(orderContext.getOrderType());
        orderContext.setCurrencyCode(currencyCode);
        CurrencyData currencyData = TestDataReader.getCurrencyData(currencyCode);

        for (ItemData item : items) {

            double convertedPrice = PriceUtil.calculatePrice(
                    item.getPriceIn(),
                    currencyData);

            item.setExpectedPrice(convertedPrice);

            itemDetailsPage.addItem(
                    item.getSku(),
                    item.getCategory(),
                    item.getSubCategory(),
                    item.isDeliveryAwaited()
            );
        }
        double expectedSubTotal = items.stream()
                .mapToDouble(ItemData::getExpectedPrice)
                .sum();

        orderContext.setExpectedSubTotal(expectedSubTotal);
    }

    @When("User fill item details using testData for Online User")
    public void userFillItemDetailsUsingTestDataForOnlineUser() throws Exception{

        List<ItemData> items = TestDataReader.getAllItem();
        orderContext.setItems(items);

    }

    @Then("Product must be added to cart")
    public void productMustBeAddedToCart() {
        int actualItemCount = itemDetailsPage.getCartItemCount();
        Assertions.assertEquals(orderContext.getItems().size(), actualItemCount);
    }


//    helper ---------------------------------------------

    private void validateCustomer(CustomerData customer) {

        int billingCard = customer.getType().equals("ExistingCustomer") ? 1 : 2;

        if (customer.getExpectedPhone() != null) {
            Assertions.assertEquals(
                    customer.getExpectedPhone(),
                    customerDetailPage.getPhone(),  "Phone number didn't match for customer: " + customer.getEmail());
        }

        if (customer.getExpectedFirstName() != null) {
            Assertions.assertEquals(
                    customer.getExpectedFirstName(),
                    customerDetailPage.getFirstName(),
                    "First Name didn't match for customer: " + customer.getEmail());
        }

        if (customer.getExpectedLastName() != null) {
            Assertions.assertEquals(
                    customer.getExpectedLastName(),
                    customerDetailPage.getLastName(),
                    "Last Name didn't match for customer: " + customer.getEmail());
        }

        if (customer.getBillingName() != null) {
            Assertions.assertEquals(
                    customer.getBillingName(),
                    customerDetailPage.getBillingCustomerName(billingCard),
                    "Billing Name didn't match for customer: " + customer.getEmail());
        }

        if (customer.getBillingAddress() != null) {
            Assertions.assertEquals(
                    customer.getBillingAddress(),
                    customerDetailPage.getBillingAddress(billingCard),
                    "Billing Address didn't match for customer: " + customer.getEmail());
        }

        if (customer.getBillingPhone() != null) {
            Assertions.assertEquals(
                    customer.getBillingPhone(),
                    customerDetailPage.getBillingPhone(billingCard),
                    "Billing Phone number didn't match for customer: " + customer.getEmail());
        }

        if (customer.getBillingPincode() != null) {
            Assertions.assertEquals(
                    customer.getBillingPincode(),
                    customerDetailPage.getBillingPincode(billingCard),
                    "Billing Pin-code didn't match for customer: " + customer.getEmail());
        }
    }

    @Then("User navigate to Payment Detail page")
    public void userNavigateToPaymentDetailPage() {
        itemDetailsPage.clickNext();
    }

    @Then("User must be on Payment Detail Page")
    public void userMustBeOnPaymentDetailPage() {
        Assertions.assertTrue(paymentDetailPage.isOnPaymentDetailPage());
    }

    @When("User makes payment using {string}")
    public void userMakesPaymentUsing(String paymentMethod) throws Exception {

        PaymentData paymentData = TestDataReader.getPaymentData(paymentMethod);
        paymentData.setType(paymentMethod);
        orderContext.setPayment(paymentData);

        paymentDetailPage.makePayment(
                    paymentMethod,
                    paymentData.getTransactionId(),
                    paymentData.getPaymentLink(),
                    paymentData.getInvoiceId()
            );
        }

    @Then("Order must placed successfully with correct Data")
    public void orderMustPlacedSuccessfullyWithCorrectData() {
        Assertions.assertTrue(orderConfirmationPage.isOnOrderConfirmationPage());
        orderConfirmationPage.verifyCustomer(orderContext.getCustomer());
        Assertions.assertEquals(
                "Processing",
                orderConfirmationPage.getOrderStatus(),
                "Status is not Processing."
        );

        String currencyCode = PriceUtil.getCurrencyCode(orderContext.getOrderType());
        orderConfirmationPage.verifyItems(orderContext.getItems(), currencyCode, orderContext.getOrderType());
        Assertions.assertEquals(
                orderContext.getPayment().getType(),
                orderConfirmationPage.getPaymentGateway(),
                "Payment Gateway does not match expected "+orderContext.getPayment().getType()
                + "got " + orderConfirmationPage.getPaymentGateway()
        );
        Assertions.assertEquals(
                orderContext.getExpectedSubTotal(),
                orderConfirmationPage.getSubTotal(currencyCode),
                0.01,
                "Sub Total doesn't match."
        );

        Assertions.assertEquals(
                orderContext.getShippingAmount(),
                orderConfirmationPage.getShipping(currencyCode),
                0.01,
                "Shipping doesn't match."
        );

        if ("ppus-nyc".equalsIgnoreCase(orderContext.getOrderType())) {

            Assertions.assertEquals(
                    orderContext.getExpectedSalesTax(),
                    orderConfirmationPage.getSalesTax(currencyCode),
                    0.01,
                    "Sales Tax mismatched."
            );

        } else if ("ppus-london".equalsIgnoreCase(orderContext.getOrderType())) {

            Assertions.assertEquals(
                    orderContext.getExpectedVat(),
                    orderConfirmationPage.getVat(currencyCode),
                    0.01,
                    "VAT mismatched."
            );
        }

        Assertions.assertEquals(
                orderContext.getExpectedgrandTotal(),
                orderConfirmationPage.getGrandtotal(currencyCode),
                0.01,
                "Grand Total mismatched."
        );
    }

    @When("User fills customer using {string} with {string} address and clicks next")

    public void userFillsCustomer(
            String customerType,
            String addressType)
            throws Exception {

        CustomerData customer =
                customerService.createCustomer(
                        customerType.toLowerCase(),
                        addressType.toLowerCase());

        orderContext.setCustomer(customer);

        customerDetailPage.fillCustomer(
                customerType.toLowerCase(),
                addressType.toLowerCase(),
                customer);

    }

    @Then("Product must be added to cart with correct price")
    public void productMustBeAddedToCartWithCorrectPrice() {
        itemDetailsPage.verifyCart(orderContext.getItems());
    }

    @And("Shipping charges must be added on basis of Cart value and AddressType")
    public void shippingChargesMustBeAddedOnBasisOfCartValueAndAddressType() throws Exception {

        String orderType = orderContext.getOrderType();
        CustomerData customer = orderContext.getCustomer();

        if (orderType.equalsIgnoreCase("ppus nyc" )
            || orderType.equalsIgnoreCase("ppus london")) {
            switch(customer.getAddresstype()) {
                case "new-international", "store-nyc", "store-london":
                    double cartValue = orderContext.getExpectedSubTotal();
                    String currency = orderContext.getCurrencyCode();
                    CurrencyData currencyData = TestDataReader.getCurrencyData(currency);
                    double shippingAmt = itemDetailsPage.calculateShipping(cartValue, currencyData);
                    orderContext.setShippingAmount(shippingAmt);
                    itemDetailsPage.verifyShippingAmount(shippingAmt);
                    break;
            }
        } else if ((customer.getAddresstype()).equalsIgnoreCase("new-international") &&
                !(orderType.equalsIgnoreCase("ppus nyc" )
                        || orderType.equalsIgnoreCase("ppus london"))) {

            double cartValue = orderContext.getExpectedSubTotal();
            String currency = orderContext.getCurrencyCode();
            CurrencyData currencyData = TestDataReader.getCurrencyCalculator(currency);
            double shippingAmt = itemDetailsPage.calculateShipping(cartValue, currencyData);
            orderContext.setShippingAmount(shippingAmt);
            itemDetailsPage.verifyShippingAmount(shippingAmt);
        }
    }

    @And("Grand total must be calculated correctly")
    public void GrandTotalMustBeCalculatedCorrectly() {

        double grandTotal = itemDetailsPage.calculateGrandTotal(orderContext.getExpectedSubTotal(),
                orderContext.getShippingAmount(), orderContext.getExpectedSalesTax(),
                orderContext.getExpectedVat());
        orderContext.setExpectedgrandTotal(grandTotal);
        itemDetailsPage.verifyGrandTotal(grandTotal);
    }

    @And("Sales tax must be calculated")
    public void salesTaxMustBeCalculated() throws Exception {

        List<ItemData> items = orderContext.getItems();
        double salesTaxPercentage = 8.8750;
        CurrencyData currencyData = TestDataReader.getCurrencyData(orderContext.getCurrencyCode());
        double salesTax = itemDetailsPage.calculateSalesTax(items, salesTaxPercentage, currencyData);
        orderContext.setExpectedSalesTax(salesTax);
        itemDetailsPage.verifySalesTax(items);
    }

    @And("VAT must be calculated")
    public void vatMustBeCalculated() throws  Exception {
        List<ItemData> items = orderContext.getItems();
        double vatPercentage = 20;
        CurrencyData currencyData = TestDataReader.getCurrencyData(orderContext.getCurrencyCode());
        double vat = itemDetailsPage.calculateVat(items, vatPercentage, currencyData);
        orderContext.setExpectedVat(vat);
        itemDetailsPage.verifyVat(items);
    }

    @When("User select client location {string}")
    public void userSelectClientLocation(String countryCode) {
        customerDetailPage.selectCustomerCountry(countryCode);
        orderContext.setClientLocation(countryCode);
    }

    @Then("Product must be added to cart with correct price on the basis of client location")
    public void productMustBeAddedToCartWithCorrectPriceOnTheBasisOfClientLocation() {
        List<ItemData> items = orderContext.getItems();
        String clientLocation = orderContext.getClientLocation();

        for(ItemData item : items) {
            switch(clientLocation) {
                case "US":
                    item.setExpectedPrice(item.getPriceUs());
                    break;

                case "ROW":
                    item.setExpectedPrice(item.getPriceRow());
                    break;

                case "IN":
                    item.setExpectedPrice(item.getPriceIn());
                    break;

                default:
                    throw new RuntimeException("Client Location is Not specified " + clientLocation);
            }
        }
        double expectedSubTotal = items.stream()
                .mapToDouble(ItemData::getExpectedPrice)
                .sum();

        orderContext.setExpectedSubTotal(expectedSubTotal);

        itemDetailsPage.verifyCart(orderContext.getItems());
    }
}
