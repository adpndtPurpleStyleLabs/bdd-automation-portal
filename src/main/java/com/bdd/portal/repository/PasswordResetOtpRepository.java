package com.bdd.portal.repository;

import com.bdd.portal.entity.PasswordResetOtp;
import com.bdd.portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByUserOrderByCreatedAtDesc(User user);
    Optional<PasswordResetOtp> findTopByEmailOrderByCreatedAtDesc(String email);
    
    // Find valid OTPs for a user
    List<PasswordResetOtp> findByUserAndUsedFalseAndExpiresAtAfter(User user, LocalDateTime now);
}
