package com.proxymed.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Fabrique des badges (JWT) comme le ferait le service d'authentification, signes avec le faux
 * secret de src/test/resources/application.properties. A importer dans un test avec @Import.
 */
@TestComponent
public class FabriqueBadge {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.emetteur}")
    private String emetteur;

    /** Valeur prete pour l'en-tete Authorization : badge valide une heure, portant ce role. */
    public String bearer(String role) {
        return bearer(role, UUID.randomUUID());
    }

    /** Comme bearer(role), pour un compte precis (claim "sub") : utile pour tester "ses propres" donnees. */
    public String bearer(String role, UUID utilisateurId) {
        return "Bearer " + badge(role, utilisateurId, jwtSecret, Instant.now().plusSeconds(3600));
    }

    /** Badge bien signe mais expire depuis une minute. */
    public String bearerExpire(String role) {
        return "Bearer " + badge(role, UUID.randomUUID(), jwtSecret, Instant.now().minusSeconds(60));
    }

    /** Badge signe avec un autre secret que celui du service d'authentification. */
    public String bearerFalsifie(String role) {
        return "Bearer " + badge(role, UUID.randomUUID(), "un-autre-secret-que-celui-du-service-auth-0123456789",
                Instant.now().plusSeconds(3600));
    }

    private String badge(String role, UUID utilisateurId, String secret, Instant expiration) {
        return Jwts.builder()
                .issuer(emetteur)
                .subject(utilisateurId.toString())
                .claim("email", role.toLowerCase() + "@proxymed.test")
                .claim("roles", List.of(role))
                .issuedAt(Date.from(expiration.minusSeconds(3600)))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }
}
