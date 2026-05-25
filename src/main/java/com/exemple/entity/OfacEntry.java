package com.exemple.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ofac_entries")
public class OfacEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String pays;

    @Column(nullable = false)
    private boolean actif;

    @Column(nullable = false)
    private LocalDateTime dateAjout;

    @PrePersist
    protected void onCreate() {
        dateAjout = LocalDateTime.now();
        actif = true;
    }
}
