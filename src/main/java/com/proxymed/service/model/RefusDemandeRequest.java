package com.proxymed.service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefusDemandeRequest(
        @NotBlank(message = "le motif de refus est obligatoire")
        @Size(max = 1000, message = "le motif de refus ne doit pas depasser 1000 caracteres")
        String motifRefus
) {
}
