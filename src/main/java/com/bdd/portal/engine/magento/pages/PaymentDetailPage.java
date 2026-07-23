package com.bdd.portal.engine.magento.pages;

import org.openqa.selenium.By;

import java.util.Map;

public class PaymentDetailPage extends BasePage{

    private final By paymentDetailText = By.xpath("//h3[@class='box-title' and normalize-space()='Payment Details']");
    private final By cartTotalPaid = By.id("cart_total_paid");
    private final By submitButton = By.id("order_submit_button");
    private static final Map<String, String> PAYMENT_ID_MAP = Map.ofEntries(
            Map.entry("Cash", "cash"),
            Map.entry("Mswipe", "mswipe"),
            Map.entry("GPay", "GPay"),
            Map.entry("SumUp", "sumup"),
            Map.entry("Razor Pay", "razorpay"),
            Map.entry("Bank Transfer", "banktransfer"),
            Map.entry("PRE - PAYMENT BY CASH", "prepaymentbycash"),
            Map.entry("PayPal", "paypal"),
            Map.entry("Ccavenue", "ccavenue"),
            Map.entry("Payu", "payu"),
            Map.entry("Cash on Delivery", "cashondelivery"),
            Map.entry("Razorpay", "Razorpay"),
            Map.entry("Stripe Payment", "stripepayment"),
            Map.entry("Cheque", "cheque"),
            Map.entry("Yes Bank POS", "yesbankpos"),
            Map.entry("HDFC Bank POS", "hdfcbankpos"),
            Map.entry("Net Banking", "netbanking"),
            Map.entry("PayZone", "payzone"),
            Map.entry("BarclaysPDQ", "barclayspdq"),
            Map.entry("Pinelabs", "pinelabs"),
            Map.entry("Square", "square"),
            Map.entry("Chase", "chase")
    );

    private By paymentCheckbox(String id) {
        return By.id("p_method_" + id);
    }

    private By amountField(String id) {
        return By.id("select_payment_amount_" + id);
    }

    private By transactionField(String id) {
        return By.id("transaction_id_" + id);
    }

    private By paymentLinkField(String id) {
        return By.id("payment_link_" + id);
    }

    private By invoiceField(String id) {
        return By.id("invoice_id_" + id);
    }

    public boolean isOnPaymentDetailPage() {
       captureScreenshot();
        return isDisplayed(paymentDetailText);
    }

    public void makePayment(String paymentMethod,
                            String transactionId,
                            String paymentLink,
                            String invoiceId) {

        String paymentId = PAYMENT_ID_MAP.get(paymentMethod);

        if (paymentId == null) {
            throw new RuntimeException("No payment ID mapping found for: " + paymentMethod);
        }

        waitForLoaderToDisappear();

        String amount = getText(cartTotalPaid)
                .replaceAll("[^0-9.]", "")
                .trim();

        scrollIntoView(paymentCheckbox(paymentId));
        waitForClickable(paymentCheckbox(paymentId));
        click(paymentCheckbox(paymentId));

        type(amountField(paymentId), amount);
        captureScreenshot();
        if (transactionId != null && !transactionId.isBlank()) {
            type(transactionField(paymentId), transactionId);
            captureScreenshot();
        }

        if (paymentLink != null && !paymentLink.isBlank()) {
            type(paymentLinkField(paymentId), paymentLink);
            captureScreenshot();
        }

        if (invoiceId != null && !invoiceId.isBlank()) {
            type(invoiceField(paymentId), invoiceId);
            captureScreenshot();
        }
        captureScreenshot();
        click(submitButton);
        waitForLoaderToDisappear();
    }
}
