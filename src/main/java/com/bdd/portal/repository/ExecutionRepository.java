package com.bdd.portal.repository;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long>, JpaSpecificationExecutor<Execution> {
    List<Execution> findByStatusOrderByIdAsc(ExecutionStatus status);
    long countByStatusAndBrowserIgnoreCase(ExecutionStatus status, String browser);
    List<Execution> findTop10ByOrderByStartTimeDesc();
    Page<Execution> findAll(Pageable pageable);
}
