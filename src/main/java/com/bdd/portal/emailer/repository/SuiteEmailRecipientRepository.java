package com.bdd.portal.emailer.repository;

import com.bdd.portal.emailer.entity.SuiteEmailRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SuiteEmailRecipientRepository extends JpaRepository<SuiteEmailRecipient, Long> {
    List<SuiteEmailRecipient> findBySuiteId(Long suiteId);
}
