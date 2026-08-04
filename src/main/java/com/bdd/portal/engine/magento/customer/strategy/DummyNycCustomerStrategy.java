package com.bdd.portal.engine.magento.customer.strategy;

import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.TestDataReader;

public class DummyNycCustomerStrategy implements CustomerStrategy{

    @Override
    public CustomerData createCustomer() throws Exception{

        CustomerData customer =
                TestDataReader.getCustomerByType("DummyCustomer-NYC");

        customer.setBillingPhone("1990998765");

        customer.setType("DummyCustomer-NYC");

        return customer;
    }
}
