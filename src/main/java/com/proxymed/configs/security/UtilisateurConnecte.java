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

    /**
     * Vrai pour un MEDECIN_JUNIOR qui n'a pas aussi le role MEDECIN_SENIOR : son acces au PDF et
     * a la fiche signee d'une consultation se limite aux consultations qui lui sont assignees.
     */
    public boolean accesLimiteAuxConsultationsAssignees() {
        return roles.contains("MEDECIN_JUNIOR") && !roles.contains("MEDECIN_SENIOR");
    }
}
