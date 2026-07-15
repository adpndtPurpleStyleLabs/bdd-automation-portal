package com.bdd.portal.engine.magento.stepDefination;

import com.bdd.portal.engine.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Value;

import java.net.URL;
import java.time.Duration;

@Slf4j
public class Hooks {

    private final String environment;

    public Hooks(@Value("${test.environment}") String environment) {
        this.environment = environment;
    }

    @Before
    public void setup() {
        String browser = DriverManager.getBrowserType();
        log.info("Initializing WebDriver for browser: {}", browser);
        System.out.println("Initializing WebDriver for browser: " + browser);
        
        try {
            WebDriver driver = null;
            if(true){
                ChromeOptions options =  new ChromeOptions();
                options.addArguments("--remote-allow-origins=*");
                options.addArguments("disable-notifications");
                options.addArguments("start-maximized");
                options.addArguments("--disable-notifications");
                driver = WebDriverManager.chromedriver().capabilities(options).create();
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
                DriverManager.setDriver(driver);
                return;
            }

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
