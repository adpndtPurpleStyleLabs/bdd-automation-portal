package com.bdd.portal.controller;

import com.bdd.portal.dto.TestDataFileInfo;
import com.bdd.portal.service.TestDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TestDataController {

    private final TestDataService testDataService;

    @GetMapping({"/test-data", "/config/test-data"})
    public String viewTestDataPage(@RequestParam(value = "file", required = false) String selectedFile, Model model) {
        List<TestDataFileInfo> files = testDataService.listTestDataFiles();

        String activeFilename = selectedFile;
        if (activeFilename == null || activeFilename.trim().isEmpty()) {
            if (!files.isEmpty()) {
                activeFilename = files.get(0).getFilename();
            } else {
                activeFilename = "customerData.json";
            }
        }

        String initialContent = "";
        try {
            initialContent = testDataService.getFileContent(activeFilename);
        } catch (Exception e) {
            log.warn("Could not load initial content for {}", activeFilename, e);
        }

        model.addAttribute("files", files);
        model.addAttribute("activeFile", activeFilename);
        model.addAttribute("initialContent", initialContent);

        return "testdata/index";
    }
}
