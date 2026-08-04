package com.bdd.portal.engine.magento.pages;

//import com.bdd.portal.engine.magento.utils.CurrencyCalculator;
import com.bdd.portal.engine.magento.utils.CurrencyData;
import com.bdd.portal.engine.magento.utils.ItemData;
import jakarta.mail.FetchProfile;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

public class ItemDetailsPage extends BasePage{

    private final By itemDetailsText = By.id("h2-item-details-title");
    private final By itemSearchButton = By.id("searchProductBySku");
    private final By searchSkuBox = By.id("searchsku_textbox");
    private final By searchEnter = By.id("search_enter_sku");
    private final By addToCartButton = By.id("online_add_to_cart");
    private final By updateItemButton = By.id("update_item_details");
    private final By itemCheckBox = By.xpath("//input[contains(@class,'order-check-box')]");
    private final By categoryDropdowns = By.xpath("(//select[starts-with(@id,'change_category_name_')])[last()]");

    private final By subCategoryDropdown = By.xpath(
                    "(//span[@role='combobox'][.//span[contains(@id,'select2-change_sub_category')]])[last()]");

    private final By deliveryAwaitedCheckbox = By.cssSelector("input.change_delivery_awaited:last-of-type");
    private final By cartItems = By.xpath("//div[contains(@class,'itemcartsection')]");
    private final By cartShipping = By.id("cart_shipping");
    private final By cartGrandTotal = By.xpath("//*[@id='cart_gand_total']");

    public boolean isOnItemDetailsPage() {
        return isDisplayed(itemDetailsText);
    }

    public void addItem(String sku,
                        String category,
                        String subCategory,
                        boolean deliveryAwaited) {

        click(itemSearchButton);

        type(searchSkuBox, sku);

        click(searchEnter);

        waitForLoaderToDisappear();

        click(itemCheckBox);

        click(addToCartButton);

        waitForLoaderToDisappear();

        scrollToLastCartItem();

        setCategory(category);

        setSubCategory(subCategory);

        if (deliveryAwaited) {
            setDeliveryAwaited(deliveryAwaited);
        }

        click(updateItemButton);

        waitForLoaderToDisappear();

        captureScreenshot();

        logStep("Added SKu" + sku +"in the cart");
    }

    public void setCategory(String category) {

        WebElement categoryDropdown = waitForVisible(categoryDropdowns);

        Select select = new Select(categoryDropdown);
        select.selectByVisibleText(category);
    }

    public void setSubCategory(String subCategory) {

        click(subCategoryDropdown);

        By option = By.xpath(
                "//li[@role='treeitem' and normalize-space()='" + subCategory + "']");

        waitForClickable(option);
        click(option);
    }

    public void setDeliveryAwaited(boolean deliveryAwaited) {

        if (!deliveryAwaited) {
            return;
        }

        List<WebElement> checkboxes =
                driver.findElements(By.cssSelector("input.change_delivery_awaited"));

        WebElement checkbox = checkboxes.getLast();

        if (!checkbox.isSelected()) {
            checkbox.click();
        }
    }

    public int getCartItemCount() {
        return driver.findElements(cartItems).size();
    }

    public void clickNext() {
        captureScreenshot();
        driver.findElement(By.xpath("//*[@class=\"next\"]")).click();
    }

    private String normalizeSku(String sku) {
        return sku.replace("-BO-", "-");
    }

    public void verifyCart(List<ItemData> expectedItems) {

        List<WebElement> cartItems = driver.findElements(this.cartItems);

        Assertions.assertEquals(
                expectedItems.size(),
                cartItems.size(),
                "Cart item count mismatch");

        for (ItemData expected : expectedItems) {

            boolean found = false;

            for (WebElement cartItem : cartItems) {

                String expectedSku = normalizeSku(expected.getSku());
                String actualSku = normalizeSku(
                        cartItem.findElement(By.cssSelector("input[id^='item_sku_']"))
                                .getAttribute("value"));

                if (actualSku.equals(expectedSku)) {

                    double actualPrice = Double.parseDouble(
                            cartItem.findElement(
                                            By.cssSelector("label[id^='price_']"))
                                    .getText());

                    Assertions.assertEquals(
                            expected.getExpectedPrice(),
                            actualPrice,
                            0.01,
                            "Price mismatch for SKU : " + expected.getSku());

                    found = true;
                    break;
                }
            }

            Assertions.assertTrue(
                    found,
                    "SKU not found : " + expected.getSku());
        }
    }

    public double calculateShipping(double cartValue, CurrencyData currencyData) {

        double cartValueInr = (cartValue / (currencyData.getRate() * currencyData.getMultiplier()));
        double shippingAmt = 0;

        if(cartValueInr <= 4000) {
            shippingAmt = 2000 * currencyData.getRate() * currencyData.getMultiplier();
            return shippingAmt;
        } else if(cartValueInr > 4000 && cartValueInr <= 15000) {
            shippingAmt = 1444 * currencyData.getRate();
            return shippingAmt;
        }else{
            return shippingAmt;
        }
    }

    public void verifyShippingAmount(double expectedShippinAmount) {
        String shippingAmt = getValue(cartShipping);
        double shippingAmount = Double.parseDouble(shippingAmt);
        captureScreenshot();
        Assertions.assertEquals(expectedShippinAmount,
                shippingAmount,
                0.01,
                "Shipping amount mismatched on cart page");
    }

    public void verifyGrandTotal(double grandTotal) {
        String netPayable = getAttribute(cartGrandTotal, "data-netpayable");
        double actualGrandTotal = Double.parseDouble(netPayable);

        Assertions.assertEquals(grandTotal,
                actualGrandTotal,
                0.01,
                "Grand Total mismatched on cart page actual "+ actualGrandTotal + "expected" + grandTotal);
    }

    public double calculateGrandTotal(double subTotal, double shipping, double salesTax, double vat) {

        double grandTotal = (subTotal+shipping+salesTax+vat);
        return grandTotal;
    }

    public double calculateSalesTax(List<ItemData> items, double salesTaxPercentage,
                                    CurrencyData currencyData) {

        for(ItemData item : items) {
            double price = (item.getPriceIn() * currencyData.getMultiplier());
            double st = (price * (salesTaxPercentage/100));
            double salestax = st * currencyData.getRate();
            item.setExpectedSalesTax(salestax);
        }
       return items.stream()
                .mapToDouble(ItemData::getExpectedSalesTax)
                .sum();
    }

    public double calculateVat(List<ItemData>items, double vatPercentage,
                               CurrencyData currencyData) {

        for(ItemData item : items) {
            double price = item.getPriceIn() * currencyData.getMultiplier();
            double vt = (price * (vatPercentage/100));
            double vat = vt * currencyData.getRate();
            item.setExpectedVat(vat);
        }
        return items.stream()
                .mapToDouble(ItemData::getExpectedVat)
                .sum();
    }

    public void verifyVat(List<ItemData>expectedItems) {
        List<WebElement> cartItems = driver.findElements(this.cartItems);
        captureScreenshot();

        for (ItemData expected : expectedItems) {

            boolean found = false;

            for (WebElement cartItem : cartItems) {

                String expectedSku = normalizeSku(expected.getSku());
                String actualSku = normalizeSku(
                        cartItem.findElement(By.cssSelector("input[id^='item_sku_']"))
                                .getAttribute("value"));

                if (actualSku.equals(expectedSku)) {

                    double actualVat = Double.parseDouble(
                            cartItem.findElement(
                                            By.xpath("//label[starts-with(@id,'vat_amount_label_')]"))
                                    .getText());
                    Assertions.assertEquals(
                            expected.getExpectedVat(),
                            actualVat,
                            0.01,
                            "Vat mismatch for SKU : " + expected.getSku());

                    found = true;
                    break;
                }
            }
        }
    }

    public void verifySalesTax(List<ItemData>expectedItems) {

        List<WebElement> cartItems = driver.findElements(this.cartItems);
        captureScreenshot();

        for (ItemData expected : expectedItems) {

            boolean found = false;

            for (WebElement cartItem : cartItems) {

                String expectedSku = normalizeSku(expected.getSku());
                String actualSku = normalizeSku(
                        cartItem.findElement(By.cssSelector("input[id^='item_sku_']"))
                                .getAttribute("value"));

                if (actualSku.equals(expectedSku)) {

                    double actualSalesTax = Double.parseDouble(
                            cartItem.findElement(
                                            By.xpath("//label[starts-with(@id,'vat_amount_label_')]"))
                                    .getText());
                    Assertions.assertEquals(
                            expected.getExpectedSalesTax(),
                            actualSalesTax,
                            0.01,
                            "Sales Tax mismatch for SKU : " + expected.getSku());

                    found = true;
                    break;
                }
            }
        }
    }
}
