package com.bdd.portal.emailer.repository;

import com.bdd.portal.emailer.entity.EmailQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EmailQueueRepository extends JpaRepository<EmailQueue, Long>, JpaSpecificationExecutor<EmailQueue> {
    List<EmailQueue> findByStatusIn(List<EmailQueue.EmailStatus> statuses);
}
