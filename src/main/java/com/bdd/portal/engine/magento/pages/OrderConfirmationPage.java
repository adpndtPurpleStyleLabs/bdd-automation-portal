package com.bdd.portal.engine.magento.pages;

import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.ItemData;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.Arrays;
import java.util.List;

public class OrderConfirmationPage extends BasePage{

    private final By orderViewText = By.xpath("//h3[normalize-space()='Order View']");

    private final By accountInfoText = By.xpath("//h4[normalize-space()='Account Information']");

    private final By email = By.xpath("//td[@class='value']//a[starts-with(@href,'mailto:')]/strong");
    private final By getStatus = By.xpath("//*[@id = 'order_status']");
    private final By skuList = By.xpath("//div[strong[normalize-space()='SKU:']]");
    private By paymentGateway = By.xpath("//b[normalize-space()='Payment Gateway:']/ancestor::li[1]");
    private By customerName = By.xpath("//td[normalize-space()='Customer Name']/following-sibling::td//strong");
    private final By billingAddressBlock = By.xpath("//div[contains(@class,'box-left')]//address");
    private final By subTotal =
            By.xpath("//td[normalize-space()='Subtotal']/following-sibling::td//span[@class='price']");

    public boolean isOnOrderConfirmationPage () {
        captureScreenshot();
        return isDisplayed(orderViewText) && isDisplayed(accountInfoText);
    }

    public String getCustomerEmail() {
        scrollIntoView(email);
        captureScreenshot();
        return getText(email).trim();
    }

    public String getOrderStatus() {
        scrollIntoView(getStatus);
        captureScreenshot();
        return getText(getStatus);
    }

    private List<String> getBillingAddressLines() {

        String address = getText(billingAddressBlock);

        return Arrays.stream(address.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    public String getPaymentGateway() {
        String text = driver.findElement(paymentGateway).getText();
        captureScreenshot();
        int start = text.indexOf("Payment Gateway:") + "Payment Gateway:".length();
        int end = text.indexOf("Txn.Id :");

        return text.substring(start, end).trim();
    }

    public String getCustomerName() {
        scrollIntoView(customerName);
        captureScreenshot();
        return getText(customerName).trim();
    }

    public String getBillingName() {
        return getBillingAddressLines().get(0);
    }

    public String getBillingAddress() {
        return getBillingAddressLines().get(1);
    }

    public String getCity() {
        String[] parts = getBillingAddressLines().get(2).split(",");
        return parts[0].trim();
    }

    public String getState() {
        String[] parts = getBillingAddressLines().get(2).split(",");
        return parts[1].trim();
    }

    public String getPincode() {
        String[] parts = getBillingAddressLines().get(2).split(",");
        return parts[2].trim();
    }

    public String getCountry() {
        return getBillingAddressLines().get(3);
    }

    public String getBillingPhone() {
        return getBillingAddressLines()
                .get(4)
                .replace("T:", "")
                .trim();
    }

    public double getSubTotal(String currencyCode) {

        List<WebElement> prices = driver.findElements(subTotal);

        String subtotal;

        if ("usd".equalsIgnoreCase(currencyCode)) {
            subtotal = prices.get(1).getText();   // $918.28
        } else {
            subtotal = prices.get(0).getText();   // ₹78,418.45
        }

        return Double.parseDouble(
                subtotal.replace("₹", "")
                        .replace("$", "")
                        .replace("[", "")
                        .replace("]", "")
                        .replace(",", "")
                        .trim());
    }

    private boolean isDummyCustomer(CustomerData customer) {
        return "DummyCustomer".equalsIgnoreCase(customer.getType())
                || "DummyCustomer-NYC".equalsIgnoreCase(customer.getType());
    }

    public void verifyCustomer(CustomerData customer) {

        scrollIntoView(billingAddressBlock);
        captureScreenshot();
        if (!isDummyCustomer(customer)) {
            Assertions.assertEquals(
                    customer.getEmail(),
                    getCustomerEmail(),
                    "Customer Email doesn't match.");
        }

        if (!isDummyCustomer(customer)) {
            Assertions.assertEquals(
                    customer.getFirstName() + " " + customer.getLastName(),
                    getCustomerName(),
                    "Customer Name doesn't match.");
        }
        if (!isDummyCustomer(customer)) {
            Assertions.assertEquals(
                    customer.getBillingName(),
                    getBillingName());
        }
        if (!isDummyCustomer(customer)) {
            Assertions.assertEquals(
                    customer.getBillingPhone(),
                    getBillingPhone());
        }

        Assertions.assertEquals(
                customer.getBillingAddress(),
                getBillingAddress());

        Assertions.assertEquals(
                customer.getCity(),
                getCity());

        Assertions.assertEquals(
                customer.getState(),
                getState());

        Assertions.assertEquals(
                customer.getCountry(),
                getCountry());

        Assertions.assertEquals(
                customer.getBillingPincode(),
                getPincode(), "Billing Pincode doesn't match.");
    }

    public String getDisplayedSku(String sku) {

        WebElement skuElement = driver.findElement(
                By.xpath("//strong[normalize-space()='SKU:']/parent::div[contains(.,'" + sku + "')]"));

        return skuElement.getText()
                .replace("SKU:", "")
                .trim();
    }

    private WebElement getItemRow(String sku) {
        return driver.findElement(
                By.xpath("//strong[normalize-space()='SKU:']/parent::div[contains(.,'" + sku + "')]/ancestor::tr"));
    }

    public double getPrice(String sku, String currencyCode) {

        WebElement row = getItemRow(sku);

        List<WebElement> prices = row.findElements(
                By.xpath("./td[2]//span[@class='price']"));

        String price;

        if ("usd".equalsIgnoreCase(currencyCode)) {
            price = prices.get(1).getText();   // [$167.39]
        } else {
            price = prices.get(0).getText();   // ₹14,294.50
        }

        return Double.parseDouble(
                price.replace("₹", "")
                        .replace("$", "")
                        .replace("[", "")
                        .replace("]", "")
                        .replace(",", "")
                        .trim());
    }

    public void verifyItems(List<ItemData> items, String currencyCode) {

        scrollIntoView(skuList);
        captureScreenshot();
        for (ItemData item : items) {

            Assertions.assertEquals(
                    item.getSku(),
                    getDisplayedSku(item.getSku()),
                    "SKU mismatch.");

            Assertions.assertEquals(
                    item.getExpectedPrice(),
                    getPrice(item.getSku(), currencyCode),
                    0.01,
                    "Price mismatch for SKU : " + item.getSku());
        }
    }
}
