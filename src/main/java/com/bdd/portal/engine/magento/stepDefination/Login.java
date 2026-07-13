package com.bdd.portal.engine.magento.stepDefination;

import io.cucumber.java.en.Given;

import com.bdd.portal.engine.DriverManager;
import com.bdd.portal.engine.magento.constants.Environments;
import org.openqa.selenium.WebDriver;

public class Login {
    @Given("the user is on the login page")
    public void the_user_is_on_the_login_page() {
        WebDriver driver = DriverManager.getDriver();
        String envKey = DriverManager.getEnvironment();
        String url = Environments.getUrlByEnvKey(envKey);
        
        System.out.println("Navigating to URL for env " + envKey + ": " + url);
        driver.get(url);
    }
}
