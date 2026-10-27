package com.proxymed.exception;

/**
 * Le PDF importe est valide mais aucun QR code n'y a ete trouve : le systeme ne peut pas savoir
 * a quelle consultation le rattacher (ex. page mal scannee, QR rogne ou illisible).
 */
public class QrCodeIntrouvableException extends RuntimeException {

    public QrCodeIntrouvableException(String message) {
        super(message);
    }
}
