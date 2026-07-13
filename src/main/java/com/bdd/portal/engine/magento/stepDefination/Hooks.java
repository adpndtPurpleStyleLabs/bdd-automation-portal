package com.bdd.portal.engine.magento.stepDefination;

import com.bdd.portal.engine.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;

@Slf4j
public class Hooks {

    @Before
    public void setup() {
        String browser = DriverManager.getBrowserType();
        log.info("Initializing WebDriver for browser: {}", browser);
        System.out.println("Initializing WebDriver for browser: " + browser);
        
        try {
            WebDriver driver = null;
            String gridUrlString = DriverManager.getGridUrl();
            if (gridUrlString == null || gridUrlString.isEmpty()) {
                gridUrlString = "http://localhost:5503/wd/hub";
            }
            URL gridUrl = new URL(gridUrlString);
            
            if ("Firefox".equalsIgnoreCase(browser)) {
                driver = new RemoteWebDriver(gridUrl, new FirefoxOptions());
            } else if ("Edge".equalsIgnoreCase(browser)) {
                driver = new RemoteWebDriver(gridUrl, new EdgeOptions());
            } else {
                ChromeOptions options = new ChromeOptions();
                driver = new RemoteWebDriver(gridUrl, options);
            }
            
            DriverManager.setDriver(driver);
            System.out.println("WebDriver initialized successfully from Grid.");
        } catch (Exception e) {
            log.error("Failed to initialize WebDriver from Grid", e);
            System.err.println("Failed to initialize WebDriver: " + e.getMessage());
            throw new RuntimeException("Could not initialize RemoteWebDriver", e);
        }
    }

    @After
    public void teardown() {
        log.info("Quitting WebDriver...");
        System.out.println("Quitting WebDriver...");
        DriverManager.quitDriver();
    }
}
