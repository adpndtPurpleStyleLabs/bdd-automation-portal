package com.bdd.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BddAutomationPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(BddAutomationPortalApplication.class, args);
	}

}
