package com.proxymed.service.model;

import com.proxymed.enums.TypeVisite;
import lombok.Builder;

import java.util.UUID;

/**
 * Contenu du QR code imprime sur une fiche PROXYMED : type de fiche, patient, consultation (la
 * visite) et type de visite. Serialise en JSON, ex. {"type":"FICHE_1_CONSULTATION_INITIALE",
 * "patientId":"...","consultationId":"...","typeVisite":"ROUTINE"}. typeVisite est null pour un QR
 * imprime avant son ajout (il reste lisible par l'import).
 */
@Builder(toBuilder = true)
public record ContenuQrModel(
        String type,
        UUID patientId,
        UUID consultationId,
        TypeVisite typeVisite
) {

    /** QR sans type de visite (format d'avant l'ajout de typeVisite). */
    public ContenuQrModel(String type, UUID patientId, UUID consultationId) {
        this(type, patientId, consultationId, null);
    }
}
