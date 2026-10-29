package com.proxymed.service.model;

import com.proxymed.enums.Sexe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Ce que la demande ne fournit pas et que le medecin junior saisit en commencant la consultation :
 * les champs obligatoires du patient et le medecin senior responsable de la Fiche 1.
 * Le nom du patient et le motif viennent de la demande ; le junior vient du badge.
 */
public record CommencementConsultationRequest(
        @NotBlank(message = "le numero de dossier PROXYMED est obligatoire")
        @Size(max = 100, message = "le numero de dossier PROXYMED ne doit pas depasser 100 caracteres")
        String numeroDossierProxymed,

        @NotNull(message = "la date de naissance est obligatoire")
        @Past(message = "la date de naissance doit etre dans le passe")
        LocalDate dateNaissance,

        @NotNull(message = "le sexe est obligatoire")
        Sexe sexe,

        @NotNull(message = "le medecin senior est obligatoire")
        Long medecinSeniorId
) {
}
