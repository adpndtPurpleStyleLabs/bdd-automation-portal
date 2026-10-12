package com.bdd.portal.service;

import com.bdd.portal.dto.TestDataFileInfo;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class TestDataService {

    private final ObjectMapper mapper;
    private final String testDataPath;

    public TestDataService(@Value("${bdd.portal.test-data-path:src/main/resources/testData}") String testDataPath) {
        this.testDataPath = testDataPath;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path getPrimaryTestDataDir() {
        Path path = Paths.get(testDataPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                log.error("Failed to create testData directory at {}", path, e);
            }
        }
        return path;
    }

    public Path getTargetTestDataDir() {
        return Paths.get("target/classes/testData");
    }

    public List<TestDataFileInfo> listTestDataFiles() {
        List<TestDataFileInfo> result = new ArrayList<>();
        Path primaryDir = getPrimaryTestDataDir();

        Set<String> coreFiles = Set.of(
                "customerData.json",
                "itemData.json",
                "userInfo.json",
                "paymentType.json",
                "currencyConversionRate.json",
                "currencyCalculator.json"
        );

        // Core files order for clean display
        List<String> coreOrder = List.of(
                "userInfo.json",
                "customerData.json",
                "itemData.json",
                "paymentType.json",
                "currencyConversionRate.json",
                "currencyCalculator.json"
        );

        Map<String, TestDataFileInfo> fileMap = new HashMap<>();

        try {
            if (Files.exists(primaryDir)) {
                try (var stream = Files.list(primaryDir)) {
                    stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                        String filename = p.getFileName().toString();
                        fileMap.put(filename, buildFileInfo(p, filename, coreFiles.contains(filename)));
                    });
                }
            }
        } catch (IOException e) {
            log.error("Error reading test data directory {}", primaryDir, e);
        }

        // Add core files in preferred order
        for (String coreName : coreOrder) {
            if (fileMap.containsKey(coreName)) {
                result.add(fileMap.remove(coreName));
            }
        }

        // Add any remaining custom JSON files alphabetically
        fileMap.values().stream()
                .sorted(Comparator.comparing(TestDataFileInfo::getFilename))
                .forEach(result::add);

        return result;
    }

    private TestDataFileInfo buildFileInfo(Path path, String filename, boolean isCore) {
        long size = 0;
        String lastModified = "N/A";
        int recordCount = 0;
        String recordLabel = "Records";

        try {
            size = Files.size(path);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            lastModified = sdf.format(new Date(Files.getLastModifiedTime(path).toMillis()));

            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length > 0) {
                JsonNode root = mapper.readTree(bytes);
                if ("customerData.json".equalsIgnoreCase(filename)) {
                    if (root.has("customers") && root.get("customers").isArray()) {
                        recordCount = root.get("customers").size();
                        recordLabel = "Customers";
                    }
                } else if ("itemData.json".equalsIgnoreCase(filename)) {
                    if (root.has("ItemData") && root.get("ItemData").isArray()) {
                        recordCount = root.get("ItemData").size();
                        recordLabel = "Items";
                    }
                } else if ("userInfo.json".equalsIgnoreCase(filename)) {
                    recordCount = root.size();
                    recordLabel = "Store Users";
                } else if ("paymentType.json".equalsIgnoreCase(filename)) {
                    recordCount = root.size();
                    recordLabel = "Payment Methods";
                } else if (filename.toLowerCase().contains("currency")) {
                    recordCount = root.size();
                    recordLabel = "Currencies";
                } else if (root.isArray()) {
                    recordCount = root.size();
                    recordLabel = "Items";
                } else if (root.isObject()) {
                    recordCount = root.size();
                    recordLabel = "Keys";
                }
            }
        } catch (Exception e) {
            log.warn("Could not calculate metadata for {}", filename, e);
        }

        String displayName = getDisplayNameForFile(filename);

        return TestDataFileInfo.builder()
                .filename(filename)
                .displayName(displayName)
                .sizeBytes(size)
                .lastModified(lastModified)
                .recordCount(recordCount)
                .recordLabel(recordLabel)
                .coreFile(isCore)
                .build();
    }

    private String getDisplayNameForFile(String filename) {
        return switch (filename) {
            case "customerData.json" -> "Customer Profiles";
            case "itemData.json" -> "Products & Items";
            case "userInfo.json" -> "User Credentials";
            case "paymentType.json" -> "Payment Methods";
            case "currencyConversionRate.json" -> "Currency Conversion Rates";
            case "currencyCalculator.json" -> "Currency Calculator Rates";
            default -> {
                String name = filename.replace(".json", "");
                yield name.substring(0, 1).toUpperCase() + name.substring(1);
            }
        };
    }

    public String getFileContent(String filename) throws IOException {
        Path filePath = resolveFilePath(filename);
        if (!Files.exists(filePath)) {
            // Check fallback from classpath
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("testData/" + filename)) {
                if (is != null) {
                    JsonNode node = mapper.readTree(is);
                    return formatJson(node);
                }
            }
            throw new IllegalArgumentException("File not found: " + filename);
        }

        byte[] bytes = Files.readAllBytes(filePath);
        JsonNode node = mapper.readTree(bytes);
        return formatJson(node);
    }

    public JsonNode getFileJsonNode(String filename) throws IOException {
        Path filePath = resolveFilePath(filename);
        if (!Files.exists(filePath)) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("testData/" + filename)) {
                if (is != null) {
                    return mapper.readTree(is);
                }
            }
            throw new IllegalArgumentException("File not found: " + filename);
        }
        byte[] bytes = Files.readAllBytes(filePath);
        return mapper.readTree(bytes);
    }

    public void saveFileContent(String filename, String jsonContent) throws IOException {
        validateFilename(filename);

        // Validate JSON
        JsonNode parsedNode;
        try {
            parsedNode = mapper.readTree(jsonContent);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON syntax: " + e.getMessage(), e);
        }

        String formattedJson = formatJson(parsedNode);
        byte[] bytes = formattedJson.getBytes(StandardCharsets.UTF_8);

        // 1. Save to primary src path
        Path primaryPath = resolveFilePath(filename);
        if (primaryPath.getParent() != null && !Files.exists(primaryPath.getParent())) {
            Files.createDirectories(primaryPath.getParent());
        }
        Files.write(primaryPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("Saved test data to {}", primaryPath.toAbsolutePath());

        // 2. Also save to target/classes if directory exists
        Path targetDir = getTargetTestDataDir();
        if (Files.exists(targetDir.getParent())) {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            Path targetPath = targetDir.resolve(filename);
            Files.write(targetPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Synced test data to {}", targetPath.toAbsolutePath());
        }
    }

    public void createNewFile(String filename, String initialContent) throws IOException {
        if (!filename.endsWith(".json")) {
            filename = filename + ".json";
        }
        validateFilename(filename);

        Path primaryPath = resolveFilePath(filename);
        if (Files.exists(primaryPath)) {
            throw new IllegalArgumentException("File already exists: " + filename);
        }

        if (initialContent == null || initialContent.trim().isEmpty()) {
            initialContent = "{\n\n}";
        }

        saveFileContent(filename, initialContent);
    }

    public void deleteFile(String filename) throws IOException {
        validateFilename(filename);

        Set<String> coreFiles = Set.of(
                "customerData.json",
                "itemData.json",
                "userInfo.json",
                "paymentType.json",
                "currencyConversionRate.json",
                "currencyCalculator.json"
        );

        if (coreFiles.contains(filename)) {
            throw new IllegalArgumentException("Core test data file cannot be deleted: " + filename);
        }

        Path primaryPath = resolveFilePath(filename);
        if (Files.exists(primaryPath)) {
            Files.delete(primaryPath);
            log.info("Deleted test data file from {}", primaryPath);
        }

        Path targetPath = getTargetTestDataDir().resolve(filename);
        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
            log.info("Deleted target test data file from {}", targetPath);
        }
    }

    private Path resolveFilePath(String filename) {
        validateFilename(filename);
        return getPrimaryTestDataDir().resolve(filename);
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename characters: " + filename);
        }
        if (!filename.endsWith(".json")) {
            throw new IllegalArgumentException("Filename must end with .json");
        }
    }

    private String formatJson(JsonNode node) throws IOException {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", "\n");
        prettyPrinter.indentObjectsWith(indenter);
        prettyPrinter.indentArraysWith(indenter);
        return mapper.writer(prettyPrinter).writeValueAsString(node);
    }
}
