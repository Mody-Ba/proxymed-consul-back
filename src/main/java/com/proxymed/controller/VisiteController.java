package com.proxymed.controller;

import com.proxymed.mappers.VisiteMapper;
import com.proxymed.service.ConsultationService;
import com.proxymed.service.model.VisiteViergeRequest;
import com.proxymed.service.model.VisiteViergeResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Visites creees par l'admin au moment d'imprimer les fiches (nouveau parcours). L'admin ne lit
 * aucune donnee medicale : la reponse ne contient que des identifiants et le type de visite.
 */
@RestController
@RequestMapping("/api/visites")
@RequiredArgsConstructor
public class VisiteController {

    private final ConsultationService consultationService;
    private final VisiteMapper visiteMapper;

    /**
     * Visite vierge (BROUILLON, sans senior ni junior) pour un patient existant. Son id est celui
     * de la consultation : c'est lui que porte le QR code de la fiche imprimee.
     */
    @ApiResponse(responseCode = "201", description = "Visite vierge creee")
    @ApiResponse(responseCode = "400", description = "Patient ou type de visite manquant, type de visite inconnu")
    @ApiResponse(responseCode = "403", description = "Reserve a l'admin")
    @ApiResponse(responseCode = "404", description = "Patient introuvable")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisiteViergeResponse creer(@Valid @RequestBody VisiteViergeRequest request) {
        return visiteMapper.toResponse(consultationService.creerVisiteVierge(request.patientId(), request.typeVisite()));
    }
}
