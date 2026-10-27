package com.proxymed.configs.security;

import java.util.List;
import java.util.UUID;

/**
 * Identite portee par le badge, placee dans le SecurityContext par AuthTokenFilter.
 * Construite uniquement a partir des claims : la Fiche 1 n'a aucune table d'utilisateurs.
 */
public record UtilisateurConnecte(
        UUID id,
        String email,
        List<String> roles
) {
}
