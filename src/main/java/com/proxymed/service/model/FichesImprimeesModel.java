package com.proxymed.service.model;

import lombok.Builder;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.UUID;

/**
 * Resultat d'une impression : les visites creees, dans l'ordre des demandes (et donc des fiches du
 * PDF), et le PDF combine (Resource a usage unique, supprimee apres lecture).
 */
@Builder(toBuilder = true)
public record FichesImprimeesModel(
        List<UUID> visiteIds,
        Resource pdf
) {
}
