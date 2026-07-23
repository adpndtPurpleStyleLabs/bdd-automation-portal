package com.bdd.portal.engine.magento.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemData {

    private String sku;
    private String category;
    private String subCategory;
    private boolean deliveryAwaited;
    private double priceIn;
    private double expectedPrice;

}
