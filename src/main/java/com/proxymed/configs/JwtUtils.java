package com.proxymed.configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Seul endroit qui verifie les badges (JWT) delivres par le service d'authentification. La
 * Fiche 1 n'en cree jamais : elle ne connait aucun utilisateur. Le reste du code ne manipule
 * que InfosBadge et une chaine opaque : passer de HS256 (secret partage) a RS256 (cle publique
 * ici) ne touchera que cette classe et sa configuration.
 */
@Slf4j
@Component
public class JwtUtils {

    static final String CLAIM_EMAIL = "email";
    static final String CLAIM_ROLES = "roles";

    /** HS256 exige une cle d'au moins 256 bits. */
    private static final int TAILLE_MIN_SECRET_OCTETS = 32;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.emetteur}")
    private String emetteur;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < TAILLE_MIN_SECRET_OCTETS) {
            throw new IllegalStateException("jwt.secret (variable PROXYMED_JWT_SECRET) doit faire au moins "
                    + TAILLE_MIN_SECRET_OCTETS + " octets pour verifier en HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    /**
     * Verifie signature, algorithme, emetteur et expiration.
     *
     * @return le contenu du badge, ou vide s'il est invalide, falsifie ou expire
     */
    public Optional<InfosBadge> verifierToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(emetteur)
                    .build()
                    .parseSignedClaims(token);
            if (!Jwts.SIG.HS256.getId().equals(jws.getHeader().getAlgorithm())) {
                log.debug("Badge refuse : algorithme {} non accepte", jws.getHeader().getAlgorithm());
                return Optional.empty();
            }
            Claims claims = jws.getPayload();
            return Optional.of(new InfosBadge(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    lireRoles(claims)));
        } catch (JwtException | IllegalArgumentException e) {
            // Le message ne contient jamais le badge lui-meme.
            log.debug("Badge refuse : {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static List<String> lireRoles(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        if (!(roles instanceof Collection<?> valeurs)) {
            throw new IllegalArgumentException("claim roles absent ou mal forme");
        }
        return valeurs.stream().map(String::valueOf).toList();
    }
}
