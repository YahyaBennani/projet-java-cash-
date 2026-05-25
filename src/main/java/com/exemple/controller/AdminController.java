package com.exemple.controller;

import com.exemple.dto.request.TransactionRequest;
import com.exemple.dto.response.TransactionResponse;
import com.exemple.entity.OfacEntry;
import com.exemple.entity.User;
import com.exemple.repository.OfacRepository;
import com.exemple.repository.UserRepository;
import com.exemple.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final OfacRepository ofacRepository;

    // Lister tous les clients
    @GetMapping("/clients")
    public ResponseEntity<List<User>> listerClients() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // Approuver un client
    @PostMapping("/clients/{id}/approve")
    public ResponseEntity<Map<String, String>> approuver(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setStatut(User.Statut.APPROVED);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Compte approuvé"));
    }

    // Bloquer un client
    @PostMapping("/clients/{id}/block")
    public ResponseEntity<Map<String, String>> bloquer(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setStatut(User.Statut.BLOCKED);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Compte bloqué"));
    }

    // Promouvoir client → admin
    @PostMapping("/clients/{id}/promote")
    public ResponseEntity<Map<String, String>> promouvoir(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setRole(User.Role.ROLE_ADMIN);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Utilisateur promu admin"));
    }

    // Créditer un compte (versement bancaire)
    @PostMapping("/clients/{id}/credit")
    public ResponseEntity<Map<String, Object>> crediter(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body,
            @AuthenticationPrincipal User admin) {
        BigDecimal montant = body.get("montant");
        transactionService.crediterSolde(id, montant, admin);
        User user = userRepository.findById(id).get();
        return ResponseEntity.ok(Map.of(
            "message", "Solde crédité avec succès",
            "nouveauSolde", user.getSolde()
        ));
    }

    // Admin fait une transaction comme un client
    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> transactionAdmin(
            @RequestBody TransactionRequest req,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.status(201)
                .body(transactionService.initier(req, admin));
    }

    // Voir toutes les transactions
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> toutesTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
            transactionService.toutesLesTransactions(PageRequest.of(page, size)));
    }

    // Gestion liste OFAC
    @GetMapping("/ofac")
    public ResponseEntity<List<OfacEntry>> listerOfac() {
        return ResponseEntity.ok(ofacRepository.findAll());
    }

    @PostMapping("/ofac")
    public ResponseEntity<OfacEntry> ajouterOfac(
            @RequestBody OfacEntry entry) {
        return ResponseEntity.status(201)
                .body(ofacRepository.save(entry));
    }

    @DeleteMapping("/ofac/{id}")
    public ResponseEntity<Void> supprimerOfac(@PathVariable Long id) {
        OfacEntry entry = ofacRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Entrée OFAC introuvable"));
        entry.setActif(false);
        ofacRepository.save(entry);
        return ResponseEntity.noContent().build();
    }
}
