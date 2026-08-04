package com.bdd.portal.emailer.repository;

import com.bdd.portal.emailer.entity.EmailConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailConfigurationRepository extends JpaRepository<EmailConfiguration, Long> {
    Optional<EmailConfiguration> findTopByOrderByIdDesc();
}
