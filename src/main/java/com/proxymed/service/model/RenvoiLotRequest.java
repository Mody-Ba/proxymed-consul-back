package com.proxymed.service.model;

import com.proxymed.service.VisaFichesLotService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Renvoi en lot : les visites dont la fiche EN_ATTENTE est renvoyee, avec un seul motif pour toutes.
 */
public record RenvoiLotRequest(
        @NotEmpty(message = "la liste des visites ne doit pas etre vide")
        @Size(max = VisaFichesLotService.MAX_FICHES,
                message = "au plus " + VisaFichesLotService.MAX_FICHES + " fiches par requete")
        List<@NotNull(message = "un identifiant de visite ne peut pas etre vide") UUID> visiteIds,

        @NotBlank(message = "le motif du renvoi est obligatoire")
        @Size(max = VisaFichesLotService.MOTIF_MAX,
                message = "le motif du renvoi ne doit pas depasser " + VisaFichesLotService.MOTIF_MAX + " caracteres")
        String motif
) {
}
