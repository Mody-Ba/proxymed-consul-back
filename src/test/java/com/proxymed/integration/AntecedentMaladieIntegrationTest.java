package com.proxymed.integration;

import tools.jackson.databind.ObjectMapper;
import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.MaladieChronique;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.MaladieChroniqueRepository;
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

import static org.hamcrest.Matchers.hasSize;
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
class AntecedentMaladieIntegrationTest {

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
    @Autowired
    private MaladieChroniqueRepository maladieChroniqueRepository;

    private UUID consultationId;
    private Long diabeteId;
    private Long htaId;

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

        diabeteId = maladieChroniqueRepository.save(MaladieChronique.builder().libelle("Diabete").actif(true).build()).getId();
        htaId = maladieChroniqueRepository.save(MaladieChronique.builder().libelle("HTA").actif(true).build()).getId();
    }

    @Test
    void cycleDeVieComplet_ajout_lecture_modification_suppression() throws Exception {
        String reponse = mockMvc.perform(post("/api/consultations/" + consultationId + "/antecedents-maladies")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "maladieChroniqueId", diabeteId, "precision", "depuis 2019"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maladieChroniqueLibelle", is("Diabete")))
                .andReturn().getResponse().getContentAsString();
        long antecedentId = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(get("/api/consultations/" + consultationId + "/antecedents-maladies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].precision", is("depuis 2019")));

        mockMvc.perform(put("/api/consultations/" + consultationId + "/antecedents-maladies/" + antecedentId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "maladieChroniqueId", htaId, "precision", "depuis 2021"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maladieChroniqueLibelle", is("HTA")))
                .andExpect(jsonPath("$.precision", is("depuis 2021")));

        mockMvc.perform(delete("/api/consultations/" + consultationId + "/antecedents-maladies/" + antecedentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/consultations/" + consultationId + "/antecedents-maladies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void ajouter_rejette_siMaladieChroniqueInconnue() throws Exception {
        mockMvc.perform(post("/api/consultations/" + consultationId + "/antecedents-maladies")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("maladieChroniqueId", 999999L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void ajouter_rejette_siChampObligatoireManquant() throws Exception {
        mockMvc.perform(post("/api/consultations/" + consultationId + "/antecedents-maladies")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifier_renvoie404_siAntecedentInconnu() throws Exception {
        mockMvc.perform(put("/api/consultations/" + consultationId + "/antecedents-maladies/999999")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("maladieChroniqueId", diabeteId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void supprimer_renvoie404_siAntecedentInconnu() throws Exception {
        mockMvc.perform(delete("/api/consultations/" + consultationId + "/antecedents-maladies/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void operations_rejetees_siFicheSignee() throws Exception {
        ConsultationInitiale consultation = consultationRepository.findById(consultationId).orElseThrow();
        consultation.setStatut(StatutConsultation.SIGNEE);
        consultationRepository.save(consultation);

        mockMvc.perform(post("/api/consultations/" + consultationId + "/antecedents-maladies")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("maladieChroniqueId", diabeteId))))
                .andExpect(status().isConflict());
    }
}
