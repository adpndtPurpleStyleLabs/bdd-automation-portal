package com.bdd.portal.engine.magento.customer.service;

import com.bdd.portal.engine.magento.customer.factory.AddressFactory;
import com.bdd.portal.engine.magento.customer.factory.CustomerFactory;
import com.bdd.portal.engine.magento.utils.CustomerData;

public class CustomerService {

    public CustomerData createCustomer(
            String customerType,
            String addressType) throws Exception {

        CustomerData customer =
                CustomerFactory
                        .getStrategy(customerType)
                        .createCustomer();

        AddressFactory
                .getStrategy(addressType)
                .apply(customer);

        customer.setAddresstype(addressType);

        if (("random-nyc".equals(customerType)) && ("store-nyc".equals(addressType))){
            customer.setBillingPhone("9871643707");
        }

        if (("random-london".equals(customerType)) && ("store-london".equals(addressType))) {
          customer.setBillingPhone("7847848484");
        }

        return customer;
    }

}
