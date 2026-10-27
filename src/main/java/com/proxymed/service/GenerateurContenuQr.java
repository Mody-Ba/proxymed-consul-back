package com.proxymed.service;

import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.ContenuQrModel;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Construit le texte encode dans le QR code de la Fiche 1, relu a l'import par
 * {@link LecteurContenuQr}. L'id de la consultation designe la fiche ; l'id du patient permet
 * de verifier a l'import que la fiche est rangee dans le bon dossier.
 */
@Component
public class GenerateurContenuQr {

    public static final String TYPE_FICHE_1_CONSULTATION_INITIALE = "FICHE_1_CONSULTATION_INITIALE";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public String construire(ConsultationModel consultation) {
        Objects.requireNonNull(consultation.patient(), "La consultation doit porter son patient");
        return construire(ContenuQrModel.builder()
                .type(TYPE_FICHE_1_CONSULTATION_INITIALE)
                .patientId(consultation.patient().id())
                .consultationId(consultation.id())
                .build());
    }

    String construire(ContenuQrModel contenu) {
        Map<String, String> champs = new LinkedHashMap<>();
        champs.put(LecteurContenuQr.CHAMP_TYPE, Objects.requireNonNull(contenu.type(), "type"));
        champs.put(LecteurContenuQr.CHAMP_PATIENT, Objects.requireNonNull(contenu.patientId(), "patientId").toString());
        champs.put(LecteurContenuQr.CHAMP_CONSULTATION,
                Objects.requireNonNull(contenu.consultationId(), "consultationId").toString());
        return JSON.writeValueAsString(champs);
    }
}
