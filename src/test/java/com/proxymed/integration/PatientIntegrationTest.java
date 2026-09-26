package com.proxymed.integration;

import tools.jackson.databind.ObjectMapper;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.Sexe;
import com.proxymed.service.model.PatientRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cycleDeVieComplet_creation_lecture_miseAJour() throws Exception {
        PatientRequest creation = new PatientRequest(
                "DOS-" + System.nanoTime(), "DMI-1001", "Awa Ndiaye", LocalDate.of(1950, 3, 12),
                Sexe.F, "770000000", "Rue 1", "Dakar", "Plateau",
                "Fils Ndiaye", "770000001", CouvertureSociale.CMU);

        String reponse = mockMvc.perform(post("/api/patients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(creation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomComplet", is("Awa Ndiaye")))
                .andExpect(jsonPath("$.age", greaterThanOrEqualTo(70)))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(reponse).get("id").asText();

        mockMvc.perform(get("/api/patients/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDossierProxymed", is(creation.numeroDossierProxymed())));

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());

        PatientRequest miseAJour = new PatientRequest(
                creation.numeroDossierProxymed(), "DMI-1001", "Awa Ndiaye Sarr", LocalDate.of(1950, 3, 12),
                Sexe.F, "770000000", "Rue 1", "Dakar", "Plateau",
                "Fils Ndiaye", "770000001", CouvertureSociale.CMU);

        mockMvc.perform(put("/api/patients/" + id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(miseAJour)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomComplet", is("Awa Ndiaye Sarr")));
    }

    @Test
    void create_rejette_siChampsObligatoiresManquants() throws Exception {
        PatientRequest invalide = new PatientRequest(
                "", null, "", null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/patients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalide)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejette_siDateNaissanceDansLeFutur() throws Exception {
        PatientRequest invalide = new PatientRequest(
                "DOS-" + System.nanoTime(), null, "Futur Patient", LocalDate.now().plusDays(1),
                Sexe.M, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/patients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalide)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_renvoie404_siPatientInconnu() throws Exception {
        mockMvc.perform(get("/api/patients/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_renvoie404_siPatientInconnu() throws Exception {
        PatientRequest req = new PatientRequest(
                "DOS-" + System.nanoTime(), null, "Inconnu", LocalDate.of(1970, 1, 1),
                Sexe.M, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/patients/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
