package com.proxymed.configs;

import java.util.List;
import java.util.UUID;

/**
 * Contenu d'un badge verifie par JwtUtils : claims sub, email et roles.
 */
public record InfosBadge(
        UUID utilisateurId,
        String email,
        List<String> roles
) {
}
