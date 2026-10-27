package com.proxymed.controller;

import com.proxymed.configs.security.UtilisateurConnecte;
import com.proxymed.enums.StatutDemandeConsultation;
import com.proxymed.mappers.DemandeConsultationMapper;
import com.proxymed.service.DemandeConsultationService;
import com.proxymed.service.model.AcceptationDemandeRequest;
import com.proxymed.service.model.DemandeConsultationRequest;
import com.proxymed.service.model.DemandeConsultationResponse;
import com.proxymed.service.model.PageResponse;
import com.proxymed.service.model.RefusDemandeRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Demandes de consultation initiale (regles PROVISOIRES) : un UTILISATEUR (patient ou proche) cree
 * une demande, un MEDECIN_SENIOR l'accepte en l'assignant a un MEDECIN_JUNIOR, ou la refuse.
 * Seul controller de la Fiche 1 ouvert au role UTILISATEUR. L'identite de l'appelant vient
 * toujours du badge (UtilisateurConnecte), jamais du corps de la requete.
 */
@RestController
@RequestMapping("/api/demandes-consultation")
@RequiredArgsConstructor
public class DemandeConsultationController {

    private static final String PAGE_DEFAUT = "0";
    private static final String TAILLE_DEFAUT = "20";

    private final DemandeConsultationService demandeConsultationService;
    private final DemandeConsultationMapper demandeConsultationMapper;

    @ApiResponse(responseCode = "201", description = "Demande creee, EN_ATTENTE")
    @ApiResponse(responseCode = "400", description = "Champ manquant, trop long, inconnu ou JSON invalide")
    @PreAuthorize("hasRole('UTILISATEUR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeConsultationResponse creer(@Valid @RequestBody DemandeConsultationRequest request,
                                             @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return demandeConsultationMapper.toResponse(
                demandeConsultationService.creer(demandeConsultationMapper.toModel(request, utilisateur.id())));
    }

    /**
     * Demandes creees par l'utilisateur connecte uniquement, de la plus recente a la plus ancienne.
     */
    @PreAuthorize("hasRole('UTILISATEUR')")
    @GetMapping("/mes-demandes")
    public PageResponse<DemandeConsultationResponse> mesDemandes(
            @AuthenticationPrincipal UtilisateurConnecte utilisateur,
            @Parameter(description = "Numero de page, a partir de 0") @RequestParam(defaultValue = PAGE_DEFAUT) int page,
            @Parameter(description = "Taille de page, de 1 a 100") @RequestParam(defaultValue = TAILLE_DEFAUT) int taille) {
        return demandeConsultationMapper.toPageResponse(
                demandeConsultationService.findByDemandeur(utilisateur.id(), page, taille));
    }

    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @GetMapping
    public PageResponse<DemandeConsultationResponse> findAll(
            @RequestParam(required = false) StatutDemandeConsultation statut,
            @Parameter(description = "Numero de page, a partir de 0") @RequestParam(defaultValue = PAGE_DEFAUT) int page,
            @Parameter(description = "Taille de page, de 1 a 100") @RequestParam(defaultValue = TAILLE_DEFAUT) int taille) {
        return demandeConsultationMapper.toPageResponse(demandeConsultationService.findAll(statut, page, taille));
    }

    /**
     * Demandes ACCEPTEES assignees au medecin junior connecte.
     */
    @PreAuthorize("hasRole('MEDECIN_JUNIOR')")
    @GetMapping("/assignees-a-moi")
    public PageResponse<DemandeConsultationResponse> assigneesAMoi(
            @AuthenticationPrincipal UtilisateurConnecte utilisateur,
            @Parameter(description = "Numero de page, a partir de 0") @RequestParam(defaultValue = PAGE_DEFAUT) int page,
            @Parameter(description = "Taille de page, de 1 a 100") @RequestParam(defaultValue = TAILLE_DEFAUT) int taille) {
        return demandeConsultationMapper.toPageResponse(
                demandeConsultationService.findAssigneesAuJunior(utilisateur.id(), page, taille));
    }

    /**
     * Le role est verifie avant la lecture (@PreAuthorize), le lien avec la demande apres
     * (@PostAuthorize) : le senior voit tout, le junior seulement ce qui lui est assigne,
     * l'utilisateur seulement ce qu'il a cree. Une demande absente donne 404 avant ce controle.
     */
    @ApiResponse(responseCode = "403", description = "Demande ni creee par l'utilisateur ni assignee au junior")
    @ApiResponse(responseCode = "404", description = "Demande introuvable")
    @PreAuthorize("hasAnyRole('UTILISATEUR', 'MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @PostAuthorize("hasRole('MEDECIN_SENIOR')"
            + " or (hasRole('MEDECIN_JUNIOR') and returnObject.juniorId() == principal.id())"
            + " or (hasRole('UTILISATEUR') and returnObject.demandeurId() == principal.id())")
    @GetMapping("/{id}")
    public DemandeConsultationResponse findById(@PathVariable UUID id) {
        return demandeConsultationMapper.toResponse(demandeConsultationService.findById(id));
    }

    @ApiResponse(responseCode = "404", description = "Demande introuvable")
    @ApiResponse(responseCode = "409", description = "La demande n'est plus EN_ATTENTE")
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @PatchMapping("/{id}/accepter")
    public DemandeConsultationResponse accepter(@PathVariable UUID id,
                                                @Valid @RequestBody AcceptationDemandeRequest request,
                                                @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return demandeConsultationMapper.toResponse(
                demandeConsultationService.accepter(id, utilisateur.id(), request.juniorId()));
    }

    @ApiResponse(responseCode = "404", description = "Demande introuvable")
    @ApiResponse(responseCode = "409", description = "La demande n'est plus EN_ATTENTE")
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @PatchMapping("/{id}/refuser")
    public DemandeConsultationResponse refuser(@PathVariable UUID id,
                                               @Valid @RequestBody RefusDemandeRequest request,
                                               @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return demandeConsultationMapper.toResponse(
                demandeConsultationService.refuser(id, utilisateur.id(), request.motifRefus()));
    }
}
