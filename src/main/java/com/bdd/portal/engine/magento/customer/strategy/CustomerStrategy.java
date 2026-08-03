package com.bdd.portal.engine.magento.customer.strategy;

import com.bdd.portal.engine.magento.utils.CustomerData;

public interface CustomerStrategy {

    CustomerData createCustomer() throws Exception;
}
