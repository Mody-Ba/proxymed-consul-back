package com.proxymed.service.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record ExamenParAppareilResponse(
        Long id,
        String etatGeneral,
        String cardioVasculaire,
        String respiratoire,
        String digestif,
        String neurologique,
        String locomoteurCutaneAutre
) {
}
