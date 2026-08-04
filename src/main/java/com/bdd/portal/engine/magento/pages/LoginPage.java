package com.bdd.portal.engine.magento.pages;

import com.bdd.portal.engine.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage{

    private final WebDriver driver;

    public LoginPage() {
        this.driver = DriverManager.getDriver();
    }

    private final By txtUsername = By.id("username");
    private final By txtPassword = By.id("login");
    private final By btnLogin = By.xpath("//input[@type='submit' and @value='Login']");

    public void enterUsername(String username) {
        driver.findElement(txtUsername).clear();
        driver.findElement(txtUsername).sendKeys(username);
    }

    public void enterPassword(String password) {
        driver.findElement(txtPassword).clear();
        driver.findElement(txtPassword).sendKeys(password);
    }

    public void clickLogin() {
        driver.findElement(btnLogin).click();
    }

    public void login(String userName, String password) {
        enterUsername(userName);
        logStep("Entered username " + userName);
        enterPassword(password);
        logStep("Entered password " + password);
        clickLogin();

        waitForUrlContains("uspp-admin/dashboard");
        captureScreenshot();
    }
}
