package com.bdd.portal.repository;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {
    List<Execution> findByStatusOrderByStartTimeDesc(ExecutionStatus status);
    List<Execution> findTop10ByOrderByStartTimeDesc();
}
