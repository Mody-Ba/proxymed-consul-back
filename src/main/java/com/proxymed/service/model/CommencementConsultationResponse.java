package com.proxymed.service.model;

import com.proxymed.enums.StatutConsultation;
import lombok.Builder;

import java.util.UUID;

/**
 * POST /api/demandes-consultation/{id}/commencer (201). Le detail se lit ensuite par
 * GET /api/consultations/{consultationId}.
 */
@Builder(toBuilder = true)
public record CommencementConsultationResponse(
        UUID demandeId,
        UUID patientId,
        UUID consultationId,
        StatutConsultation statutConsultation
) {
}
