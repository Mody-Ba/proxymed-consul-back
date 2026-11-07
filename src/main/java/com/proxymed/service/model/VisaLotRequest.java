package com.proxymed.service.model;

import com.proxymed.service.VisaFichesLotService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Visa en lot : les visites dont la fiche EN_ATTENTE est visee. Le senior (identifiant et nom) vient
 * toujours du badge, jamais de cette requete.
 */
public record VisaLotRequest(
        @NotEmpty(message = "la liste des visites ne doit pas etre vide")
        @Size(max = VisaFichesLotService.MAX_FICHES,
                message = "au plus " + VisaFichesLotService.MAX_FICHES + " fiches par requete")
        List<@NotNull(message = "un identifiant de visite ne peut pas etre vide") UUID> visiteIds
) {
}
