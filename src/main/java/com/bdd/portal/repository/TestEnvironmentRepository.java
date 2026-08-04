package com.bdd.portal.repository;

import com.bdd.portal.entity.TestEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestEnvironmentRepository extends JpaRepository<TestEnvironment, Long> {
    Optional<TestEnvironment> findByName(String name);
}
