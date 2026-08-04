package com.bdd.portal.repository;

import com.bdd.portal.entity.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
    List<ScheduledJob> findByActiveTrueAndNextRunTimeLessThanEqual(LocalDateTime now);
    List<ScheduledJob> findByActiveTrueOrderByNextRunTimeAsc();
}
