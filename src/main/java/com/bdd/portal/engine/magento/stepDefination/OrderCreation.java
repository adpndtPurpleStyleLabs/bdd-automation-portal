package com.bdd.portal.engine.magento.stepDefination;

import com.bdd.portal.config.SpringContext;
import com.bdd.portal.engine.DriverManager;
import com.bdd.portal.engine.magento.pages.CustomerDetailPage;
import com.bdd.portal.engine.magento.pages.DashboardPage;
import com.bdd.portal.engine.magento.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import com.bdd.portal.service.TestEnvironmentService;
import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.TestDataReader;
import org.openqa.selenium.WebDriver;

import java.io.InputStream;
import java.util.List;

@Slf4j
public class OrderCreation {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("testdata/customerData.json");
    private TestEnvironmentService getTestEnvironmentService() {
        return SpringContext.getBean(TestEnvironmentService.class);
    }

    private DashboardPage dashboardPage = new DashboardPage();
    private LoginPage loginPage = new LoginPage();
    private CustomerDetailPage customerDetailPage = new CustomerDetailPage();
    private String userName = "gaurav.dubey@purplestylelabs.com";
    private String password = "1qaz1qaz";

    @Given("User is on the Dashboard page")
    public void theUserIsOnTheDashboardPage() {
        WebDriver driver = DriverManager.getDriver();

        String envKey = DriverManager.getEnvironment();

        String url = getTestEnvironmentService().getUrlByName(envKey);

        System.out.println("Navigating to URL for env " + envKey + ": " + url);
        driver.get(url);

        loginPage.login(userName, password);
        Assertions.assertTrue(dashboardPage.isLoginSuccessful(userName));
    }

    @Then("Logged in user opens order creation")
    public void logged_in_user_opens_order_creation() {

        dashboardPage.openCreateOrderPage();

    }

    @Then("User should be on customer details page")
    public void user_should_be_on_customer_details_page() {
        Assertions.assertTrue(customerDetailPage.isOncustomerDetailPage());
        customerDetailPage.ignoreCountrySelection();
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

    private void validateCustomer(CustomerData customer) {

        if (customer.getExpectedPhone() != null) {
            Assertions.assertEquals(
                    customer.getExpectedPhone(),
                    customerDetailPage.getPhone());
        }

        if (customer.getExpectedFirstName() != null) {
            Assertions.assertEquals(
                    customer.getExpectedFirstName(),
                    customerDetailPage.getFirstName());
        }

        if (customer.getExpectedLastName() != null) {
            Assertions.assertEquals(
                    customer.getExpectedLastName(),
                    customerDetailPage.getLastName());
        }

        if (customer.getBillingName() != null) {
            Assertions.assertEquals(
                    customer.getBillingName(),
                    customerDetailPage.getBillingCustomerName());
        }

        if (customer.getBillingAddress() != null) {
            Assertions.assertEquals(
                    customer.getBillingAddress(),
                    customerDetailPage.getBillingAddress());
        }

        if (customer.getBillingPhone() != null) {
            Assertions.assertEquals(
                    customer.getBillingPhone(),
                    customerDetailPage.getBillingPhone());
        }
    }
}
