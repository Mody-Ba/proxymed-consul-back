package com.proxymed.exception;

/**
 * Le fichier importe depasse 10 Mo (413). Levee par le service, ou reprise par le
 * GlobalExceptionHandler quand le serveur refuse la requete avant le controller
 * (spring.servlet.multipart.max-file-size / max-request-size).
 */
public class FichierTropVolumineuxException extends RuntimeException {

    public FichierTropVolumineuxException() {
        super("Le fichier importe depasse la taille maximale de 10 Mo");
    }
}
