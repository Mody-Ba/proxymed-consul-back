package com.proxymed.service.model;

import com.proxymed.enums.RoleMedecin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MedecinRequest(
        @NotBlank(message = "le nom est obligatoire")
        String nom,

        @NotBlank(message = "le prenom est obligatoire")
        String prenom,

        @NotBlank(message = "le numero d'ordre est obligatoire")
        String numeroOrdre,

        @NotNull(message = "le role est obligatoire")
        RoleMedecin role,

        Long structureRattachementId
) {
}
