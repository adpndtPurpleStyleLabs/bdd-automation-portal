package com.bdd.portal.service;

import com.bdd.portal.dto.TestDataFileInfo;
import com.bdd.portal.engine.magento.utils.CustomerData;
import com.bdd.portal.engine.magento.utils.ItemData;
import com.bdd.portal.engine.magento.utils.PaymentData;
import com.bdd.portal.engine.magento.utils.TestDataReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TestDataServiceTest {

    @Autowired
    private TestDataService testDataService;

    @Test
    void testListTestDataFiles() {
        List<TestDataFileInfo> files = testDataService.listTestDataFiles();
        assertNotNull(files);
        assertFalse(files.isEmpty());

        boolean hasCustomerData = files.stream().anyMatch(f -> "customerData.json".equals(f.getFilename()));
        boolean hasItemData = files.stream().anyMatch(f -> "itemData.json".equals(f.getFilename()));
        boolean hasUserInfo = files.stream().anyMatch(f -> "userInfo.json".equals(f.getFilename()));
        boolean hasPaymentType = files.stream().anyMatch(f -> "paymentType.json".equals(f.getFilename()));

        assertTrue(hasCustomerData, "Should list customerData.json");
        assertTrue(hasItemData, "Should list itemData.json");
        assertTrue(hasUserInfo, "Should list userInfo.json");
        assertTrue(hasPaymentType, "Should list paymentType.json");
    }

    @Test
    void testGetFileContent() throws Exception {
        String content = testDataService.getFileContent("customerData.json");
        assertNotNull(content);
        assertTrue(content.contains("customers"), "customerData.json should contain 'customers' key");

        String userContent = testDataService.getFileContent("userInfo.json");
        assertNotNull(userContent);
        assertTrue(userContent.contains("userId"), "userInfo.json should contain 'userId' field");
    }

    @Test
    void testInvalidJsonThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            testDataService.saveFileContent("customerData.json", "{ invalid json text }");
        });
    }

    @Test
    void testCreateAndReadAndCustomFile() throws Exception {
        String customFile = "tempTestFile.json";
        String sampleJson = "{\n  \"status\": \"ok\",\n  \"count\": 10\n}";

        testDataService.createNewFile(customFile, sampleJson);

        String readContent = testDataService.getFileContent(customFile);
        assertNotNull(readContent);
        assertTrue(readContent.contains("status"));

        // Cleanup
        testDataService.deleteFile(customFile);
    }

    @Test
    void testTestDataReaderIntegration() throws Exception {
        List<CustomerData> customers = TestDataReader.getAllCustomers();
        assertNotNull(customers);
        assertFalse(customers.isEmpty());

        List<ItemData> items = TestDataReader.getAllItem();
        assertNotNull(items);
        assertFalse(items.isEmpty());

        PaymentData cashPayment = TestDataReader.getPaymentData("Cash");
        assertNotNull(cashPayment);
        assertNotNull(cashPayment.getTransactionId());
    }
}
