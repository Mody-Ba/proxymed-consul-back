package com.proxymed.exception;

import java.util.UUID;

/**
 * La consultation initiale ne peut pas etre commencee a partir de cette demande (409) : la demande
 * n'est pas ACCEPTEE, ou sa consultation a deja ete commencee.
 */
public class ConsultationNonCommencableException extends RuntimeException {

    private ConsultationNonCommencableException(String message) {
        super(message);
    }

    public static ConsultationNonCommencableException nonAcceptee(UUID demandeId) {
        return new ConsultationNonCommencableException(
                "La demande de consultation " + demandeId + " n'est pas acceptee : seule une demande ACCEPTEE "
                        + "peut donner lieu a une consultation initiale");
    }

    public static ConsultationNonCommencableException dejaCommencee(UUID demandeId) {
        return new ConsultationNonCommencableException(
                "La consultation initiale de la demande " + demandeId + " a deja ete commencee");
    }
}
