package com.exemple.security;

import com.exemple.entity.User;
import com.exemple.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Token invalide ou expiré
        if (!jwtService.isTokenValid(token)) {
            sendError(response, 401, "Token invalide ou expiré");
            return;
        }

        // Un preAuthToken ne donne accès à AUCUNE route protégée
        // Il est uniquement accepté par /auth/verify-otp (géré dans AuthService)
        if (jwtService.isPreAuthToken(token)) {
            sendError(response, 401, "Token temporaire — complétez l'authentification via /auth/verify-otp");
            return;
        }

        Long userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            sendError(response, 401, "Utilisateur introuvable");
            return;
        }

        if (user.getStatut() == User.Statut.BLOCKED) {
            sendError(response, 403, "Compte bloqué");
            return;
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))
                );

        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"erreur\":\"" + message + "\"}");
    }
}
