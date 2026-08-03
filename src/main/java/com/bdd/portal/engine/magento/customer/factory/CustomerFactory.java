package com.bdd.portal.engine.magento.customer.factory;

import com.bdd.portal.engine.magento.customer.strategy.*;

public class CustomerFactory {

    public static CustomerStrategy getStrategy(
            String customerType){

        return switch(customerType.toLowerCase()){

            case "existing" ->
                    new ExistingCustomerStrategy();

            case "random" ->
                    new RandomCustomerStrategy();

            case "dummy" ->
                    new DummyCustomerStrategy();

            case "random-nyc" ->
                new RandomNYCCustomerStrategy();

            case "dummy-nyc" ->
                new DummyNycCustomerStrategy();

            case "dummy-london" ->
                new DummyLondonCustomerStrategy();

            case "random-london" ->
                new RandomLondonCustomerStrategy();

            default ->
                    throw new RuntimeException();

        };

    }

}
