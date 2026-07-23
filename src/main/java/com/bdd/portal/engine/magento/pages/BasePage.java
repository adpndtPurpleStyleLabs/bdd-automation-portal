package com.bdd.portal.engine.magento.pages;

import com.bdd.portal.engine.DriverManager;
import org.openqa.selenium.*;

import java.time.Duration;
import java.util.NoSuchElementException;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
public class BasePage {

    protected WebDriver driver;
    protected WebDriverWait wait;

    public BasePage() {
        this.driver = DriverManager.getDriver();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected void click(By locator) {
        logStep("Clicking element: " + locator.toString());
        waitForClickable(locator).click();
    }

    protected void type(By locator, String value) {
        logStep("Typing '" + value + "' into element: " + locator.toString());
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(value);
    }

    protected String getText(By locator) {
        return waitForVisible(locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        return waitForVisible(locator).isDisplayed();
    }

    protected void waitForUrlContains(String text) {
        wait.until(ExpectedConditions.urlContains(text));
    }

    protected void logStep(String message) {
        com.bdd.portal.engine.reporting.StepLogger.log(message);
    }

    protected void captureScreenshot() {
        com.bdd.portal.engine.reporting.StepLogger.takeScreenshot(driver);
    }

    protected void waitForLoaderToDisappear() {

        By loader = By.id("loading-mask");

        try {
            wait.until(driver -> {
                try {
                    WebElement element = driver.findElement(loader);

                    String style = element.getAttribute("style");

                    return style == null || !style.contains("display: block");

                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    return true;
                }
            });

        } catch (TimeoutException e) {
            logStep("Loader did not disappear within timeout.");
        }
    }

    public void scrollToLastCartItem() {
     try {
         ((JavascriptExecutor) driver).executeScript(
                 "window.scrollBy(0, document.body.scrollHeight);"
         );
         Thread.sleep(2000);
     } catch (InterruptedException e) {
         throw new RuntimeException(e);
     }
    }

    protected WebDriver driver() {
        return driver;
    }

    protected void scrollIntoView(By locator) {
        WebElement element = waitForVisible(locator);

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                element);
    }

    public void selectByVisibleText(By locator, String text) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        new Select(element).selectByVisibleText(text);
    }

    public void acceptAlert() {
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        System.out.println("Alert Message: " + alert.getText());
        alert.accept();
    }

    public void clear(By locator) {
        WebElement element = wait.until(
                ExpectedConditions.elementToBeClickable(locator));
        element.clear();
    }
}
