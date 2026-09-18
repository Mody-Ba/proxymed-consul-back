package com.proxymed.exception;

/**
 * Violation d'une regle de gestion du cahier des charges (section 5.1) :
 * champ conditionnellement obligatoire manquant, incoherence de decision, etc.
 */
public class RegleGestionException extends RuntimeException {

    public RegleGestionException(String message) {
        super(message);
    }
}
