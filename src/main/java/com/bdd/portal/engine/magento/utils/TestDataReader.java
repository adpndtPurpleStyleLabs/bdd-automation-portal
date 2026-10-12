package com.bdd.portal.engine.magento.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class TestDataReader {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static InputStream getTestDataInputStream(String relativePath) throws Exception {
        // 1. Check primary source directory
        File srcFile = new File("src/main/resources/" + relativePath);
        if (srcFile.exists() && srcFile.isFile()) {
            return new FileInputStream(srcFile);
        }

        // 2. Check target/classes build directory
        File targetFile = new File("target/classes/" + relativePath);
        if (targetFile.exists() && targetFile.isFile()) {
            return new FileInputStream(targetFile);
        }

        // 3. Fallback to classpath
        InputStream inputStream = TestDataReader.class
                .getClassLoader()
                .getResourceAsStream(relativePath);

        if (inputStream != null) {
            return inputStream;
        }

        throw new RuntimeException("Test data file not found: " + relativePath);
    }

    public static CustomerData getCustomerData(String key) throws Exception {
        try (InputStream inputStream = getTestDataInputStream("testData/customerData.json")) {
            JsonNode root = mapper.readTree(inputStream);
            return mapper.treeToValue(root.get(key), CustomerData.class);
        }
    }

    public static List<CustomerData> getAllCustomers() throws Exception {
        try (InputStream inputStream = getTestDataInputStream("testData/customerData.json")) {
            JsonNode root = mapper.readTree(inputStream);
            return mapper.convertValue(
                    root.get("customers"),
                    new TypeReference<List<CustomerData>>() {}
            );
        }
    }

    public static CustomerData getCustomerByType(String type) throws Exception {
        return getAllCustomers()
                .stream()
                .filter(customer -> customer.getType().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Customer type not found: " + type));
    }

    public static UserInfo getUserByStore(String store) throws Exception{
        try(InputStream inputStream = getTestDataInputStream("testData/userInfo.json")) {
            JsonNode root = mapper.readTree((inputStream));
            JsonNode storeNode = root.get(store);
            if (storeNode == null) {
                throw new RuntimeException(
                        "No user information found for store: " + store
                );
            }
            return mapper.convertValue(
                    storeNode,
                    new TypeReference<UserInfo>() {}
            );
        }
    }

    public static List<ItemData> getAllItem() throws Exception {
        try (InputStream inputStream = getTestDataInputStream("testData/itemData.json")) {
            JsonNode root = mapper.readTree(inputStream);
            return mapper.convertValue(
                    root.get("ItemData"),
                    new TypeReference<List<ItemData>>() {}
            );
        }
    }

    public static PaymentData getPaymentData(String key) throws Exception {
        try (InputStream inputStream = getTestDataInputStream("testData/paymentType.json")) {
            JsonNode root = mapper.readTree(inputStream);
            return mapper.treeToValue(root.get(key), PaymentData.class);
        }
    }

    public static CurrencyData getCurrencyData(String currency) throws Exception {
        try (InputStream inputStream = getTestDataInputStream("testData/currencyConversionRate.json")) {
            JsonNode root = mapper.readTree(inputStream);
            return mapper.treeToValue(
                    root.get(currency.toLowerCase()),
                    CurrencyData.class);
        }
    }

    public static CurrencyData getCurrencyCalculator(String currency) throws Exception {
        try (InputStream inputStream = getTestDataInputStream("testData/currencyCalculator.json")) {
            JsonNode root = mapper.readTree(inputStream);
            return mapper.treeToValue(
                    root.get(currency.toLowerCase()),
                    CurrencyData.class);
        }
    }
}
