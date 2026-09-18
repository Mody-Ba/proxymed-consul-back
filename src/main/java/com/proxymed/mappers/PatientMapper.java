package com.proxymed.mappers;

import com.proxymed.service.model.PatientModel;
import com.proxymed.service.model.PatientRequest;
import com.proxymed.service.model.PatientResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

/**
 * Mapper API : convertit uniquement PatientRequest <-> PatientModel et PatientModel -> PatientResponse.
 * Ne doit jamais connaitre l'entite JPA Patient.
 */
@Component("apiPatientMapper")
public class PatientMapper {

    public PatientModel toModel(PatientRequest req) {
        return PatientModel.builder()
                .numeroDossierProxymed(req.numeroDossierProxymed())
                .numeroDmi(req.numeroDmi())
                .nomComplet(req.nomComplet())
                .dateNaissance(req.dateNaissance())
                .age(calculerAge(req.dateNaissance()))
                .sexe(req.sexe())
                .telephone(req.telephone())
                .adresseDomicile(req.adresseDomicile())
                .commune(req.commune())
                .quartier(req.quartier())
                .personneAContacterNom(req.personneAContacterNom())
                .personneAContacterTelephone(req.personneAContacterTelephone())
                .couvertureSociale(req.couvertureSociale())
                .build();
    }

    public PatientResponse toResponse(PatientModel model) {
        return PatientResponse.builder()
                .id(model.id())
                .numeroDossierProxymed(model.numeroDossierProxymed())
                .numeroDmi(model.numeroDmi())
                .nomComplet(model.nomComplet())
                .dateNaissance(model.dateNaissance())
                .age(model.age())
                .sexe(model.sexe())
                .telephone(model.telephone())
                .adresseDomicile(model.adresseDomicile())
                .commune(model.commune())
                .quartier(model.quartier())
                .personneAContacterNom(model.personneAContacterNom())
                .personneAContacterTelephone(model.personneAContacterTelephone())
                .couvertureSociale(model.couvertureSociale())
                .build();
    }

    private Integer calculerAge(LocalDate dateNaissance) {
        return dateNaissance != null ? Period.between(dateNaissance, LocalDate.now()).getYears() : null;
    }
}
