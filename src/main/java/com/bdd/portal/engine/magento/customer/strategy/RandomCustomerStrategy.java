package com.bdd.portal.engine.magento.customer.strategy;

import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.RandomCustomerGenerator;
import com.bdd.portal.engine.magento.customer.util.CustomerUtil;

public class RandomCustomerStrategy implements CustomerStrategy {

    @Override
    public CustomerData createCustomer() {

        CustomerData customer =
                RandomCustomerGenerator.generateIndianCustomer();

        CustomerUtil.initializeBilling(customer);

        return customer;
    }
}
