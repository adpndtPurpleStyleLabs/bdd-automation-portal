package com.bdd.portal.service;

import com.bdd.portal.entity.PasswordResetOtp;
import com.bdd.portal.entity.User;
import com.bdd.portal.repository.PasswordResetOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    @Transactional
    public String generateOtp(User user, String ipAddress) {
        // Invalidate previous active OTPs
        List<PasswordResetOtp> activeOtps = otpRepository.findByUserAndUsedFalseAndExpiresAtAfter(user, LocalDateTime.now());
        for (PasswordResetOtp otp : activeOtps) {
            otp.setUsed(true); // Mark as used to invalidate
        }
        otpRepository.saveAll(activeOtps);

        // Generate 6 digit OTP
        int number = secureRandom.nextInt(999999);
        String otpStr = String.format("%06d", number);

        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setUser(user);
        otp.setEmail(user.getEmail());
        otp.setOtpHash(passwordEncoder.encode(otpStr));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otp.setIpAddress(ipAddress);
        
        otpRepository.save(otp);
        return otpStr;
    }

    @Transactional
    public boolean verifyOtp(String email, String rawOtp) {
        PasswordResetOtp otp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found for this email"));

        if (otp.isUsed()) {
            throw new IllegalArgumentException("OTP has already been used");
        }

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired");
        }

        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Maximum attempts reached. Please request a new OTP.");
        }

        if (passwordEncoder.matches(rawOtp, otp.getOtpHash())) {
            otp.setUsed(true);
            otpRepository.save(otp);
            return true;
        } else {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepository.save(otp);
            throw new IllegalArgumentException("Invalid OTP");
        }
    }
}
