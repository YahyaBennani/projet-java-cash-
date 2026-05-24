package com.exemple.controller;

import com.exemple.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/me")
public class UserController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> profil(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
            "id",       user.getId(),
            "username", user.getUsername(),
            "email",    user.getEmail(),
            "role",     user.getRole(),
            "statut",   user.getStatut(),
            "solde",    user.getSolde()
        ));
    }
}
