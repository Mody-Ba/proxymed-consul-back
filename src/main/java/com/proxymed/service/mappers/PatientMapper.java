package com.proxymed.service.mappers;

import com.proxymed.entity.Patient;
import com.proxymed.service.model.PatientModel;
import org.springframework.stereotype.Component;

/**
 * Mapper DB : convertit uniquement entre PatientModel et l'entite JPA Patient.
 * Ne doit jamais connaitre PatientRequest/PatientResponse.
 */
@Component("dbPatientMapper")
public class PatientMapper {

    public Patient toEntity(PatientModel model) {
        return Patient.builder()
                .numeroDossierProxymed(model.numeroDossierProxymed())
                .numeroDmi(model.numeroDmi())
                .nomComplet(model.nomComplet())
                .dateNaissance(model.dateNaissance())
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

    /**
     * Applique le modele sur une entite managee existante (mutation en place,
     * necessaire pour que Hibernate suive les changements sur l'entite persistante).
     */
    public void applyToEntity(Patient entity, PatientModel model) {
        entity.setNumeroDossierProxymed(model.numeroDossierProxymed());
        entity.setNumeroDmi(model.numeroDmi());
        entity.setNomComplet(model.nomComplet());
        entity.setDateNaissance(model.dateNaissance());
        entity.setSexe(model.sexe());
        entity.setTelephone(model.telephone());
        entity.setAdresseDomicile(model.adresseDomicile());
        entity.setCommune(model.commune());
        entity.setQuartier(model.quartier());
        entity.setPersonneAContacterNom(model.personneAContacterNom());
        entity.setPersonneAContacterTelephone(model.personneAContacterTelephone());
        entity.setCouvertureSociale(model.couvertureSociale());
    }

    public PatientModel toModel(Patient entity) {
        return PatientModel.builder()
                .id(entity.getId())
                .numeroDossierProxymed(entity.getNumeroDossierProxymed())
                .numeroDmi(entity.getNumeroDmi())
                .nomComplet(entity.getNomComplet())
                .dateNaissance(entity.getDateNaissance())
                .age(entity.getAge())
                .sexe(entity.getSexe())
                .telephone(entity.getTelephone())
                .adresseDomicile(entity.getAdresseDomicile())
                .commune(entity.getCommune())
                .quartier(entity.getQuartier())
                .personneAContacterNom(entity.getPersonneAContacterNom())
                .personneAContacterTelephone(entity.getPersonneAContacterTelephone())
                .couvertureSociale(entity.getCouvertureSociale())
                .build();
    }
}
