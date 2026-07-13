package com.bdd.portal.engine;

import org.openqa.selenium.WebDriver;

public class DriverManager {
    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final ThreadLocal<String> browserType = new ThreadLocal<>();
    private static final ThreadLocal<String> gridUrl = new ThreadLocal<>();
    private static final ThreadLocal<String> environment = new ThreadLocal<>();

    public static void setBrowserType(String browser) { 
        browserType.set(browser); 
    }
    
    public static String getBrowserType() { 
        return browserType.get() != null ? browserType.get() : "Chrome"; 
    }
    
    public static void setDriver(WebDriver d) { 
        driver.set(d); 
    }
    
    public static WebDriver getDriver() { 
        return driver.get(); 
    }
    
    public static void setGridUrl(String url) {
        gridUrl.set(url);
    }
    
    public static String getGridUrl() {
        return gridUrl.get();
    }
    
    public static void setEnvironment(String env) {
        environment.set(env);
    }
    
    public static String getEnvironment() {
        return environment.get();
    }
    
    public static void quitDriver() {
        if (driver.get() != null) {
            driver.get().quit();
            driver.remove();
        }
    }
    
    public static void removeBrowserType() {
        browserType.remove();
        gridUrl.remove();
        environment.remove();
    }
}
