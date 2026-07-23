package com.bdd.portal.engine.magento.stepDefination;

import com.bdd.portal.engine.magento.pages.DashboardPage;
import com.bdd.portal.engine.magento.pages.LoginPage;
import io.cucumber.java.en.Given;
import org.junit.jupiter.api.Assertions;
import com.bdd.portal.engine.DriverManager;
import com.bdd.portal.config.SpringContext;
import com.bdd.portal.service.TestEnvironmentService;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;

public class Login {

    private TestEnvironmentService getTestEnvironmentService() {
        return SpringContext.getBean(TestEnvironmentService.class);
    }

    LoginPage loginPage = new LoginPage();
    private DashboardPage dashboardPage = new DashboardPage();
    private String userName ;

    @Given("User is on the login page")
    public void the_user_is_on_the_login_page() {
        WebDriver driver = DriverManager.getDriver();
        String envKey = DriverManager.getEnvironment();
        
        String url = getTestEnvironmentService().getUrlByName(envKey);
        
        System.out.println("Navigating to URL for env " + envKey + ": " + url);
        driver.get(url);
    }

    @When("User enters valid {string} and {string}")
    public void theUserEntersValidUsernameAndPassword(String userName, String password) {
        this.userName = userName;
        loginPage.login(userName, password);
    }

    @Then("User should be redirected to the homepage")
    public void theUserShouldBeRedirectedToTheHomepage() {
        Assertions.assertTrue(dashboardPage.isLoginSuccessful(userName));
        System.out.println("-------------------Login success --------------------");
    }
}
