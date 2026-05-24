package com.exemple.controller;

import com.exemple.entity.User;
import com.exemple.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/clients")
    public ResponseEntity<List<User>> listerClients() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/clients/{id}/approve")
    public ResponseEntity<Map<String, String>> approuver(
            @PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setStatut(User.Statut.APPROVED);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Compte approuvé"));
    }

    @PostMapping("/clients/{id}/block")
    public ResponseEntity<Map<String, String>> bloquer(
            @PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setStatut(User.Statut.BLOCKED);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Compte bloqué"));
    }
}
