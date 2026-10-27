package com.proxymed.service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Aucun identifiant de demandeur ici : il vient toujours du badge.
 */
public record DemandeConsultationRequest(
        @NotBlank(message = "le nom du patient est obligatoire")
        @Size(max = 200, message = "le nom du patient ne doit pas depasser 200 caracteres")
        String nomPatient,

        @NotNull(message = "pourProche est obligatoire (true si la demande est pour un proche)")
        Boolean pourProche,

        @NotBlank(message = "le motif est obligatoire")
        @Size(max = 2000, message = "le motif ne doit pas depasser 2000 caracteres")
        String motif
) {
}
