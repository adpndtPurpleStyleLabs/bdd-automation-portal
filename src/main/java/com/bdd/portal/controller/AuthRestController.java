package com.bdd.portal.controller;

import com.bdd.portal.entity.User;
import com.bdd.portal.service.AuditService;
import com.bdd.portal.service.OtpService;
import com.bdd.portal.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private final UserService userService;
    private final OtpService otpService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");
        if (email != null) {
            User user = userService.findByEmail(email);
            if (user != null) {
                String otp = otpService.generateOtp(user, httpRequest.getRemoteAddr());
                auditService.logAction(user.getUsername(), "OTP_REQUESTED", httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), "Forgot password OTP requested");
                
                // Publish event to trigger email
                eventPublisher.publishEvent(new OtpRequestedEvent(user, otp));
            }
        }
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(Map.of("message", "If the account exists, an OTP has been sent."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");
        String otp = request.get("otp");
        
        try {
            boolean isValid = otpService.verifyOtp(email, otp);
            auditService.logAction(email, "OTP_VERIFIED", httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), "OTP verified successfully");
            return ResponseEntity.ok(Map.of("success", true, "message", "OTP verified"));
        } catch (IllegalArgumentException e) {
            auditService.logAction(email, "OTP_FAILED", httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), "OTP verification failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        try {
            // Verify OTP again just to be secure before resetting
            otpService.verifyOtp(email, otp);
            
            User user = userService.findByEmail(email);
            if (user != null) {
                userService.updatePassword(user.getId(), newPassword);
                auditService.logAction(user.getUsername(), "PASSWORD_RESET", httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), "Password reset via OTP");
                
                eventPublisher.publishEvent(new PasswordChangedEvent(user));
                return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    public record OtpRequestedEvent(User user, String otp) {}
    public record PasswordChangedEvent(User user) {}
}
