package com.bdd.portal.engine.magento.stepDefination;

import com.bdd.portal.engine.DriverManager;
import com.bdd.portal.engine.reporting.StepLogger;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

@Slf4j
public class Hooks {

    @Before
    public void beforeScenario(Scenario scenario) {
        StepLogger.log("Starting Scenario: " + scenario.getName());
        WebDriver driver = DriverManager.getDriver();
        if (driver != null) {
            StepLogger.takeScreenshot(driver);
        }
    }

    @After
    public void afterScenario(Scenario scenario) {
        StepLogger.log("Ending Scenario: " + scenario.getName() + " - Status: " + scenario.getStatus());
        WebDriver driver = DriverManager.getDriver();
        if (driver != null) {
            StepLogger.takeScreenshot(driver);
        }
    }
}
