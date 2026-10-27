package com.proxymed.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bouton "Authorize" de Swagger : coller l'accessToken recu de /api/auth/connexion du service
 * d'authentification (sans le prefixe "Bearer "). Le badge est envoye sur toutes les operations.
 */
@Configuration
public class OpenApiConfig {

    public static final String SCHEMA_BADGE = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PROXYMED API")
                        .description("Fiche 1 : evaluation medicale initiale d'eligibilite. Toutes les adresses "
                                + "demandent le badge (JWT) delivre par le service d'authentification")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(SCHEMA_BADGE,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_BADGE));
    }
}
