package com.sitewatch.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Getter @Setter @NoArgsConstructor
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(length = 120)
    private String email;

    /** BCrypt hash, never the plain password. */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    private LocalDateTime createdAt = LocalDateTime.now();
}
