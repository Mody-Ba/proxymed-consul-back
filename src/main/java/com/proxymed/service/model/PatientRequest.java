package com.proxymed.service.model;

import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.Sexe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank(message = "le numero de dossier PROXYMED est obligatoire")
        String numeroDossierProxymed,

        String numeroDmi,

        @NotBlank(message = "le nom complet est obligatoire")
        String nomComplet,

        @NotNull(message = "la date de naissance est obligatoire")
        @Past(message = "la date de naissance doit etre dans le passe")
        LocalDate dateNaissance,

        @NotNull(message = "le sexe est obligatoire")
        Sexe sexe,

        String telephone,
        String adresseDomicile,
        String commune,
        String quartier,
        String personneAContacterNom,
        String personneAContacterTelephone,
        CouvertureSociale couvertureSociale
) {
}
