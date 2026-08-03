package com.bdd.portal.engine.magento.customer.factory;

import com.bdd.portal.engine.magento.customer.address.*;

public class AddressFactory {

    public static AddressStrategy getStrategy(
            String addressType){

        return switch(addressType.toLowerCase()){

            case "existing" ->
                    new ExistingAddressStrategy();

            case "store" ->
                    new StoreAddressStrategy();

            case "new-in" ->
                    new NewIndianAddressStrategy();

            case "new-international" ->
                    new NewInternationalAddressStrategy();

            case "store-nyc" ->
                new StoreNycStrategy();

            case "store-london" ->
                new StoreLondonStategy();

            default ->
                    throw new RuntimeException();

        };

    }

}
