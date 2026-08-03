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
    private final By paymentGateway = By.xpath("//b[normalize-space()='Payment Gateway:']/ancestor::li[1]");
    private final By customerName = By.xpath("//td[normalize-space()='Customer Name']/following-sibling::td//strong");
    private final By billingAddressBlock = By.xpath("//div[contains(@class,'box-left')]//address");
    private final By subTotal = By.xpath("//td[normalize-space()='Subtotal']/following-sibling::td//span[@class='price']");
    private final By shipping = By.xpath("//td[normalize-space()='Shipping & Handling']/following-sibling::td//span[@class='price']");
    private final By salesTax = By.xpath("//td[normalize-space()='Sales Tax']/following-sibling::td//span[@class='price']");
    private final By vat = By.xpath("//td[normalize-space()='VAT']/following-sibling::td//span[@class='price']");
    private final By grandTotal = By.xpath("//td[normalize-space()='Grand Total']/following-sibling::td//span[@class='price']");

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

    public double getPrice(By locator, String currencyCode) {

        List<WebElement> prices = driver.findElements(locator);

        if (prices.isEmpty()) {
            throw new RuntimeException("No price found for locator: " + locator);
        }

        String price;

        if ("inr".equalsIgnoreCase(currencyCode)) {
            price = prices.get(0).getText();
        } else {
            if (prices.size() == 1) {
                // Item table: only one price (GBP/USD/AED)
                price = prices.get(0).getText();
            } else {
                // Order totals: index 0 = INR, index 1 = website currency
                price = prices.get(1).getText();
            }
        }

        return Double.parseDouble(
                price.replace("₹", "")
                        .replace("$", "")
                        .replace("£", "")
                        .replace("[", "")
                        .replace("]", "")
                        .replace(",", "")
                        .trim());
    }

    public double getSubTotal(String currencyCode) {
        captureScreenshot();
        return getPrice(subTotal, currencyCode);
    }

    public double getShipping(String currencyCode) {
        captureScreenshot();
        return getPrice(shipping, currencyCode);
    }

    public double getSalesTax(String currencyCode) {
        captureScreenshot();
        return getPrice(salesTax, currencyCode);
    }

    public double getVat(String currencyCode) {
        captureScreenshot();
        return getPrice(vat, currencyCode);
    }

    public double getGrandtotal(String currencyCode) {
        captureScreenshot();
        return getPrice(grandTotal, currencyCode);
    }

    private boolean isDummyCustomer(CustomerData customer) {
        return "DummyCustomer".equalsIgnoreCase(customer.getType())
                || "DummyCustomer-NYC".equalsIgnoreCase(customer.getType())
                || "DummyCustomer-London".equalsIgnoreCase(customer.getType());
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

        if ("inr".equalsIgnoreCase(currencyCode)) {
            price = prices.get(0).getText();
        } else {
            if (prices.size() == 1) {
                // Item table: only one price (GBP/USD/AED)
                price = prices.get(0).getText();
            } else {
                // Order totals: index 0 = INR, index 1 = website currency
                price = prices.get(1).getText();
            }
        }

        return Double.parseDouble(
                price.replace("₹", "")
                        .replace("$", "")
                        .replace("£", "")
                        .replace("[", "")
                        .replace("]", "")
                        .replace(",", "")
                        .trim());
    }

    private double getItemTax(String sku, int columnIndex) {

        WebElement row = getItemRow(sku);

        List<WebElement> prices = row.findElements(
                By.xpath("./td[" + columnIndex + "]//span[@class='price']"));

        String price = prices.size() == 1
                ? prices.get(0).getText()
                : prices.get(1).getText();

        return Double.parseDouble(
                price.replace("₹", "")
                        .replace("$", "")
                        .replace("£", "")
                        .replace("[", "")
                        .replace("]", "")
                        .replace(",", "")
                        .trim());
    }

    public void verifyItems(List<ItemData> items, String currencyCode,
                            String orderType) {

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

            if ("ppus-nyc".equalsIgnoreCase(orderType)) {

                Assertions.assertEquals(
                        item.getExpectedSalesTax(),
                        getItemTax(item.getSku(), 7),
                        0.01,
                        "Sales Tax mismatch for SKU : " + item.getSku());

            } else if ("ppus-london".equalsIgnoreCase(orderType)) {

                Assertions.assertEquals(
                        item.getExpectedVat(),
                        getItemTax(item.getSku(), 7),
                        0.01,
                        "VAT mismatch for SKU : " + item.getSku());
            }
        }
    }
}
