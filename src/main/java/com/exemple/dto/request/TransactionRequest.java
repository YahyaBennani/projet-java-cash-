package com.exemple.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {
    private Long destinataireId;
    private BigDecimal montant;
    private String devise;
    private String description;
}
