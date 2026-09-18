package com.proxymed.controller;

import com.proxymed.entity.Structure;
import com.proxymed.repository.StructureRepository;
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
class StructureApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StructureRepository structureRepository;

    @Test
    void listeSeulementLesStructuresActives() throws Exception {
        structureRepository.save(Structure.builder().libelle("Centre de sante Grand Dakar").actif(true).build());
        structureRepository.save(Structure.builder().libelle("Ancien centre ferme").actif(false).build());

        mockMvc.perform(get("/api/referentiels/structures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].libelle").value("Centre de sante Grand Dakar"));
    }
}
