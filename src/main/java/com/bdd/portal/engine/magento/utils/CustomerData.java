package com.bdd.portal.engine.magento.utils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerData {

    private String email;
    private String expectedPhone;
    private String expectedFirstName;
    private String expectedLastName;

    private String billingName;
    private String billingAddress;
    private String billingPhone;
    private String billingPincode;

    private String type;

    private String firstName;
    private String lastName;
    private String phone;

    private String country;
    private String state;
    private String city;
}
