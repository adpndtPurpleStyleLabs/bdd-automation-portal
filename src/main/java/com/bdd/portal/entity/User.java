package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // The username will be the email as well for simplicity, or we can use a separate email. The plan says "Email unique". Let's use email as username or keep them separate.
    // User requested "Fields: First Name, Last Name, Email, Phone, Role, Department, Job Title, Status, Password"

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String phone;

    @Column
    private String department;

    @Column
    private String jobTitle;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column
    private String avatar;

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "require_password_change")
    private boolean requirePasswordChange = false;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
