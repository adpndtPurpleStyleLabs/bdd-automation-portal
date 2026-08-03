package com.bdd.portal.engine.magento.customer.strategy;

import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.TestDataReader;

public class DummyLondonCustomerStrategy implements CustomerStrategy{

    @Override
    public CustomerData createCustomer() throws Exception{

        CustomerData customer =
                TestDataReader.getCustomerByType("DummyCustomer-London");

        customer.setBillingPhone("1990998765");

        customer.setType("DummyCustomer-London");

        return customer;
    }
}
