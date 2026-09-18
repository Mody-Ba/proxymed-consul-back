package com.proxymed.service.model;

public record ExamenParAppareilRequest(
        String etatGeneral,
        String cardioVasculaire,
        String respiratoire,
        String digestif,
        String neurologique,
        String locomoteurCutaneAutre
) {
}
