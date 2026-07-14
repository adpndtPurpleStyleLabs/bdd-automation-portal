package com.bdd.portal.engine.magento.pages;

import org.openqa.selenium.By;

public class DashboardPage extends BasePage {

    private final By dashboardText = By.xpath("//span[normalize-space()='Dashboard']");

    private By loggedInUser(String username) {
        return By.xpath("//p[@class='super'][contains(.,'Logged in as " + username + "')]");
    }
    private final By logoutButton = By.xpath("//a[normalize-space()='Log Out']");

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
}
