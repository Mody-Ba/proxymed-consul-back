package com.proxymed.controller;

import com.proxymed.entity.FacteurDeRisque;
import com.proxymed.repository.FacteurDeRisqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FacteurDeRisqueApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FacteurDeRisqueRepository facteurDeRisqueRepository;

    @Test
    void listeSeulementLesElementsActifs() throws Exception {
        facteurDeRisqueRepository.save(FacteurDeRisque.builder().libelle("Tabac").actif(true).build());
        facteurDeRisqueRepository.save(FacteurDeRisque.builder().libelle("Ancien facteur").actif(false).build());

        mockMvc.perform(get("/api/referentiels/facteurs-risque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].libelle").value("Tabac"));
    }
}
