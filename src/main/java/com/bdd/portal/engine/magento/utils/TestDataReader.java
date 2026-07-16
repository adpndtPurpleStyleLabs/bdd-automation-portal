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
}
