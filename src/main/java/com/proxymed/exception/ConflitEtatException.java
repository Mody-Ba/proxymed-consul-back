package com.proxymed.exception;

/**
 * Action incompatible avec le statut courant de la fiche
 * (ex. modifier une fiche SIGNEE, signer une fiche non VALIDEE).
 */
public class ConflitEtatException extends RuntimeException {

    public ConflitEtatException(String message) {
        super(message);
    }
}
