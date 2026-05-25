package com.exemple.controller;

import com.exemple.dto.request.TransactionRequest;
import com.exemple.dto.response.TransactionResponse;
import com.exemple.entity.User;
import com.exemple.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> initier(
            @RequestBody TransactionRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(201)
                .body(transactionService.initier(req, user));
    }

    // ADMIN → toutes les transactions (y compris FAILED)
    // CLIENT → seulement les siennes
    @GetMapping("/historique")
    public ResponseEntity<Page<TransactionResponse>> historique(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (user.getRole() == User.Role.ROLE_ADMIN) {
            return ResponseEntity.ok(transactionService.toutesLesTransactions(pageable));
        }

        return ResponseEntity.ok(transactionService.historique(user, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transactionService.detail(id, user));
    }
}
