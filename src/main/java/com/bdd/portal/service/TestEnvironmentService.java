package com.bdd.portal.service;

import com.bdd.portal.entity.TestEnvironment;
import com.bdd.portal.repository.TestEnvironmentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestEnvironmentService {

    private final TestEnvironmentRepository repository;

    @PostConstruct
    public void initDefaultEnvironments() {
        if (repository.count() == 0) {
            log.info("No test environments found in database. Initializing default environments...");
            addEnvironment("MAGE1", "https://mage1.ppustage.dev/uspp-admin");
            addEnvironment("MAGE2", "https://mage2.ppustage.dev/uspp-admin");
            addEnvironment("MAGE3", "https://mage3.ppustage.dev/uspp-admin");
            addEnvironment("STAGE", "https://magento.ppustage.dev/uspp-admin");
        }
    }

    private void addEnvironment(String name, String url) {
        TestEnvironment env = new TestEnvironment();
        env.setName(name);
        env.setUrl(url);
        repository.save(env);
    }

    public List<TestEnvironment> getAllEnvironments() {
        return repository.findAll();
    }

    public List<String> getAllEnvironmentNames() {
        return repository.findAll().stream()
                .map(TestEnvironment::getName)
                .collect(Collectors.toList());
    }

    public String getUrlByName(String name) {
        return repository.findByName(name)
                .map(TestEnvironment::getUrl)
                .orElse("https://magento.ppustage.dev/uspp-admin"); // Fallback
    }

    public TestEnvironment saveEnvironment(TestEnvironment environment) {
        return repository.save(environment);
    }

    public void deleteEnvironment(Long id) {
        repository.deleteById(id);
    }
}
