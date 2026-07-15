package com.bdd.portal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path reportPath = Paths.get("bdd-reports").toAbsolutePath();
        registry.addResourceHandler("/reports/**")
                .addResourceLocations("file:" + reportPath.toString() + "/");
    }
}
