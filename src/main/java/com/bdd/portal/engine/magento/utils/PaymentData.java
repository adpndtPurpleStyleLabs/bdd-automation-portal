package com.bdd.portal.engine.magento.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentData {

    private String transactionId;
    private String paymentLink;
    private String invoiceId;
    private String type;

}
