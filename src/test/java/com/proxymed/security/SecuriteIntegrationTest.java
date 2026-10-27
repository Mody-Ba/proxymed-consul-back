package com.proxymed.security;

import com.proxymed.support.FabriqueBadge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Badge du service d'authentification : 401 s'il est absent, falsifie ou expire, 403 si son role
 * n'est pas un role de medecin (regle provisoire), acces pour les medecins junior et senior.
 * Pas de BadgeMedecinParDefaut ici : chaque test pose lui-meme son en-tete Authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
class SecuriteIntegrationTest {

    private static final String URL = "/api/medecins";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FabriqueBadge fabriqueBadge;

    @Test
    void sansBadge_401() throws Exception {
        verifierErreur(mockMvc.perform(get(URL)), 401, "Non authentifie", "Badge d'authentification manquant");
    }

    @Test
    void badgeFalsifie_401() throws Exception {
        verifierErreur(appeler(fabriqueBadge.bearerFalsifie("MEDECIN_SENIOR")),
                401, "Non authentifie", "Badge invalide ou expire");
    }

    @Test
    void badgeIllisible_401() throws Exception {
        verifierErreur(appeler("Bearer pas-un-badge"), 401, "Non authentifie", "Badge invalide ou expire");
    }

    @Test
    void badgeExpire_401() throws Exception {
        verifierErreur(appeler(fabriqueBadge.bearerExpire("MEDECIN_SENIOR")),
                401, "Non authentifie", "Badge invalide ou expire");
    }

    @Test
    void utilisateur_403() throws Exception {
        verifierErreur(appeler(fabriqueBadge.bearer("UTILISATEUR")),
                403, "Acces refuse", "Votre role ne permet pas d'acceder a cette ressource");
    }

    @Test
    void admin_403() throws Exception {
        verifierErreur(appeler(fabriqueBadge.bearer("ADMIN")),
                403, "Acces refuse", "Votre role ne permet pas d'acceder a cette ressource");
    }

    @Test
    void medecinJunior_accesOk() throws Exception {
        appeler(fabriqueBadge.bearer("MEDECIN_JUNIOR")).andExpect(status().isOk());
    }

    @Test
    void medecinSenior_accesOk() throws Exception {
        appeler(fabriqueBadge.bearer("MEDECIN_SENIOR")).andExpect(status().isOk());
    }

    @Test
    void docOpenApi_publique() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme", is("bearer")));
    }

    private ResultActions appeler(String authorization) throws Exception {
        return mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, authorization));
    }

    private static void verifierErreur(ResultActions resultat, int statut, String erreur, String message) throws Exception {
        resultat.andExpect(status().is(statut))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status", is(statut)))
                .andExpect(jsonPath("$.erreur", is(erreur)))
                .andExpect(jsonPath("$.message", is(message)))
                .andExpect(jsonPath("$.horodatage", notNullValue()));
    }
}
