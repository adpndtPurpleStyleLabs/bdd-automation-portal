package com.bdd.portal.service;

import com.bdd.portal.entity.AuditLog;
import com.bdd.portal.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void logAction(String username, String action, String ipAddress, String browser, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUsername(username);
            auditLog.setAction(action);
            auditLog.setIpAddress(ipAddress);
            auditLog.setBrowser(browser);
            auditLog.setDetails(details);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    public Page<AuditLog> getAuditLogsForUser(String username, Pageable pageable) {
        return auditLogRepository.findByUsernameOrderByTimeDesc(username, pageable);
    }
}
