package com.proxymed.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Pour les tests d'integration metier : chaque requete MockMvc part avec un badge valide de
 * medecin senior, sauf si le test pose lui-meme un en-tete Authorization.
 * Les cas 401 / 403 sont verifies a part, dans SecuriteIntegrationTest.
 */
@TestConfiguration
@Import(FabriqueBadge.class)
public class BadgeMedecinParDefaut {

    @Bean
    MockMvcBuilderCustomizer badgeMedecinParDefaut(FabriqueBadge fabriqueBadge) {
        return builder -> builder.defaultRequest(get("/")
                .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_SENIOR")));
    }
}
