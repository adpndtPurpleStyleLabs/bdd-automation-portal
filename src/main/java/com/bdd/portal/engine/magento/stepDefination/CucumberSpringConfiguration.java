package com.bdd.portal.engine.magento.stepDefination;

import com.bdd.portal.BddAutomationPortalApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Configuration;

@CucumberContextConfiguration
@ContextConfiguration(classes = CucumberSpringConfiguration.TestConfig.class)
public class CucumberSpringConfiguration {

    @Configuration
    public static class TestConfig {
    }
}
