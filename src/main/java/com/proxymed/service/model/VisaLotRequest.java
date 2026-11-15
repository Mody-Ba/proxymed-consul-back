package com.proxymed.service.model;

import com.proxymed.enums.FrequenceVisites;
import com.proxymed.service.VisaFichesLotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Visa en lot : les fiches EN_ATTENTE a viser, chacune avec la frequence des visites decidee par le
 * senior (facultative, refusee pour une visite de ROUTINE). Le senior (identifiant et nom) vient
 * toujours du badge, jamais de cette requete.
 * Exemple : {"fiches":[{"visiteId":"...","frequenceVisites":"HEBDOMADAIRE"},{"visiteId":"..."}]}
 */
public record VisaLotRequest(
        @NotEmpty(message = "la liste des fiches ne doit pas etre vide")
        @Size(max = VisaFichesLotService.MAX_FICHES,
                message = "au plus " + VisaFichesLotService.MAX_FICHES + " fiches par requete")
        List<@NotNull(message = "une fiche de la liste ne peut pas etre vide") @Valid Fiche> fiches
) {

    /** Une fiche a viser : sa visite et, pour une visite INITIALE, la frequence des visites (facultative). */
    public record Fiche(
            @NotNull(message = "l'identifiant de la visite est obligatoire")
            UUID visiteId,

            FrequenceVisites frequenceVisites
    ) {
    }
}
