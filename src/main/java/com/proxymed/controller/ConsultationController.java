package com.proxymed.controller;

import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.exception.ErrorResponse;
import com.proxymed.mappers.ConsultationMapper;
import com.proxymed.service.model.ConsultationResponse;
import com.proxymed.service.model.ConsultationSummaryResponse;
import com.proxymed.service.model.ConsultationUpdateRequest;
import com.proxymed.service.ConsultationService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * La creation directe d'une consultation est fermee : la visite (fiche) est creee a l'impression
     * par l'admin, puis importee et visee par un senior. Le corps eventuel de la requete est ignore.
     */
    static final String MESSAGE_CREATION_FERMEE = "La creation directe d'une consultation n'est plus possible : "
            + "la visite est creee par l'admin a l'impression de la fiche (POST /api/visites/impression)";

    /**
     * Recherche/liste du DMI (section 5.3/5.4). medecinJuniorAffecteId sert aussi
     * a lister les patients affectes a un medecin junior donne.
     */
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
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
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @GetMapping("/en-attente-dass")
    public List<ConsultationSummaryResponse> enAttenteValidationDass() {
        return consultationService.fichesEnAttenteValidationDass()
                .stream().map(consultationMapper::toSummaryResponse).toList();
    }

    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @GetMapping("/{id}")
    public ConsultationResponse findById(@PathVariable UUID id) {
        return consultationMapper.toResponse(consultationService.findById(id));
    }

    /**
     * PDF de la Fiche 1 (section 5.2), avec QR code.
     */
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @ApiResponse(responseCode = "200", description = "Fiche de consultation au format PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> telechargerPdf(@PathVariable UUID id) {
        Resource pdf = consultationService.genererPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("fiche-consultation-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @ApiResponse(responseCode = "409", description = "Creation directe fermee : une consultation part d'une visite "
            + "creee a l'impression par l'admin")
    @PostMapping
    public ResponseEntity<ErrorResponse> creer() {
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Creation de consultation fermee",
                        MESSAGE_CREATION_FERMEE));
    }

    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @PutMapping("/{id}")
    public ConsultationResponse mettreAJour(@PathVariable UUID id, @Valid @RequestBody ConsultationUpdateRequest request) {
        return consultationMapper.toResponse(consultationService.mettreAJour(id, consultationMapper.toModel(request)));
    }

    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @PostMapping("/{id}/valider")
    public ConsultationResponse valider(@PathVariable UUID id) {
        return consultationMapper.toResponse(consultationService.valider(id));
    }

    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @PostMapping("/{id}/signer")
    public ConsultationResponse signer(@PathVariable UUID id) {
        return consultationMapper.toResponse(consultationService.signer(id));
    }
}
