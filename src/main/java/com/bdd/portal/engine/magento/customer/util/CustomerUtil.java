package com.bdd.portal.engine.magento.customer.util;

import com.bdd.portal.engine.magento.utils.CustomerData;

public class CustomerUtil {

    public static void initializeBilling(CustomerData customer){

        customer.setBillingName(
                customer.getFirstName()
                        + " "
                        + customer.getLastName());

        customer.setBillingPhone(
                customer.getPhone());

    }

    public static void copyAddress(
            CustomerData source,
            CustomerData destination){

        destination.setBillingAddress(source.getBillingAddress());
        destination.setCity(source.getCity());
        destination.setState(source.getState());
        destination.setCountry(source.getCountry());
        destination.setBillingPincode(source.getBillingPincode());
//        destination.setBillingPhone(source.getBillingPhone());

    }

}
