package com.exemple.service;

import com.exemple.dto.request.LoginRequest;
import com.exemple.dto.request.RegisterRequest;
import com.exemple.dto.request.VerifyOtpRequest;
import com.exemple.dto.response.AuthResponse;
import com.exemple.dto.response.RegisterResponse;
import com.exemple.entity.RefreshToken;
import com.exemple.entity.User;
import com.exemple.repository.RefreshTokenRepository;
import com.exemple.repository.UserRepository;
import com.exemple.security.JwtService;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email déjà utilisé");
        if (userRepository.existsByUsername(req.getUsername()))
            throw new RuntimeException("Username déjà utilisé");

        String totpSecret = new DefaultSecretGenerator().generate();

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.ROLE_CLIENT)
                .statut(User.Statut.PENDING)
                .totpSecret(totpSecret)
                .twoFaEnabled(true)
                .build();

        user = userRepository.save(user);

        String qrUrl = "otpauth://totp/BankApp:" + req.getEmail()
                + "?secret=" + totpSecret + "&issuer=BankApp";

        return RegisterResponse.builder()
                .userId(user.getId())
                .message("Compte créé. En attente d'approbation admin.")
                .totpSecret(totpSecret)
                .totpQrUrl(qrUrl)
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Identifiants incorrects"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
            throw new RuntimeException("Identifiants incorrects");

        if (user.getStatut() == User.Statut.PENDING)
            throw new RuntimeException("Compte en attente d'approbation");

        if (user.getStatut() == User.Statut.BLOCKED)
            throw new RuntimeException("Compte bloqué");

        if (user.isTwoFaEnabled()) {
            return AuthResponse.builder()
                    .message("OTP_REQUIRED")
                    .build();
        }

        return generateTokens(user);
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(
                codeGenerator, new SystemTimeProvider());

        boolean valid = verifier.isValidCode(user.getTotpSecret(), req.getOtpCode());
        if (!valid)
            throw new RuntimeException("Code OTP invalide");

        return generateTokens(user);
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (stored.isRevoked() || stored.isExpired())
            throw new RuntimeException("Token expiré ou révoqué");

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return generateTokens(stored.getUser());
    }

    @Transactional
    public void logout(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    private AuthResponse generateTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefresh = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefresh))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ip("unknown")
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefresh)
                .role(user.getRole().name())
                .message("Authentification réussie")
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erreur hachage token");
        }
    }
}
