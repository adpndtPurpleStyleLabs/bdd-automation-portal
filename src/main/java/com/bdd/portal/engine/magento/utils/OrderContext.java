package com.bdd.portal.engine.magento.utils;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderContext {

    private CustomerData customer;

    private List<ItemData> items;

    private PaymentData payment;

    private double expectedSubTotal;

    private String orderId;

    private String orderType;
}
