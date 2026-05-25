package com.exemple.security;

import com.exemple.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshExpiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Token complet — émis APRÈS login + OTP validés
    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .claim("type", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getKey())
                .compact();
    }

    // Token temporaire 5 min — émis APRÈS password vérifié, AVANT OTP
    // Ne donne accès à AUCUNE route protégée
    public String generatePreAuthToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "PRE_AUTH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 300_000)) // 5 min
                .signWith(getKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractClaims(token).getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Vérifie que c'est bien un PRE_AUTH token (pas un accessToken usurpé)
    public boolean isPreAuthToken(String token) {
        try {
            String type = extractClaims(token).get("type", String.class);
            return "PRE_AUTH".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    // Vérifie que c'est bien un ACCESS token (pas un preAuth usurpé)
    public boolean isAccessToken(String token) {
        try {
            String type = extractClaims(token).get("type", String.class);
            return "ACCESS".equals(type);
        } catch (Exception e) {
            return false;
        }
    }
}
