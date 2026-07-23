package com.bdd.portal.engine.magento.pages;

import com.bdd.portal.engine.magento.utils.CustomerData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import java.util.List;
import java.util.Random;

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
    private final By addNewAddress = By.cssSelector("[data-address-id='add_new_address']");
    private final By billingPincode = By.xpath("//*[@id = 'billing_postcode']");
    private final By countryDropdown = By.id("billing_country_id");
    private final By stateDropdown = By.id("billing_region_id");
    private final By stateTextBox = By.id("billing_state");
    private final By cityTextBox = By.id("billing_city");
    private final By billingMobileNumber = By.id("billing_mobile_no");
    private final By billingAddressTextArea = By.id("billing_street");
    private final By addAddressDetailButton = By.id("add_address_collection");
    By customerCredit = By.id("customerCredit");

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

    private WebElement getBillingCard(int cardIndex) {
        By billingCard = By.xpath("(//div[@data-address-type='billing'])[" + cardIndex + "]");
        return driver.findElement(billingCard);
    }

    public String getBillingCustomerName(int cardIndex) {
        List<WebElement> lines = getBillingCard(cardIndex).findElements(By.tagName("p"));
        return lines.get(1).getText();
    }
    public String getBillingAddress(int cardIndex) {
        List<WebElement> lines = getBillingCard(cardIndex).findElements(By.tagName("p"));
        return lines.get(2).getText();
    }
    public String getBillingPincode(int cardIndex) {
        List<WebElement> lines = getBillingCard(cardIndex).findElements(By.tagName("p"));
        return lines.get(3).getText();
    }
    public String getBillingPhone(int cardIndex) {
        List<WebElement> lines = getBillingCard(cardIndex).findElements(By.tagName("p"));
        return lines.get(4).getText();
    }

    public void dummyClick(){
        driver.findElement(dummyclick).click();
        waitForVisible(firstBillingAddressCard);
        captureScreenshot();
    }

    public void fillCustomerDetail(String email) {
        ignoreCountrySelection();
        fillCustomerEmail(email);
        dummyClick();
    }

    public void clickNext() {
        captureScreenshot();
        driver.findElement(By.xpath("//*[@class=\"next\"]")).click();
    }

    public void selectState(String state) {

        if (driver.findElements(stateDropdown).size() > 0
                && driver.findElement(stateDropdown).isDisplayed()) {

            new Select(driver.findElement(stateDropdown))
                    .selectByVisibleText(state);

        } else if (driver.findElements(stateTextBox).size() > 0
                && driver.findElement(stateTextBox).isDisplayed()) {

            WebElement textBox = wait.until(
                    ExpectedConditions.refreshed(
                            ExpectedConditions.elementToBeClickable(stateTextBox)));

            textBox.clear();
            textBox.sendKeys(state);

        } else {
            throw new RuntimeException("No state field found.");
        }
    }

    private void fillRandomCustomer(CustomerData customer) {

        type(customerEmailTextBox, customer.getEmail());
        dummyClick();
        wait.until(ExpectedConditions.attributeContains(
                customerCredit,
                "style",
                "display: none"));
        type(mobileNumber, customer.getPhone());
        type(firstName, customer.getFirstName());
        type(lastName, customer.getLastName());
        dummyClick();

        wait.until(ExpectedConditions.alertIsPresent());
        acceptAlert();
        scrollIntoView(addNewAddress);
        waitForClickable(addNewAddress);
        click(addNewAddress);
        waitForClickable(billingPincode);
        type(billingPincode, customer.getBillingPincode());

        selectByVisibleText(countryDropdown, customer.getCountry());
        waitForLoaderToDisappear();
        selectState(customer.getState());

        clear(cityTextBox);
        type(cityTextBox, customer.getCity());
        type(billingMobileNumber, customer.getPhone());
        type(billingAddressTextArea, customer.getBillingAddress());
        click(addAddressDetailButton);
        wait.until(ExpectedConditions.alertIsPresent());
        acceptAlert();
        captureScreenshot();
        clickNext();
    }

    public void fillExistingCustomer(CustomerData customer) {

        type(customerEmailTextBox, customer.getEmail());

        dummyClick();

        wait.until(ExpectedConditions.attributeContains(
                customerCredit,
                "style",
                "display: none"));
        captureScreenshot();
        clickNext();
    }

    public void fillDummyCustomer() {
        selectDummyCustomer();
        wait.until(ExpectedConditions.attributeContains(
                customerCredit,
                "style",
                "display: none"));
        int randomInt = new Random().nextInt(100000);
        type(firstName, "Dummy"+ randomInt);
        type(lastName, "Automation" + randomInt);
        dummyClick();
        wait.until(ExpectedConditions.alertIsPresent());
        acceptAlert();
        captureScreenshot();
        clickNext();
    }

    public void fillCustomer(String customerType,
                             CustomerData customer) {

        switch (customerType.toLowerCase()) {

            case "random", "random-nyc":
                fillRandomCustomer(customer);
                break;

            case "existing":
                fillExistingCustomer(customer);
                break;

            case "dummy", "dummy-nyc" :
                fillDummyCustomer();
                break;


            default:
                throw new IllegalArgumentException("Invalid customer type");
        }
    }
}
