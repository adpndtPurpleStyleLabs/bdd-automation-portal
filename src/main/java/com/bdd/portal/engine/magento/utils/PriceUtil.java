package com.bdd.portal.engine.magento.utils;

public class PriceUtil {

    public static String getCurrencyCode(String orderType) {

        orderType = orderType.toLowerCase();

        if (orderType.contains("ppus nyc")) {
            return "usd";
        }

        if (orderType.contains("london")) {
            return "gbp";
        }

        return "inr";
    }

    public static double calculatePrice(double basePrice,
                                        CurrencyData currency) {

        double price = basePrice
                * currency.getMultiplier()
                * currency.getRate();

        return Math.round(price * 100.0) / 100.0;
    }
}
