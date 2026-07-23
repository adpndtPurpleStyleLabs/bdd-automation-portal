package com.bdd.portal.engine.magento.pages;

import com.bdd.portal.engine.magento.utils.ItemData;
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
}
