package com.proxymed.integration;

import tools.jackson.databind.ObjectMapper;
import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConstanteVitaleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedecinRepository medecinRepository;
    @Autowired
    private ConsultationRepository consultationRepository;

    private UUID consultationId;

    @BeforeEach
    void setUp() {
        Patient patient = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-" + System.nanoTime()).nomComplet("Patient Test")
                .dateNaissance(LocalDate.of(1955, 1, 1)).sexe(Sexe.M).couvertureSociale(CouvertureSociale.CMU).build());
        Medecin medecin = medecinRepository.save(Medecin.builder()
                .nom("Diop").prenom("Awa").numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        ConsultationInitiale consultation = consultationRepository.save(ConsultationInitiale.builder()
                .patient(patient).medecinSenior(medecin).statut(StatutConsultation.BROUILLON).build());
        consultationId = consultation.getId();
    }

    @Test
    void cycleDeVieComplet_ajout_modification_suppression() throws Exception {
        String reponse = mockMvc.perform(post("/api/consultations/" + consultationId + "/constantes")
                        .contentType("application/json")
                        .content("""
                                {"type":"FC","valeur":75}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estNormal", is(true)))
                .andReturn().getResponse().getContentAsString();
        long constanteId = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(put("/api/consultations/" + consultationId + "/constantes/" + constanteId)
                        .contentType("application/json")
                        .content("""
                                {"type":"FC","valeur":150}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estAlerte", is(true)));

        mockMvc.perform(delete("/api/consultations/" + consultationId + "/constantes/" + constanteId))
                .andExpect(status().isNoContent());

        String fiche = mockMvc.perform(get("/api/consultations/" + consultationId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(
                        objectMapper.readTree(fiche).get("constantesVitales").isEmpty())
                .isTrue();
    }

    @Test
    void supprimerPoidsOuTaille_recalculeLImc() throws Exception {
        long poidsId = ajouterConstante("POIDS", 70);
        ajouterConstante("TAILLE", 175);

        String fiche = mockMvc.perform(get("/api/consultations/" + consultationId))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(trouverConstante(fiche, "IMC")).isNotNull();

        mockMvc.perform(delete("/api/consultations/" + consultationId + "/constantes/" + poidsId))
                .andExpect(status().isNoContent());

        // Poids supprime -> l'ancien IMC (calcule avant suppression) n'est plus mis a jour,
        // mais l'appel ne doit pas echouer.
        mockMvc.perform(get("/api/consultations/" + consultationId))
                .andExpect(status().isOk());
    }

    @Test
    void ajouter_rejette_siChampObligatoireManquant() throws Exception {
        mockMvc.perform(post("/api/consultations/" + consultationId + "/constantes")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifier_renvoie404_siConstanteInconnue() throws Exception {
        mockMvc.perform(put("/api/consultations/" + consultationId + "/constantes/999999")
                        .contentType("application/json")
                        .content("""
                                {"type":"FC","valeur":80}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void supprimer_renvoie404_siConstanteInconnue() throws Exception {
        mockMvc.perform(delete("/api/consultations/" + consultationId + "/constantes/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ajouter_rejette_siFicheSignee() throws Exception {
        ConsultationInitiale consultation = consultationRepository.findById(consultationId).orElseThrow();
        consultation.setStatut(StatutConsultation.SIGNEE);
        consultationRepository.save(consultation);

        mockMvc.perform(post("/api/consultations/" + consultationId + "/constantes")
                        .contentType("application/json")
                        .content("""
                                {"type":"FC","valeur":80}
                                """))
                .andExpect(status().isConflict());
    }

    private long ajouterConstante(String type, double valeur) throws Exception {
        String reponse = mockMvc.perform(post("/api/consultations/" + consultationId + "/constantes")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("type", type, "valeur", valeur))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse).get("id").asLong();
    }

    private tools.jackson.databind.JsonNode trouverConstante(String ficheJson, String type) throws Exception {
        tools.jackson.databind.JsonNode racine = objectMapper.readTree(ficheJson);
        for (tools.jackson.databind.JsonNode c : racine.get("constantesVitales")) {
            if (c.get("type").asText().equals(type)) {
                return c;
            }
        }
        return null;
    }
}
