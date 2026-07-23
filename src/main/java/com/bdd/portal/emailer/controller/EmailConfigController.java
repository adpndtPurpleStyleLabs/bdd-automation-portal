package com.bdd.portal.emailer.controller;

import com.bdd.portal.emailer.entity.EmailConfiguration;
import com.bdd.portal.emailer.entity.EmailQueue;
import com.bdd.portal.emailer.repository.EmailConfigurationRepository;
import com.bdd.portal.emailer.repository.EmailQueueRepository;
import com.bdd.portal.emailer.service.EmailService;
import com.bdd.portal.emailer.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings/email")
@RequiredArgsConstructor
@Slf4j
public class EmailConfigController {

    private final EmailConfigurationRepository configurationRepository;
    private final EmailQueueRepository emailQueueRepository;
    private final EmailService emailService;

    @GetMapping
    public String viewConfig(Model model) {
        EmailConfiguration config = configurationRepository.findTopByOrderByIdDesc()
                .orElse(new EmailConfiguration());
        if (config.getId() != null && config.getPassword() != null && !config.getPassword().isEmpty()) {
            config.setPassword("********");
        }
        model.addAttribute("emailConfig", config);
        return "settings/email-config";
    }

    @PostMapping
    public String saveConfig(@ModelAttribute EmailConfiguration config, RedirectAttributes redirectAttributes) {
        // If password is not modified (i.e. starts with some placeholder or is empty), load old password
        if (config.getPassword() == null || config.getPassword().isEmpty() || config.getPassword().equals("********")) {
            if (config.getId() != null) {
                EmailConfiguration existing = configurationRepository.findById(config.getId()).orElse(null);
                if (existing != null) {
                    config.setPassword(existing.getPassword()); // Keep encrypted
                }
            }
        } else {
            // Encrypt new password
            config.setPassword(EncryptionUtil.encrypt(config.getPassword()));
        }
        
        configurationRepository.save(config);
        redirectAttributes.addFlashAttribute("successMessage", "Email configuration saved successfully.");
        return "redirect:/settings/email";
    }

    @PostMapping("/test")
    public String testConfig(@ModelAttribute EmailConfiguration config, RedirectAttributes redirectAttributes) {
        try {
            emailService.testConnection(config);
            redirectAttributes.addFlashAttribute("successMessage", "Test Connection Successful!");
        } catch (Exception e) {
            log.error("SMTP Test failed", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Test Connection Failed: " + e.getMessage());
        }
        return "redirect:/settings/email";
    }

    @GetMapping("/history")
    public String emailHistory(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "15") int size,
                               Model model) {
        Page<EmailQueue> historyPage = emailQueueRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        model.addAttribute("historyPage", historyPage);
        return "admin/email-history";
    }
}
