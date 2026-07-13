package com.bdd.portal.controller;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ExecutionRepository executionRepository;

    @GetMapping
    public String listReports(Model model) {
        List<Execution> all = executionRepository.findAll();
        model.addAttribute("executions", all);
        return "reports/list";
    }
}
