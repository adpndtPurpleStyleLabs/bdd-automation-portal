package com.bdd.portal.controller;

import com.bdd.portal.entity.User;
import com.bdd.portal.service.AuditService;
import com.bdd.portal.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user, Authentication authentication, HttpServletRequest request) {
        try {
            String rawPassword = user.getPassword(); // Store before it gets encoded
            User created = userService.createUser(user);
            auditService.logAction(authentication.getName(), "CREATE_USER", request.getRemoteAddr(), request.getHeader("User-Agent"), "Created user " + user.getEmail());
            
            // Publish event to trigger welcome email
            eventPublisher.publishEvent(new UserCreatedEvent(created, rawPassword));
            
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails, Authentication authentication, HttpServletRequest request) {
        User existingUser = userService.findById(id);
        if (existingUser == null) {
            return ResponseEntity.notFound().build();
        }

        existingUser.setFirstName(userDetails.getFirstName());
        existingUser.setLastName(userDetails.getLastName());
        existingUser.setPhone(userDetails.getPhone());
        existingUser.setDepartment(userDetails.getDepartment());
        existingUser.setJobTitle(userDetails.getJobTitle());
        existingUser.setRole(userDetails.getRole());
        existingUser.setEnabled(userDetails.isEnabled());
        
        // Email changes require confirmation in a full implementation, but assuming ADMIN can change directly here if needed.
        if (userDetails.getEmail() != null && !existingUser.getEmail().equals(userDetails.getEmail())) {
            if (userService.findByEmail(userDetails.getEmail()) != null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }
            existingUser.setEmail(userDetails.getEmail());
            existingUser.setUsername(userDetails.getEmail());
        }

        User updated = userService.updateUser(existingUser);
        auditService.logAction(authentication.getName(), "UPDATE_USER", request.getRemoteAddr(), request.getHeader("User-Agent"), "Updated user " + existingUser.getEmail());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        User existingUser = userService.findById(id);
        if (existingUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Soft delete
        existingUser.setEnabled(false);
        userService.updateUser(existingUser);
        
        auditService.logAction(authentication.getName(), "DELETE_USER", request.getRemoteAddr(), request.getHeader("User-Agent"), "Disabled user " + existingUser.getEmail());
        return ResponseEntity.ok(Map.of("message", "User disabled successfully"));
    }
    
    public record UserCreatedEvent(User user, String rawPassword) {}
}
