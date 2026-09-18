package com.proxymed.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant horodatage,
        int status,
        String erreur,
        String message,
        List<String> details
) {
    public ErrorResponse(int status, String erreur, String message) {
        this(Instant.now(), status, erreur, message, List.of());
    }

    public ErrorResponse(int status, String erreur, String message, List<String> details) {
        this(Instant.now(), status, erreur, message, details);
    }
}
