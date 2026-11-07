package com.proxymed.mappers;

import com.proxymed.exception.ErrorResponse;
import com.proxymed.exception.GlobalExceptionHandler;
import com.proxymed.service.model.DecisionLotResponse;
import com.proxymed.service.model.FicheAViserModel;
import com.proxymed.service.model.FicheAViserResponse;
import com.proxymed.service.model.PageResponse;
import com.proxymed.service.model.ResultatDecisionModel;
import com.proxymed.service.model.ResultatDecisionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper API de la liste du senior et des visas / renvois en lot. Le code, l'erreur et le message
 * d'une fiche refusee viennent de GlobalExceptionHandler : ceux d'une requete sur cette seule fiche.
 */
@Component("apiVisaFicheSigneeMapper")
@RequiredArgsConstructor
public class VisaFicheSigneeMapper {

    private final GlobalExceptionHandler globalExceptionHandler;

    public PageResponse<FicheAViserResponse> toPageResponse(Page<FicheAViserModel> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    public FicheAViserResponse toResponse(FicheAViserModel model) {
        return FicheAViserResponse.builder()
                .ficheId(model.ficheId())
                .visiteId(model.visiteId())
                .patientId(model.patientId())
                .nomPatient(model.nomPatient())
                .numeroDossierProxymed(model.numeroDossierProxymed())
                .typeVisite(model.typeVisite())
                .dateConsultation(model.dateConsultation())
                .dateImport(model.dateImport())
                .statut(model.statut())
                .nomSeniorVisa(model.nomSeniorVisa())
                .dateVisa(model.dateVisa())
                .motifRenvoi(model.motifRenvoi())
                .build();
    }

    public DecisionLotResponse toResponse(List<ResultatDecisionModel> resultats) {
        List<ResultatDecisionResponse> lignes = resultats.stream().map(this::toResponse).toList();
        int reussies = (int) resultats.stream().filter(ResultatDecisionModel::reussi).count();
        return DecisionLotResponse.builder()
                .nombreFiches(lignes.size())
                .nombreReussies(reussies)
                .nombreErreurs(lignes.size() - reussies)
                .resultats(lignes)
                .build();
    }

    private ResultatDecisionResponse toResponse(ResultatDecisionModel resultat) {
        ResultatDecisionResponse.ResultatDecisionResponseBuilder ligne = ResultatDecisionResponse.builder()
                .index(resultat.index())
                .visiteId(resultat.visiteId());
        if (resultat.reussi()) {
            return ligne.resultat(resultat.statut().name())
                    .code(HttpStatus.OK.value())
                    .statut(resultat.statut())
                    .build();
        }
        ErrorResponse erreur = globalExceptionHandler.erreurDeDecision(resultat.erreur());
        return ligne.resultat(ResultatDecisionResponse.ERREUR)
                .code(erreur.status())
                .erreur(erreur.erreur())
                .message(erreur.message())
                .build();
    }
}
