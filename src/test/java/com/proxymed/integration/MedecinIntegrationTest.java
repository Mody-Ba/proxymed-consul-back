package com.proxymed.integration;

import tools.jackson.databind.ObjectMapper;
import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.entity.Structure;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.repository.StructureRepository;
import com.proxymed.service.model.MedecinRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
class MedecinIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StructureRepository structureRepository;
    @Autowired
    private MedecinRepository medecinRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ConsultationRepository consultationRepository;

    @Test
    void cycleDeVieComplet_creation_lecture_miseAJour_suppression() throws Exception {
        Structure structure = structureRepository.save(Structure.builder().libelle("Centre A").actif(true).build());

        MedecinRequest creation = new MedecinRequest("Diop", "Awa", "SEN-" + System.nanoTime(),
                RoleMedecin.SENIOR, structure.getId());

        String reponse = mockMvc.perform(post("/api/medecins")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(creation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom", is("Diop")))
                .andExpect(jsonPath("$.structureRattachementLibelle", is("Centre A")))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(get("/api/medecins/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom", is("Awa")));

        MedecinRequest miseAJour = new MedecinRequest("Diop", "Awa Fatou", creation.numeroOrdre(),
                RoleMedecin.SENIOR, structure.getId());
        mockMvc.perform(put("/api/medecins/" + id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(miseAJour)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom", is("Awa Fatou")));

        mockMvc.perform(delete("/api/medecins/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/medecins/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_filtreParRole() throws Exception {
        medecinRepository.save(Medecin.builder().nom("Fall").prenom("Omar")
                .numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        medecinRepository.save(Medecin.builder().nom("Sy").prenom("Kine")
                .numeroOrdre("JUN-" + System.nanoTime()).role(RoleMedecin.JUNIOR).build());

        mockMvc.perform(get("/api/medecins").param("role", "JUNIOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].role", hasSize(1)))
                .andExpect(jsonPath("$[0].nom", is("Sy")));
    }

    @Test
    void create_rejette_siChampsObligatoiresManquants() throws Exception {
        MedecinRequest invalide = new MedecinRequest("", "", "", null, null);

        mockMvc.perform(post("/api/medecins")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalide)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejette_siStructureInconnue() throws Exception {
        MedecinRequest req = new MedecinRequest("Diop", "Awa", "SEN-" + System.nanoTime(), RoleMedecin.SENIOR, 999999L);

        mockMvc.perform(post("/api/medecins")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_renvoie404_siMedecinInconnu() throws Exception {
        mockMvc.perform(get("/api/medecins/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_renvoie404_siMedecinInconnu() throws Exception {
        MedecinRequest req = new MedecinRequest("Diop", "Awa", "SEN-" + System.nanoTime(), RoleMedecin.SENIOR, null);

        mockMvc.perform(put("/api/medecins/999999")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_renvoie404_siMedecinInconnu() throws Exception {
        mockMvc.perform(delete("/api/medecins/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_rejette_siMedecinRattacheAUneConsultation() throws Exception {
        Medecin medecinSenior = medecinRepository.save(Medecin.builder().nom("Ba").prenom("Mody")
                .numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        Patient patient = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-" + System.nanoTime())
                .nomComplet("Patient Test").dateNaissance(LocalDate.of(1960, 1, 1))
                .sexe(Sexe.M).couvertureSociale(CouvertureSociale.CMU).build());
        consultationRepository.save(ConsultationInitiale.builder()
                .patient(patient).medecinSenior(medecinSenior)
                .statut(StatutConsultation.BROUILLON).build());

        mockMvc.perform(delete("/api/medecins/" + medecinSenior.getId()))
                .andExpect(status().isBadRequest());
    }
}
