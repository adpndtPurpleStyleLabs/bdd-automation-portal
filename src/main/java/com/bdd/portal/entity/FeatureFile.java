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

    private Integer currentVersion = 1;
    
    // We will establish a OneToMany relationship to FeatureVersion if needed, 
    // but usually querying versions by featureFile ID is sufficient.
    @OneToMany(mappedBy = "featureFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeatureVersion> versions = new ArrayList<>();
}
