package com.bdd.portal.engine.magento.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CustomerDetailPage extends BasePage{

    private final By customerDetailtext = By.xpath("//*[@class='box-title' and normalize-space()='Client Details']");
    private final By countryselectionPopUp = By.xpath("//span[@class='currency-model-close']");
    private final By customerEmailTextBox = By.xpath("//*[@id= \"email_id\"]");
    private final By dummyCustomerCheck = By.xpath("//*[@id= \"no_customer_contact\"]");
    private final By mobileNumber = By.id("mobile_no");
    private final By firstName = By.id("first_name");
    private final By lastName = By.id("last_name");
    private final By firstBillingAddressCard = By.xpath("(//div[@data-address-type='billing'])[1]");
    private final By dummyclick = By.xpath("//h3[normalize-space()='Summary']");

    public boolean isOncustomerDetailPage() {
      return isDisplayed(customerDetailtext);
    }

    public void ignoreCountrySelection() {
        waitForClickable(countryselectionPopUp);
        click(countryselectionPopUp);
    }

    public void fillCustomerEmail(String email) {
        type(customerEmailTextBox, email);
    }

    public void selectDummyCustomer() {
        click(dummyCustomerCheck);
        captureScreenshot();
    }

    public String getPhone() {
        return driver.findElement(mobileNumber).getAttribute("value");
    }

    public String getFirstName() {
        return driver.findElement(firstName).getAttribute("value");
    }

    public String getLastName() {
        return driver.findElement(lastName).getAttribute("value");
    }

    private WebElement getBillingCard() {
        return driver.findElement(firstBillingAddressCard);
    }

    public String getBillingCustomerName() {
        List<WebElement> lines = getBillingCard().findElements(By.tagName("p"));
        return lines.get(1).getText();
    }
    public String getBillingAddress() {
        List<WebElement> lines = getBillingCard().findElements(By.tagName("p"));
        return lines.get(2).getText();
    }
    public String getBillingPincode() {
        List<WebElement> lines = getBillingCard().findElements(By.tagName("p"));
        return lines.get(3).getText();
    }
    public String getBillingPhone() {
        List<WebElement> lines = getBillingCard().findElements(By.tagName("p"));
        return lines.get(4).getText();
    }

    public void dummyClick(){
        driver.findElement(dummyclick).click();
        waitForVisible(firstBillingAddressCard);
        captureScreenshot();
    }
}
