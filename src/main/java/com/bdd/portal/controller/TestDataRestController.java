package com.bdd.portal.controller;

import com.bdd.portal.dto.TestDataFileInfo;
import com.bdd.portal.service.TestDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-data")
@RequiredArgsConstructor
@Slf4j
public class TestDataRestController {

    private final TestDataService testDataService;

    @GetMapping("/files")
    public ResponseEntity<List<TestDataFileInfo>> listFiles() {
        return ResponseEntity.ok(testDataService.listTestDataFiles());
    }

    @GetMapping("/file/{filename}")
    public ResponseEntity<?> getFileContent(@PathVariable String filename) {
        try {
            String content = testDataService.getFileContent(filename);

            Map<String, Object> response = new HashMap<>();
            response.put("filename", filename);
            response.put("content", content);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error reading file {}", filename, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }

    @PostMapping("/file/{filename}")
    public ResponseEntity<?> saveFileContent(@PathVariable String filename, @RequestBody Map<String, String> payload) {
        try {
            String content = payload.get("content");
            if (content == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Content payload is required"));
            }

            testDataService.saveFileContent(filename, content);
            return ResponseEntity.ok(Map.of("message", "File saved successfully", "filename", filename));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error saving file {}", filename, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save file: " + e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createFile(@RequestBody Map<String, String> payload) {
        try {
            String filename = payload.get("filename");
            String content = payload.getOrDefault("content", "{\n\n}");

            if (filename == null || filename.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Filename is required"));
            }

            testDataService.createNewFile(filename.trim(), content);
            return ResponseEntity.ok(Map.of("message", "File created successfully", "filename", filename));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating file", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create file: " + e.getMessage()));
        }
    }

    @DeleteMapping("/file/{filename}")
    public ResponseEntity<?> deleteFile(@PathVariable String filename) {
        try {
            testDataService.deleteFile(filename);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting file {}", filename, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "filename", required = false) String customName) {
        try {
            String filename = (customName != null && !customName.trim().isEmpty()) ? customName.trim() : file.getOriginalFilename();
            if (filename == null || filename.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file name"));
            }

            if (!filename.endsWith(".json")) {
                filename = filename + ".json";
            }

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            testDataService.saveFileContent(filename, content);

            return ResponseEntity.ok(Map.of("message", "File uploaded successfully", "filename", filename));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            String content = testDataService.getFileContent(filename);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            log.error("Error downloading file {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }
}
