package com.proxymed.exception;

/**
 * La demande de consultation n'est plus EN_ATTENTE : elle a deja ete acceptee ou refusee.
 */
public class DemandeDejaTraiteeException extends RuntimeException {

    public DemandeDejaTraiteeException(String message) {
        super(message);
    }
}
