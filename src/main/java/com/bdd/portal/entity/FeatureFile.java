package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feature_file")
@Getter
@Setter
public class FeatureFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String relativePath;

    @Column(unique = true)
    private String slug;

    private String moduleSlug;

    private String folder;

    @Column(length = 1000)
    private String description;

    private String tags;

    private int scenarioCount;

    private int stepCount;

    private LocalDateTime lastModified;

    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "feature_scenarios", joinColumns = @JoinColumn(name = "feature_file_id"))
    private List<FeatureScenario> scenarios = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    public static class FeatureScenario {
        private String name;
        private int lineNumber;
        private String slug;

        public FeatureScenario() {}

        public FeatureScenario(String name, int lineNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
        }
    }
}
