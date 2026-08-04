package com.bdd.portal.engine.magento.customer.strategy;

import com.bdd.portal.engine.magento.customer.util.CustomerUtil;
import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.RandomCustomerGenerator;

public class RandomLondonCustomerStrategy implements CustomerStrategy{

    @Override
    public CustomerData createCustomer() {

        CustomerData customer =
                RandomCustomerGenerator.generateInternationalCustomer();

        CustomerUtil.initializeBilling(customer);

        return customer;
    }
}
