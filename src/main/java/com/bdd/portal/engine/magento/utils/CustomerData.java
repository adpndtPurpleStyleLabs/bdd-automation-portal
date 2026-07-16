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

    public String getEmail() {return email;}

    public String getType() {return type;}

    public void setType(String type) {
        this.type = type;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getExpectedPhone() {
        return expectedPhone;
    }

    public void setExpectedPhone(String expectedPhone) {
        this.expectedPhone = expectedPhone;
    }

    public String getExpectedFirstName() {
        return expectedFirstName;
    }

    public void setExpectedFirstName(String expectedFirstName) {
        this.expectedFirstName = expectedFirstName;
    }

    public String getExpectedLastName() {
        return expectedLastName;
    }

    public void setExpectedLastName(String expectedLastName) {
        this.expectedLastName = expectedLastName;
    }

    public String getBillingName() {
        return billingName;
    }

    public void setBillingName(String billingName) {
        this.billingName = billingName;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getBillingPhone() {
        return billingPhone;
    }

    public void setBillingPhone(String billingPhone) {
        this.billingPhone = billingPhone;
    }

    public String getBillingPincode() {
        return billingPincode;
    }

    public void setBillingPincode(String billingPincode) {
        this.billingPincode = billingPincode;
    }
}
