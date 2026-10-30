package com.proxymed.exception;

/**
 * Le fichier importe n'est pas un PDF (415) : type de contenu declare autre que application/pdf,
 * ou contenu qui ne commence pas par la signature d'un PDF (fichier vide compris).
 */
public class FichierNonPdfException extends RuntimeException {

    public FichierNonPdfException() {
        super("Le fichier importe doit etre un PDF");
    }
}
