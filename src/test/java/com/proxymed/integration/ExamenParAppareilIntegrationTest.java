package com.proxymed.integration;

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
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExamenParAppareilIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
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
    void findOne_renvoie404_siAucunExamenEnregistre() throws Exception {
        mockMvc.perform(get("/api/consultations/" + consultationId + "/examen-par-appareil"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creerPuisMettreAJour_viaUpsert() throws Exception {
        mockMvc.perform(put("/api/consultations/" + consultationId + "/examen-par-appareil")
                        .contentType("application/json")
                        .content("""
                                {"etatGeneral":"bon","cardioVasculaire":"RAS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etatGeneral", is("bon")));

        mockMvc.perform(get("/api/consultations/" + consultationId + "/examen-par-appareil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardioVasculaire", is("RAS")));

        // Deuxieme appel PUT -> mise a jour de la meme fiche (pas de doublon)
        mockMvc.perform(put("/api/consultations/" + consultationId + "/examen-par-appareil")
                        .contentType("application/json")
                        .content("""
                                {"etatGeneral":"altere","respiratoire":"sibilants"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etatGeneral", is("altere")))
                .andExpect(jsonPath("$.respiratoire", is("sibilants")));

        mockMvc.perform(get("/api/consultations/" + consultationId + "/examen-par-appareil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etatGeneral", is("altere")));
    }

    @Test
    void creerOuMettreAJour_rejette_siFicheSignee() throws Exception {
        ConsultationInitiale consultation = consultationRepository.findById(consultationId).orElseThrow();
        consultation.setStatut(StatutConsultation.SIGNEE);
        consultationRepository.save(consultation);

        mockMvc.perform(put("/api/consultations/" + consultationId + "/examen-par-appareil")
                        .contentType("application/json")
                        .content("""
                                {"etatGeneral":"bon"}
                                """))
                .andExpect(status().isConflict());
    }
}
