package com.proxymed.service.model;

import com.proxymed.enums.TypeVisite;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

/**
 * Creation d'une visite vierge a l'impression : un patient existant et le type de visite, rien d'autre.
 */
@Builder(toBuilder = true)
public record VisiteViergeRequest(
        @NotNull(message = "le patient est obligatoire")
        UUID patientId,

        @NotNull(message = "le type de visite est obligatoire (INITIALE ou ROUTINE)")
        TypeVisite typeVisite
) {
}
