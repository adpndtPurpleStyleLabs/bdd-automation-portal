package com.bdd.portal.engine.magento.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.NoSuchElementException;

public class DashboardPage extends BasePage {

    private final By dashboardText = By.xpath("//span[normalize-space()='Dashboard']");
    private final By customerDetailtext = By.xpath("//*[@class='box-title' and normalize-space()='Client Details']");


    private By loggedInUser(String username) {
        return By.xpath("//p[@class='super'][contains(.,'Logged in as " + username + "')]");
    }
    private final By logoutButton = By.xpath("//a[normalize-space()='Log Out']");

    private final By salesMenu = By.xpath("//span[normalize-space()='Sales']");

    private final By createOrderButton = By.xpath("//li[contains(@class,'level1')]//span[normalize-space()='Create Order']");

    public boolean isDashboardDisplayed() {
        return isDisplayed(dashboardText);
    }

    public boolean isLogoutButtonDisplayed() {
        return isDisplayed(logoutButton);
    }

    public boolean isLoginSuccessful(String expectedUser) {

        return isDashboardDisplayed()
                && getText(loggedInUser(expectedUser)).contains("Logged in as " + expectedUser)
                && isLogoutButtonDisplayed();
    }

    public void openCreateOrderPage() {
        driver.findElement(salesMenu).click();
        List<WebElement> buttons = driver.findElements(createOrderButton);
        if(buttons.isEmpty()){
            throw new NoSuchElementException("Create Order button not found");
        }
        buttons.getFirst().click();
        waitForVisible(customerDetailtext);
        captureScreenshot();
    }
}
