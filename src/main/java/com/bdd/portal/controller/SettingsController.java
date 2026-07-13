package com.bdd.portal.controller;

import com.bdd.portal.entity.Settings;
import com.bdd.portal.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsRepository settingsRepository;

    @GetMapping
    public String showSettings(Model model) {
        Settings settings = settingsRepository.findById(1L).orElse(new Settings());
        model.addAttribute("settings", settings);
        return "settings";
    }

    @PostMapping
    public String saveSettings(@ModelAttribute Settings settings) {
        settings.setId(1L); // Ensure single instance
        settingsRepository.save(settings);
        return "redirect:/settings?success";
    }
}
