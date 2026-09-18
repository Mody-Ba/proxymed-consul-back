package com.proxymed.controller;

import tools.jackson.databind.ObjectMapper;
import com.proxymed.entity.Structure;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.service.model.MedecinRequest;
import com.proxymed.repository.StructureRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MedecinApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StructureRepository structureRepository;

    @Test
    void creerEtFiltrerParRole() throws Exception {
        Structure structure = structureRepository.save(Structure.builder().libelle("Centre de sante Grand Dakar").build());

        MedecinRequest senior = new MedecinRequest("Diop", "Awa", "SEN-1", RoleMedecin.SENIOR, structure.getId());
        MedecinRequest junior = new MedecinRequest("Fall", "Omar", "JUN-1", RoleMedecin.JUNIOR, null);

        mockMvc.perform(post("/api/medecins")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(senior)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.structureRattachementLibelle", is("Centre de sante Grand Dakar")));

        mockMvc.perform(post("/api/medecins")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(junior)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/medecins").param("role", "SENIOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].numeroOrdre", hasSize(1)))
                .andExpect(jsonPath("$[0].numeroOrdre", is("SEN-1")));
    }

    @Test
    void create_rejette_siStructureInconnue() throws Exception {
        MedecinRequest req = new MedecinRequest("Diop", "Awa", "SEN-2", RoleMedecin.SENIOR, 999999L);

        mockMvc.perform(post("/api/medecins")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
