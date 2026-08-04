package com.bdd.portal.engine.magento.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SalesInformationPage extends BasePage{

    private final By salesInfo = By.xpath("//*[@class='box-title' and normalize-space()='Sales Information']");
    private final By leadByDropdown = By.id("select2-lead_by-container");
    private final By leadBySearch = By.xpath("//input[@class='select2-search__field']");
    private final By createdByDropdown = By.id("select2-created_by-container");
    private final By createdBySearch = By.xpath("//input[@class='select2-search__field']");
    private final By sourceByDropdown = By.id("select2-source_by-container");
    private final By select2SearchBox = By.xpath("//input[@class='select2-search__field']");
//    private final By orderTypeDropdown = By.id("select2-order_type-container");
//    private final By select2SearchhBox = By.xpath("//input[@class='select2-search__field']");
    private final By slipNumber = By.id("order_comment");

    public boolean isOnSalesInformatioPage() {
      return isDisplayed(salesInfo);
    }

    public void setLeadBy(String leadBy) {
        click(leadByDropdown);
        type(leadBySearch, leadBy);

        By option = By.xpath("//li[@role='treeitem' and normalize-space()='" + leadBy + "']");
        click(option);
    }

    public void setCreatedBy(String createdBy) {
        click(createdByDropdown);
        type(createdBySearch, createdBy);

        By option = By.xpath("//li[@role='treeitem' and normalize-space()='" + createdBy + "']");
        click(option);
    }

    public void setSourceBy(String sourceBy) {
        click(sourceByDropdown);

        if (isDisplayed(select2SearchBox)) {
            type(select2SearchBox, sourceBy);
        }

        By option = By.xpath("//li[@role='treeitem' and normalize-space()='" + sourceBy + "']");
        click(option);
    }

//    public void setOrderType(String orderType) {
//        click(orderTypeDropdown);
//
//        if (isDisplayed(select2SearchhBox)) {
//            type(select2SearchhBox, orderType);
//        }
//
//        By option = By.xpath("//li[@role='treeitem' and normalize-space()='" + orderType + "']");
//        click(option);
//    }

    public void setSlipNumber(String comment) {
        type(slipNumber, comment);
    }

    public void clickNext() {
        captureScreenshot();
        driver.findElement(By.xpath("//*[@class=\"next\"]")).click();
    }
}
