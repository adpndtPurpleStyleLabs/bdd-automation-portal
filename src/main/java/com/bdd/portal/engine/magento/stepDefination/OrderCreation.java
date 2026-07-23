package com.bdd.portal.engine.magento.stepDefination;

import com.bdd.portal.config.SpringContext;
import com.bdd.portal.engine.DriverManager;
import com.bdd.portal.engine.magento.pages.*;
import com.bdd.portal.engine.magento.utils.*;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import com.bdd.portal.service.TestEnvironmentService;
import org.openqa.selenium.WebDriver;
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

//    @Then("User fill customer {string} and click next")
//    public void userFillCustomerDetailsAndClickNext(String customerEmail) {
////        expectedCustomerEmail = customerEmail;
//        customerDetailPage.fillCustomerDetail(customerEmail);
//        customerDetailPage.clickNext();
//    }

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

    @When("User fill item details using testData")
    public void userFillItemDetailsUsingTestData() throws Exception{

        List<ItemData> items = TestDataReader.getAllItem();
        orderContext.setItems(items);

        String currencyCode = PriceUtil.getCurrencyCode(orderContext.getOrderType());
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

    @Then("Product must be added to cart")
    public void productMustBeAddedToCart() {
        int actualItemCount = itemDetailsPage.getCartItemCount();
        Assertions.assertEquals(orderContext.getItems().size(), actualItemCount);
    }


//    helper ---------------------------------------------

    private void validateCustomer(CustomerData customer) {

        int billingCard = customer.getType().equals("ExistingCustomer") ? 1 : 1;

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

    @Then("Order must placed successfully with correct Payment Method")
    public void orderMustPlacedSuccessfullyWithCorrectPaymentMethod() {
        Assertions.assertTrue(orderConfirmationPage.isOnOrderConfirmationPage());
        orderConfirmationPage.verifyCustomer(orderContext.getCustomer());
        Assertions.assertEquals(
                "Processing",
                orderConfirmationPage.getOrderStatus(),
                "Status is not Processing."
        );

        String currencyCode = PriceUtil.getCurrencyCode(orderContext.getOrderType());
        orderConfirmationPage.verifyItems(orderContext.getItems(), currencyCode);
        Assertions.assertEquals(
                orderContext.getPayment().getType(),
                orderConfirmationPage.getPaymentGateway(),
                "Payment Gateway does not match."
        );
        Assertions.assertEquals(
                orderContext.getExpectedSubTotal(),
                orderConfirmationPage.getSubTotal(currencyCode),
                0.01,
                "Grand Total doesn't match."
        );
    }

    @When("User fills customer using {string} flow and clicks next")
    public void userFillsCustomer(String customerType) throws Exception {
        CustomerData customer;

        switch (customerType.toLowerCase()) {

            case "existing":
                customer =
                        TestDataReader.getCustomerByType("ExistingCustomer");
                break;

            case "dummy":
                customer = TestDataReader.getCustomerByType("DummyCustomer");
                break;

            case "random":
                customer =
                        RandomCustomerGenerator.generateIndianCustomer();
                customer.setBillingName(
                        customer.getFirstName() + " " + customer.getLastName());
                customer.setBillingPhone(customer.getPhone());
                break;

            case "random-nyc":
                customer =
                        RandomCustomerGenerator.generateInternationalCustomer();
                customer.setBillingName(
                        customer.getFirstName() + " " + customer.getLastName());
                customer.setBillingPhone(customer.getPhone());
                break;

            case "dummy-nyc":
                customer = TestDataReader.getCustomerByType("DummyCustomer-NYC");
                break;

            default:
                throw new RuntimeException();
        }
        orderContext.setCustomer(customer);
        customerDetailPage.fillCustomer(customerType, customer);
    }

    @Then("Product must be added to cart with correct price")
    public void productMustBeAddedToCartWithCorrectPrice() {
        itemDetailsPage.verifyCart(orderContext.getItems());
    }


}
