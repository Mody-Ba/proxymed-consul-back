package com.proxymed.service.model;

import lombok.Builder;

import java.util.UUID;

/**
 * Contenu du QR code imprime sur une fiche PROXYMED : type de fiche, patient et consultation.
 * Serialise en JSON, ex. {"type":"FICHE_1_CONSULTATION_INITIALE","patientId":"...","consultationId":"..."}.
 */
@Builder(toBuilder = true)
public record ContenuQrModel(
        String type,
        UUID patientId,
        UUID consultationId
) {
}
