package com.exemple.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal solde;

    private String totpSecret;

    @Column(nullable = false)
    private boolean twoFaEnabled;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (solde == null) solde = BigDecimal.ZERO;
        if (statut == null) statut = Statut.PENDING;
        if (role == null) role = Role.ROLE_CLIENT;
        twoFaEnabled = false;
    }

    public enum Role {
        ROLE_CLIENT, ROLE_ADMIN
    }

    public enum Statut {
        PENDING, APPROVED, BLOCKED
    }
}
