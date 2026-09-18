package com.proxymed.controller;

import com.proxymed.entity.SituationSociale;
import com.proxymed.repository.SituationSocialeRepository;
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
class SituationSocialeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SituationSocialeRepository situationSocialeRepository;

    @Test
    void listeSeulementLesElementsActifs() throws Exception {
        situationSocialeRepository.save(SituationSociale.builder().libelle("Isolement familial").actif(true).build());
        situationSocialeRepository.save(SituationSociale.builder().libelle("Ancienne situation").actif(false).build());

        mockMvc.perform(get("/api/referentiels/situations-sociales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].libelle").value("Isolement familial"));
    }
}
