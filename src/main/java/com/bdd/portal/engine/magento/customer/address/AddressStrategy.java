package com.bdd.portal.engine.magento.customer.address;

import com.bdd.portal.engine.magento.utils.CustomerData;

public interface AddressStrategy {

    void apply(CustomerData customer) throws Exception;

}
