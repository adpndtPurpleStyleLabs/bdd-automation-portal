package com.bdd.portal.engine.magento.customer.address;

import com.bdd.portal.engine.magento.customer.util.CustomerUtil;
import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.TestDataReader;

public class StoreAddressStrategy
        implements AddressStrategy {

    @Override
    public void apply(CustomerData customer) throws Exception {

        CustomerData store =
                TestDataReader.getCustomerByType("DummyCustomer");

        CustomerUtil.copyAddress(store, customer);

    }

}
