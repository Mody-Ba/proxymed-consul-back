package com.proxymed.service;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.StatutConsultation;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtres de recherche du DMI (section 5.3/5.4) : nom, n° dossier, n° DMI,
 * medecin senior, medecin junior affecte, statut, decision d'eligibilite, structure.
 */
public final class ConsultationSpecifications {

    private ConsultationSpecifications() {
    }

    public static Specification<ConsultationInitiale> filtrer(
            String nomPatient,
            String numeroDossierProxymed,
            String numeroDmi,
            Long medecinSeniorId,
            Long medecinJuniorAffecteId,
            StatutConsultation statut,
            DecisionEligibilite decisionEligibilite,
            Long structureId
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            var predicate = cb.conjunction();

            if (nomPatient != null && !nomPatient.isBlank()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("patient").get("nomComplet")),
                        "%" + nomPatient.toLowerCase() + "%"));
            }
            if (numeroDossierProxymed != null && !numeroDossierProxymed.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("patient").get("numeroDossierProxymed"), numeroDossierProxymed));
            }
            if (numeroDmi != null && !numeroDmi.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("patient").get("numeroDmi"), numeroDmi));
            }
            if (medecinSeniorId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("medecinSenior").get("id"), medecinSeniorId));
            }
            if (medecinJuniorAffecteId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("medecinJuniorAffecte").get("id"), medecinJuniorAffecteId));
            }
            if (statut != null) {
                predicate = cb.and(predicate, cb.equal(root.get("statut"), statut));
            }
            if (decisionEligibilite != null) {
                predicate = cb.and(predicate, cb.equal(root.get("decisionEligibilite"), decisionEligibilite));
            }
            if (structureId != null) {
                Join<Object, Object> medecinSenior = root.join("medecinSenior", JoinType.INNER);
                predicate = cb.and(predicate, cb.equal(medecinSenior.get("structureRattachement").get("id"), structureId));
            }
            return predicate;
        };
    }

    public static Specification<ConsultationInitiale> enAttenteValidationDass() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("statut"), StatutConsultation.VALIDEE),
                cb.isFalse(root.get("signaturePointFocalDass"))
        );
    }
}
