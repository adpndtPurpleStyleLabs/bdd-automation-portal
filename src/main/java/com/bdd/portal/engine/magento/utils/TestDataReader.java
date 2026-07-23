package com.bdd.portal.engine.magento.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;

public class TestDataReader {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static CustomerData getCustomerData(String key) throws Exception {

        InputStream inputStream = TestDataReader.class
                .getClassLoader()
                .getResourceAsStream("testData/customerData.json");

        if (inputStream == null) {
            throw new RuntimeException("customerData.json not found in resources/testData");
        }

        JsonNode root = mapper.readTree(inputStream);

        return mapper.treeToValue(root.get(key), CustomerData.class);
    }

    public static List<CustomerData> getAllCustomers() throws Exception {

        InputStream inputStream = TestDataReader.class
                .getClassLoader()
                .getResourceAsStream("testData/customerData.json");

        if (inputStream == null) {
            throw new RuntimeException("customerData.json not found");
        }

        JsonNode root = mapper.readTree(inputStream);

        return mapper.convertValue(
                root.get("customers"),
                new TypeReference<List<CustomerData>>() {}
        );
    }

    public static CustomerData getCustomerByType(String type) throws Exception {

        return getAllCustomers()
                .stream()
                .filter(customer -> customer.getType().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Customer type not found: " + type));
    }

    public static List<ItemData> getAllItem() throws Exception {

        InputStream inputStream = TestDataReader.class
                .getClassLoader()
                .getResourceAsStream("testData/itemData.json");

        if (inputStream == null) {
            throw new RuntimeException("itemData.json not found");
        }

        JsonNode root = mapper.readTree(inputStream);

        return mapper.convertValue(
                root.get("ItemData"),
                new TypeReference<List<ItemData>>() {}
        );
    }

    public static PaymentData getPaymentData(String key) throws Exception {

        InputStream inputStream = TestDataReader.class
                .getClassLoader()
                .getResourceAsStream("testData/paymentType.json");

        if (inputStream == null) {
            throw new RuntimeException("paymentData.json not found");
        }

        JsonNode root = mapper.readTree(inputStream);

        return mapper.treeToValue(root.get(key), PaymentData.class);
    }

    public static CurrencyData getCurrencyData(String currency) throws Exception {

        InputStream inputStream = TestDataReader.class
                .getClassLoader()
                .getResourceAsStream("testData/currencyConversionRate.json");

        JsonNode root = mapper.readTree(inputStream);

        return mapper.treeToValue(
                root.get(currency.toLowerCase()),
                CurrencyData.class);
    }
}
