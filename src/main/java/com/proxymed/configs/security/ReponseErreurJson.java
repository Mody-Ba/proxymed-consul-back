package com.proxymed.configs.security;

import com.proxymed.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Ecrit une ErrorResponse depuis la chaine de filtres de securite, ou le
 * GlobalExceptionHandler n'intervient pas.
 */
@Component
@RequiredArgsConstructor
public class ReponseErreurJson {

    private final ObjectMapper objectMapper;

    public void ecrire(HttpServletResponse response, int status, String erreur, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(status, erreur, message));
    }
}
