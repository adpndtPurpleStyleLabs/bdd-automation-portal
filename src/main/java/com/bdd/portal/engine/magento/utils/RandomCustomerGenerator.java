package com.bdd.portal.engine.magento.utils;

import java.util.Random;

public class RandomCustomerGenerator {

    private static final Random random = new Random();

    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditya", "Rahul", "Rohan",
            "Priya", "Ananya", "Sneha"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Verma", "Patel", "Singh",
            "Gupta", "Yadav"
    };

    private static final Address[] INDIAN_ADDRESSES = {
            new Address("India", "Maharashtra", "Mumbai", "400001"),
            new Address("India", "Karnataka", "Bengaluru", "560001"),
            new Address("India", "Delhi", "New Delhi", "110001"),
            new Address("India", "Uttar Pradesh", "Lucknow", "226001"),
            new Address("India", "Tamil Nadu", "Chennai", "600001")
    };

    private static final Address[] INTERNATIONAL_ADDRESSES = {
            new Address("United States", "California", "Los Angeles", "90001"),
            new Address("United Kingdom", "England", "London", "SW1A1AA"),
            new Address("Australia", "New South Wales", "Sydney", "2000"),
            new Address("Canada", "Ontario", "Toronto", "M5H2N2"),
            new Address("Singapore", "Singapore", "Singapore", "018989")
    };

    public static CustomerData generateIndianCustomer() {
        return generateCustomer(INDIAN_ADDRESSES, true);
    }

    public static CustomerData generateInternationalCustomer() {
        return generateCustomer(INTERNATIONAL_ADDRESSES, false);
    }

    private static CustomerData generateCustomer(Address[] addresses, boolean indianPhone) {

        Address address = addresses[random.nextInt(addresses.length)];

        CustomerData customer = new CustomerData();

        customer.setEmail(randomEmail());
        customer.setFirstName(randomFirstName());
        customer.setLastName(randomLastName());
        customer.setPhone(indianPhone ? randomIndianPhone() : randomPhone());

        customer.setCountry(address.getCountry());
        customer.setState(address.getState());
        customer.setCity(address.getCity());
        customer.setBillingPincode(address.getPincode());

        customer.setBillingAddress(random.nextInt(999) + " Test Street");

        return customer;
    }

    private static String randomEmail() {
        return "psltest" + System.currentTimeMillis() + "@gmail.com";
    }

    private static String randomFirstName() {
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
    }

    private static String randomLastName() {
        return LAST_NAMES[random.nextInt(LAST_NAMES.length)];
    }

    private static String randomIndianPhone() {
        return "9" + String.format("%09d", random.nextInt(1_000_000_000));
    }

    private static String randomPhone() {
        return String.valueOf(1000000000L + random.nextInt(900000000));
    }
}