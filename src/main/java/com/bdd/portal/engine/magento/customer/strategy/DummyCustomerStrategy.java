package com.bdd.portal.engine.magento.customer.strategy;

import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.TestDataReader;

public class DummyCustomerStrategy implements CustomerStrategy {

    @Override
    public CustomerData createCustomer() throws Exception{

        CustomerData customer =
                TestDataReader.getCustomerByType("DummyCustomer");
        customer.setBillingPhone("1990998765");

        customer.setType("DummyCustomer");

        return customer;
    }
}
