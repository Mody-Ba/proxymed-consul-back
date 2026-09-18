package com.proxymed.controller;

import com.proxymed.entity.MaladieChronique;
import com.proxymed.repository.MaladieChroniqueRepository;
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
class MaladieChroniqueApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MaladieChroniqueRepository maladieChroniqueRepository;

    @Test
    void listeSeulementLesElementsActifs() throws Exception {
        maladieChroniqueRepository.save(MaladieChronique.builder().libelle("Diabete").actif(true).build());
        maladieChroniqueRepository.save(MaladieChronique.builder().libelle("Ancienne maladie").actif(false).build());

        mockMvc.perform(get("/api/referentiels/maladies-chroniques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].libelle").value("Diabete"));
    }
}
