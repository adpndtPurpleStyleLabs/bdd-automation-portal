package com.bdd.portal.engine.magento.customer.strategy;

import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.TestDataReader;

public class ExistingCustomerStrategy implements CustomerStrategy {

    @Override
    public CustomerData createCustomer() throws Exception{
        return TestDataReader.getCustomerByType("ExistingCustomer");
    }
}
