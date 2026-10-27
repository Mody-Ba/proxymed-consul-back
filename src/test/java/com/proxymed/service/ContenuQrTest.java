package com.proxymed.service;

import com.proxymed.exception.RegleGestionException;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.ContenuQrModel;
import com.proxymed.service.model.PatientModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Construction (GenerateurContenuQr) et lecture (LecteurContenuQr) du texte du QR code de la Fiche 1.
 */
class ContenuQrTest {

    private static final String PATIENT = "6f1c2a3b-4d5e-4f60-8a71-92b3c4d5e6f7";
    private static final String CONSULTATION = "0a1b2c3d-4e5f-4a6b-9c7d-8e9f0a1b2c3d";

    private final GenerateurContenuQr generateur = new GenerateurContenuQr();
    private final LecteurContenuQr lecteur = new LecteurContenuQr();

    @Test
    void construire_produitLeJsonAttendu() {
        ConsultationModel consultation = ConsultationModel.builder()
                .id(UUID.fromString(CONSULTATION))
                .patient(PatientModel.builder().id(UUID.fromString(PATIENT)).build())
                .build();

        assertThat(generateur.construire(consultation)).isEqualTo("{\"type\":\"FICHE_1_CONSULTATION_INITIALE\","
                + "\"patientId\":\"" + PATIENT + "\",\"consultationId\":\"" + CONSULTATION + "\"}");
    }

    @Test
    void construire_puisLire_restitueLeTypeLePatientEtLaConsultation() {
        UUID patientId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();
        ConsultationModel consultation = ConsultationModel.builder()
                .id(consultationId).patient(PatientModel.builder().id(patientId).build()).build();

        ContenuQrModel lu = lecteur.lire(generateur.construire(consultation));

        assertThat(lu).isEqualTo(new ContenuQrModel("FICHE_1_CONSULTATION_INITIALE", patientId, consultationId));
    }

    @Test
    void lire_accepteLesChampsDansNImporteQuelOrdre_etLesChampsEnPlus() {
        ContenuQrModel lu = lecteur.lire("{ \"consultationId\": \"" + CONSULTATION + "\", \"version\": 2, "
                + "\"patientId\": \"" + PATIENT.toUpperCase() + "\", \"type\": \"FICHE_1_CONSULTATION_INITIALE\" }");

        assertThat(lu.patientId()).isEqualTo(UUID.fromString(PATIENT));
        assertThat(lu.consultationId()).isEqualTo(UUID.fromString(CONSULTATION));
    }

    @Test
    void construire_exigeLIdDuPatientEtDeLaConsultation() {
        assertThatThrownBy(() -> generateur.construire(ConsultationModel.builder()
                .id(UUID.randomUUID()).patient(PatientModel.builder().build()).build()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> generateur.construire(ConsultationModel.builder()
                .patient(PatientModel.builder().id(UUID.randomUUID()).build()).build()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> generateur.construire(ConsultationModel.builder().id(UUID.randomUUID()).build()))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "https://exemple.com/qr",
            "{pas du json",
            "[\"FICHE_1_CONSULTATION_INITIALE\"]",
            "{\"patientId\":\"" + PATIENT + "\",\"consultationId\":\"" + CONSULTATION + "\"}",
            "{\"type\":\"FICHE_1_CONSULTATION_INITIALE\",\"consultationId\":\"" + CONSULTATION + "\"}",
            "{\"type\":\"FICHE_1_CONSULTATION_INITIALE\",\"patientId\":\"" + PATIENT + "\"}",
            "{\"type\":\"FICHE_1_CONSULTATION_INITIALE\",\"patientId\":\"pas-un-uuid\",\"consultationId\":\"" + CONSULTATION + "\"}",
            "{\"type\":\"FICHE_1_CONSULTATION_INITIALE\",\"patientId\":\"1-2-3-4-5\",\"consultationId\":\"" + CONSULTATION + "\"}",
            "{\"type\":\"FICHE_1_CONSULTATION_INITIALE\",\"patientId\":\"" + PATIENT + "\",\"consultationId\":42}",
            "{\"type\":null,\"patientId\":\"" + PATIENT + "\",\"consultationId\":\"" + CONSULTATION + "\"}",
            // Champ de la Fiche 2 (visiteId) au lieu de consultationId
            "{\"type\":\"FICHE_1_CONSULTATION_INITIALE\",\"patientId\":\"" + PATIENT + "\",\"visiteId\":\"" + CONSULTATION + "\"}"
    })
    void lire_rejetteCeQuiNEstPasUnQrProxymed(String texte) {
        assertThatThrownBy(() -> lecteur.lire(texte))
                .isInstanceOf(RegleGestionException.class)
                .hasMessage("Le code QR detecte n'est pas un code QR PROXYMED");
    }

    @Test
    void lire_rejetteLAncienFormat_idDeConsultationSeul_avecUnMessageExplicite() {
        assertThatThrownBy(() -> lecteur.lire(CONSULTATION))
                .isInstanceOf(RegleGestionException.class)
                .hasMessage(LecteurContenuQr.MESSAGE_ANCIEN_FORMAT)
                .hasMessageContaining("ancienne fiche")
                .hasMessageContaining("regenerez le PDF");
    }

    @Test
    void lire_rejetteUnAutreTypeDeFiche() {
        assertThatThrownBy(() -> lecteur.lire(
                "{\"type\":\"FICHE_2_SUIVI\",\"patientId\":\"" + PATIENT + "\",\"consultationId\":\"" + CONSULTATION + "\"}"))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("Type de fiche non pris en charge : FICHE_2_SUIVI");
    }
}
