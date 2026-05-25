package com.exemple.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private User expediteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private User destinataire;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @Column(nullable = false)
    private String devise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut;

    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;



    public enum Statut {
        COMPLETED,  // exécutée — irréversible
        FAILED      // échouée (solde insuffisant, OFAC, etc.)
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (devise == null) devise = "MAD";
    }
}
