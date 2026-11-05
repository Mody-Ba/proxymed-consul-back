package com.proxymed.service.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * Impression de fiches vierges par l'admin : une demande (patient existant + type de visite) par fiche.
 */
@Builder(toBuilder = true)
public record ImpressionFichesRequest(
        @NotEmpty(message = "la liste des fiches a imprimer ne doit pas etre vide")
        @Size(max = ImpressionFichesRequest.MAX_FICHES,
                message = "au plus " + ImpressionFichesRequest.MAX_FICHES + " fiches par impression")
        List<@NotNull(message = "une demande d'impression ne peut pas etre vide") @Valid VisiteViergeRequest> fiches
) {

    public static final int MAX_FICHES = 50;
}
