package com.proxymed.exception;

/**
 * Un patient porte deja ce numero de dossier PROXYMED (unique en base) : 409 au lieu d'une
 * violation de contrainte non geree.
 */
public class PatientExistantException extends RuntimeException {

    public PatientExistantException() {
        super("Un patient existe deja avec ce numero de dossier PROXYMED");
    }
}
