package com.proxymed.integration;

import com.proxymed.entity.FacteurDeRisque;
import com.proxymed.repository.FacteurDeRisqueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FacteurDeRisqueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FacteurDeRisqueRepository facteurDeRisqueRepository;

    @Test
    void findAll_neRetourneQueLesFacteursActifs() throws Exception {
        facteurDeRisqueRepository.save(FacteurDeRisque.builder().libelle("Tabagisme").actif(true).build());
        facteurDeRisqueRepository.save(FacteurDeRisque.builder().libelle("Obsolete").actif(false).build());

        mockMvc.perform(get("/api/referentiels/facteurs-risque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].libelle", hasSize(1)))
                .andExpect(jsonPath("$[0].libelle", is("Tabagisme")));
    }
}
