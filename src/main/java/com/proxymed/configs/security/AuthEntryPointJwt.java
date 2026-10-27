package com.proxymed.configs.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 en JSON quand une adresse protegee est appelee sans badge valide.
 */
@Component
@RequiredArgsConstructor
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private final ReponseErreurJson reponseErreurJson;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String message = request.getAttribute(AuthTokenFilter.ATTRIBUT_BADGE_INVALIDE) != null
                ? "Badge invalide ou expire"
                : "Badge d'authentification manquant";
        reponseErreurJson.ecrire(response, HttpServletResponse.SC_UNAUTHORIZED, "Non authentifie", message);
    }
}
