package com.bdd.portal.engine.magento.customer.address;


import com.bdd.portal.engine.magento.customer.util.CustomerUtil;
import  com.bdd.portal.engine.magento.utils.CustomerData;
import  com.bdd.portal.engine.magento.utils.RandomCustomerGenerator;

public class NewInternationalAddressStrategy implements AddressStrategy {

    @Override
    public void apply(CustomerData customer) {

        CustomerData random =
                RandomCustomerGenerator.generateInternationalCustomer();

        CustomerUtil.copyAddress(random, customer);

    }


}
