package com.proxymed.configs.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 en JSON quand le badge est valide mais que son role ne donne pas acces a l'adresse.
 */
@Component
@RequiredArgsConstructor
public class AccesRefuseHandler implements AccessDeniedHandler {

    private final ReponseErreurJson reponseErreurJson;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        reponseErreurJson.ecrire(response, HttpServletResponse.SC_FORBIDDEN, "Acces refuse",
                "Votre role ne permet pas d'acceder a cette ressource");
    }
}
