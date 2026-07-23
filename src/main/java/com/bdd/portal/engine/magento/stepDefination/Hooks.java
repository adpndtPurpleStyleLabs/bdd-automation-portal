package com.bdd.portal.engine.magento.stepDefination;

import com.bdd.portal.config.SpringContext;
import com.bdd.portal.engine.DriverManager;
import com.bdd.portal.engine.reporting.StepLogger;
import com.bdd.portal.entity.Execution;
import com.bdd.portal.repository.ExecutionRepository;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.core.env.Environment;

@Slf4j
public class Hooks {

    @Before
    public void beforeScenario(Scenario scenario) {
        StepLogger.log("Starting Scenario: " + scenario.getName());
        
        String executionIdStr = System.getProperty("current.execution.id");
        if (executionIdStr != null) {
            Long executionId = Long.parseLong(executionIdStr);
            ExecutionRepository executionRepository = SpringContext.getBean(ExecutionRepository.class);
            Execution execution = executionRepository.findById(executionId).orElse(null);
            if (execution != null) {
                String browser = execution.getBrowser();
                Environment env = SpringContext.getBean(Environment.class);
                String gridUrl = env.getProperty("bdd.portal.selenium-grid-url");
                String publicVncBaseUrl = env.getProperty("vnc.public.base-url", "http://localhost:7900");
                
                WebDriver driver;
                try {
                    if (gridUrl != null && !gridUrl.isEmpty()) {
                        java.net.URL gridHubUrl = new java.net.URL(gridUrl);
                        if ("Firefox".equalsIgnoreCase(browser)) {
                            driver = new org.openqa.selenium.remote.RemoteWebDriver(gridHubUrl, new org.openqa.selenium.firefox.FirefoxOptions());
                        } else if ("Edge".equalsIgnoreCase(browser)) {
                            driver = new org.openqa.selenium.remote.RemoteWebDriver(gridHubUrl, new org.openqa.selenium.edge.EdgeOptions());
                        } else {
                            org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
                            driver = new org.openqa.selenium.remote.RemoteWebDriver(gridHubUrl, options);
                        }
                        
                        org.openqa.selenium.Capabilities caps = ((org.openqa.selenium.remote.RemoteWebDriver) driver).getCapabilities();
                        execution.setSeleniumSessionId(((org.openqa.selenium.remote.RemoteWebDriver) driver).getSessionId().toString());
                        execution.setBrowserVersion(caps.getBrowserVersion());
                        if (caps.getPlatformName() != null) {
                            execution.setPlatform(caps.getPlatformName().name());
                        }
                        
                        String publicVncUrl = publicVncBaseUrl.endsWith("/") ? 
                                publicVncBaseUrl.substring(0, publicVncBaseUrl.length() - 1) : publicVncBaseUrl;
                        publicVncUrl += "/?autoconnect=1&password=secret&resize=scale";
                        
                        execution.setNoVncUrl(publicVncUrl);
                        execution.setVncUrl(publicVncUrl);
                        
                        executionRepository.save(execution);
                        
                        com.bdd.portal.service.WebSocketNotificationService notificationService = SpringContext.getBean(com.bdd.portal.service.WebSocketNotificationService.class);
                        notificationService.broadcastExecutionUpdate(execution);
                        
                        log.info("WebDriver initialized successfully from Grid.");
                    } else {
                        log.info("Grid URL not found. Initializing local DEV ChromeDriver.");
                        org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
                        options.addArguments("--remote-allow-origins=*");
                        options.addArguments("disable-notifications");
                        options.addArguments("start-maximized");
                        options.addArguments("--disable-notifications");
                        driver = io.github.bonigarcia.wdm.WebDriverManager.chromedriver().capabilities(options).create();
                    }
                    
                    driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(60));
                    driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
                    
                    DriverManager.setBrowserType(browser);
                    DriverManager.setGridUrl(gridUrl);
                    DriverManager.setEnvironment(execution.getEnvironment());
                    DriverManager.setDriver(driver);
                    
                } catch (Exception e) {
                    log.error("Failed to initialize WebDriver", e);
                    throw new RuntimeException("Execution failed due to WebDriver initialization error", e);
                }
            }
        } else {
            // Fallback for local execution from IDE
            log.info("Execution ID not found. Initializing local DEV ChromeDriver.");
            org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("disable-notifications");
            options.addArguments("start-maximized");
            options.addArguments("--disable-notifications");
            WebDriver driver = io.github.bonigarcia.wdm.WebDriverManager.chromedriver().capabilities(options).create();
            driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(60));
            driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
            DriverManager.setDriver(driver);
            DriverManager.setBrowserType("Chrome");
        }

        WebDriver driver = DriverManager.getDriver();
        if (driver != null) {
            driver.get("https://google.com/");
            driver.manage().window().maximize();
            StepLogger.takeScreenshot(driver);
        }
    }

    @After
    public void afterScenario(Scenario scenario) {
        StepLogger.log("Ending Scenario: " + scenario.getName() + " - Status: " + scenario.getStatus());
        WebDriver driver = DriverManager.getDriver();
        if (driver != null) {
            StepLogger.takeScreenshot(driver);
            DriverManager.quitDriver();
            DriverManager.removeBrowserType();
        }
    }
}
