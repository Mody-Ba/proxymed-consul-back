package com.proxymed.integration;

import com.proxymed.support.FabriqueBadge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deux origines, avec un espace apres la virgule : verifie aussi le decoupage de PROXYMED_CORS_ORIGINS.
 * Pas de BadgeMedecinParDefaut ici : chaque test pose lui-meme (ou non) son en-tete Authorization.
 */
@SpringBootTest(properties = "proxymed.cors.origines=http://localhost:4200, https://front.proxymed.test")
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
class CorsIntegrationTest {

    private static final String URL_DEMANDES = "/api/demandes-consultation";
    private static final String URL_MEDECINS = "/api/medecins";
    private static final String ORIGINE_ANGULAR = "http://localhost:4200";
    private static final String AUTRE_ORIGINE_AUTORISEE = "https://front.proxymed.test";
    private static final String ORIGINE_INCONNUE = "http://site-inconnu.test";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FabriqueBadge fabriqueBadge;

    @Test
    void preVerification_demandesConsultation_sansBadge_pasDe401() throws Exception {
        mockMvc.perform(options(URL_DEMANDES)
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGINE_ANGULAR))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void preVerification_patchAccepterDemande_sansBadge_pasDe401() throws Exception {
        mockMvc.perform(options(URL_DEMANDES + "/1/accepter")
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGINE_ANGULAR))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")));
    }

    @Test
    void preVerification_secondeOrigineAutorisee_200() throws Exception {
        mockMvc.perform(options("/api/consultations/1/pdf")
                        .header(HttpHeaders.ORIGIN, AUTRE_ORIGINE_AUTORISEE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, AUTRE_ORIGINE_AUTORISEE));
    }

    @Test
    void preVerification_methodeNonAutorisee_403() throws Exception {
        mockMvc.perform(options(URL_DEMANDES)
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void preVerification_origineInconnue_refusee() throws Exception {
        mockMvc.perform(options(URL_DEMANDES)
                        .header(HttpHeaders.ORIGIN, ORIGINE_INCONNUE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void requete_origineInconnue_refusee() throws Exception {
        mockMvc.perform(get(URL_MEDECINS)
                        .header(HttpHeaders.ORIGIN, ORIGINE_INCONNUE)
                        .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_SENIOR")))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    /** Le front doit pouvoir lire Content-Disposition (nom des PDF telecharges). */
    @Test
    void requete_origineAutorisee_avecBadge_enTetesCorsEtContentDispositionExpose() throws Exception {
        mockMvc.perform(get(URL_MEDECINS)
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR)
                        .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_SENIOR")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGINE_ANGULAR))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Content-Disposition")))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    /** Le 401 JSON reste identique, et porte les en-tetes CORS pour que le front puisse le lire. */
    @Test
    void requete_endpointProtege_origineAutorisee_sansBadge_401AvecEnTetesCors() throws Exception {
        mockMvc.perform(get(URL_DEMANDES)
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/json")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGINE_ANGULAR))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, not("*")));
    }
}
