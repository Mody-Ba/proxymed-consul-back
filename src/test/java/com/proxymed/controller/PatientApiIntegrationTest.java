package com.proxymed.controller;

import tools.jackson.databind.ObjectMapper;
import com.proxymed.service.model.PatientRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cycleDeVieComplet_creation_lecture_miseAJour() throws Exception {
        PatientRequest creation = new PatientRequest(
                "DOS-1001", "DMI-1001", "Awa Ndiaye", LocalDate.of(1950, 3, 12),
                com.proxymed.enums.Sexe.F, "770000000", "Rue 1", "Dakar", "Plateau",
                "Fils Ndiaye", "770000001", com.proxymed.enums.CouvertureSociale.CMU);

        String reponse = mockMvc.perform(post("/api/patients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(creation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomComplet", is("Awa Ndiaye")))
                .andExpect(jsonPath("$.age").exists())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(reponse).get("id").asText();

        mockMvc.perform(get("/api/patients/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDossierProxymed", is("DOS-1001")));

        PatientRequest miseAJour = new PatientRequest(
                "DOS-1001", "DMI-1001", "Awa Ndiaye Sarr", LocalDate.of(1950, 3, 12),
                com.proxymed.enums.Sexe.F, "770000000", "Rue 1", "Dakar", "Plateau",
                "Fils Ndiaye", "770000001", com.proxymed.enums.CouvertureSociale.CMU);

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
    void findById_renvoie404_siPatientInconnu() throws Exception {
        mockMvc.perform(get("/api/patients/" + java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
