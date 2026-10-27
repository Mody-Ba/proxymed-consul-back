package com.proxymed.exception;

/**
 * La consultation designee par le QR code a deja une fiche signee : une seule fiche par consultation.
 */
public class FicheSigneeExistanteException extends RuntimeException {

    public FicheSigneeExistanteException(String message) {
        super(message);
    }
}
