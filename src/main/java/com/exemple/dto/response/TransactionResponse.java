package com.exemple.dto.response;

import com.exemple.entity.Transaction;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private String expediteurUsername;
    private String destinataireUsername;
    private BigDecimal montant;
    private String devise;
    private Transaction.Statut statut;
    private String description;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .expediteurUsername(t.getExpediteur().getUsername())
                .destinataireUsername(t.getDestinataire().getUsername())
                .montant(t.getMontant())
                .devise(t.getDevise())
                .statut(t.getStatut())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
