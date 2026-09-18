package com.proxymed.controller;

import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.mappers.ConsultationMapper;
import com.proxymed.service.model.ConsultationCreateRequest;
import com.proxymed.service.model.ConsultationResponse;
import com.proxymed.service.model.ConsultationSummaryResponse;
import com.proxymed.service.model.ConsultationUpdateRequest;
import com.proxymed.service.ConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;
    private final ConsultationMapper consultationMapper;

    /**
     * Recherche/liste du DMI (section 5.3/5.4). medecinJuniorAffecteId sert aussi
     * a lister les patients affectes a un medecin junior donne.
     */
    @GetMapping
    public List<ConsultationSummaryResponse> rechercher(
            @RequestParam(required = false) String nomPatient,
            @RequestParam(required = false) String numeroDossierProxymed,
            @RequestParam(required = false) String numeroDmi,
            @RequestParam(required = false) Long medecinSeniorId,
            @RequestParam(required = false) Long medecinJuniorAffecteId,
            @RequestParam(required = false) StatutConsultation statut,
            @RequestParam(required = false) DecisionEligibilite decisionEligibilite,
            @RequestParam(required = false) Long structureId
    ) {
        return consultationService.rechercher(nomPatient, numeroDossierProxymed, numeroDmi,
                        medecinSeniorId, medecinJuniorAffecteId, statut, decisionEligibilite, structureId)
                .stream().map(consultationMapper::toSummaryResponse).toList();
    }

    /**
     * Fiches validees en attente de signature du point focal DASS (section 5.4).
     */
    @GetMapping("/en-attente-dass")
    public List<ConsultationSummaryResponse> enAttenteValidationDass() {
        return consultationService.fichesEnAttenteValidationDass()
                .stream().map(consultationMapper::toSummaryResponse).toList();
    }

    @GetMapping("/{id}")
    public ConsultationResponse findById(@PathVariable UUID id) {
        return consultationMapper.toResponse(consultationService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationResponse creer(@Valid @RequestBody ConsultationCreateRequest request) {
        return consultationMapper.toResponse(consultationService.creerBrouillon(consultationMapper.toModel(request)));
    }

    @PutMapping("/{id}")
    public ConsultationResponse mettreAJour(@PathVariable UUID id, @RequestBody ConsultationUpdateRequest request) {
        return consultationMapper.toResponse(consultationService.mettreAJour(id, consultationMapper.toModel(request)));
    }

    @PostMapping("/{id}/valider")
    public ConsultationResponse valider(@PathVariable UUID id) {
        return consultationMapper.toResponse(consultationService.valider(id));
    }

    @PostMapping("/{id}/signer")
    public ConsultationResponse signer(@PathVariable UUID id) {
        return consultationMapper.toResponse(consultationService.signer(id));
    }
}
