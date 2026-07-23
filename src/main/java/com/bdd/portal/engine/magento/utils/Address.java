package com.bdd.portal.engine.magento.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Address {
    private final String country;
    private final String state;
    private final String city;
    private final String pincode;

    public Address(String country, String state, String city, String pincode) {
        this.country = country;
        this.state = state;
        this.city = city;
        this.pincode = pincode;
    }

    public String getCountry() {
        return country;
    }

    public String getState() {
        return state;
    }

    public String getCity() {
        return city;
    }

    public String getPincode() {
        return pincode;
    }
}
