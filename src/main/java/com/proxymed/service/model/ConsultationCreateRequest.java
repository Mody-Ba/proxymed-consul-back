package com.proxymed.service.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Payload minimal pour creer une fiche en BROUILLON (section 1 : identification patient et medecin).
 */
@Builder(toBuilder = true)
public record ConsultationCreateRequest(
        @NotNull(message = "le patient est obligatoire")
        UUID patientId,

        @NotNull(message = "le medecin senior est obligatoire")
        Long medecinSeniorId,

        LocalDate dateConsultation,
        LocalTime heureConsultation,
        String saisiePar
) {
}
