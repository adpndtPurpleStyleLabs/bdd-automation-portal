package com.bdd.portal.controller;

import com.bdd.portal.entity.TestEnvironment;
import com.bdd.portal.service.TestEnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/config/environments")
@RequiredArgsConstructor
public class EnvironmentConfigController {

    private final TestEnvironmentService testEnvironmentService;

    @GetMapping
    public String listEnvironments(Model model) {
        model.addAttribute("environments", testEnvironmentService.getAllEnvironments());
        model.addAttribute("newEnvironment", new TestEnvironment());
        return "config/environments";
    }

    @PostMapping("/add")
    public String addEnvironment(@ModelAttribute TestEnvironment environment, RedirectAttributes redirectAttributes) {
        testEnvironmentService.saveEnvironment(environment);
        redirectAttributes.addFlashAttribute("successMessage", "Environment added successfully!");
        return "redirect:/config/environments";
    }

    @PostMapping("/update")
    public String updateEnvironment(@ModelAttribute TestEnvironment environment, RedirectAttributes redirectAttributes) {
        testEnvironmentService.saveEnvironment(environment);
        redirectAttributes.addFlashAttribute("successMessage", "Environment updated successfully!");
        return "redirect:/config/environments";
    }

    @PostMapping("/delete/{id}")
    public String deleteEnvironment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        testEnvironmentService.deleteEnvironment(id);
        redirectAttributes.addFlashAttribute("successMessage", "Environment deleted successfully!");
        return "redirect:/config/environments";
    }
}
