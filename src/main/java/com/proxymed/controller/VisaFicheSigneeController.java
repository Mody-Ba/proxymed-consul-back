package com.proxymed.controller;

import com.proxymed.configs.security.UtilisateurConnecte;
import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.mappers.VisaFicheSigneeMapper;
import com.proxymed.service.RapportFicheService;
import com.proxymed.service.VisaFicheSigneeService;
import com.proxymed.service.VisaFichesLotService;
import com.proxymed.service.model.DecisionLotResponse;
import com.proxymed.service.model.FicheAViserResponse;
import com.proxymed.service.model.PageResponse;
import com.proxymed.service.model.RenvoiLotRequest;
import com.proxymed.service.model.VisaLotRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Liste, visa et renvoi des fiches scannees, reserves au MEDECIN_SENIOR (ni l'admin ni le junior).
 * Pour l'instant tout senior voit et peut viser toutes les fiches. Le scan d'une fiche se lit par
 * GET /api/consultations/{visiteId}/fiche-signee. Le senior qui vise (identifiant et nom) vient
 * toujours du badge, jamais du corps de la requete ; faute de claim "nom" dans le badge, le nom
 * enregistre avec le visa est l'adresse e-mail du badge.
 */
@RestController
@RequestMapping("/api/fiches-signees")
@RequiredArgsConstructor
public class VisaFicheSigneeController {

    private static final String PAGE_DEFAUT = "0";
    private static final String TAILLE_DEFAUT = "20";

    private final VisaFicheSigneeService visaFicheSigneeService;
    private final VisaFichesLotService visaFichesLotService;
    private final VisaFicheSigneeMapper visaFicheSigneeMapper;
    private final RapportFicheService rapportFicheService;

    /**
     * Fiches d'un statut (EN_ATTENTE par defaut), de la plus anciennement importee a la plus recente.
     */
    @ApiResponse(responseCode = "400", description = "Statut inconnu, page negative ou taille hors [1, 100]")
    @ApiResponse(responseCode = "403", description = "Reserve au medecin senior")
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @GetMapping
    public PageResponse<FicheAViserResponse> findByStatut(
            @Parameter(description = "EN_ATTENTE (par defaut), VISEE ou RENVOYEE")
            @RequestParam(required = false) StatutFicheSignee statut,
            @Parameter(description = "Numero de page, a partir de 0") @RequestParam(defaultValue = PAGE_DEFAUT) int page,
            @Parameter(description = "Taille de page, de 1 a " + VisaFicheSigneeService.TAILLE_PAGE_MAX)
            @RequestParam(defaultValue = TAILLE_DEFAUT) int taille) {
        return visaFicheSigneeMapper.toPageResponse(visaFicheSigneeService.findByStatut(statut, page, taille));
    }

    /**
     * Visa d'une ou plusieurs fiches EN_ATTENTE, une transaction par fiche : une fiche refusee
     * n'empeche pas les autres. Toujours 200 si la requete elle-meme est valide ; le detail est dans
     * le resultat de chaque fiche.
     */
    @ApiResponse(responseCode = "200", description = "Un resultat par visite (VISEE, ou ERREUR avec son code : "
            + "404 visite ou fiche introuvable, 409 fiche qui n'est pas EN_ATTENTE ou visee au meme moment par un autre senior)")
    @ApiResponse(responseCode = "400", description = "Liste vide, de plus de " + VisaFichesLotService.MAX_FICHES
            + " visites ou avec des doublons (rien n'est vise)")
    @ApiResponse(responseCode = "403", description = "Reserve au medecin senior")
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @PostMapping("/visa")
    public DecisionLotResponse viser(@Valid @RequestBody VisaLotRequest request,
                                     @AuthenticationPrincipal UtilisateurConnecte senior) {
        return visaFicheSigneeMapper.toResponse(
                visaFichesLotService.viser(request.visiteIds(), senior.id(), senior.email()));
    }

    /**
     * Renvoi d'une ou plusieurs fiches EN_ATTENTE avec un motif obligatoire (le meme pour toutes),
     * une transaction par fiche. L'admin pourra reimporter une fiche RENVOYEE.
     */
    @ApiResponse(responseCode = "200", description = "Un resultat par visite (RENVOYEE, ou ERREUR avec son code : "
            + "404 visite ou fiche introuvable, 409 fiche qui n'est pas EN_ATTENTE)")
    @ApiResponse(responseCode = "400", description = "Motif absent, vide ou de plus de " + VisaFichesLotService.MOTIF_MAX
            + " caracteres ; liste vide, de plus de " + VisaFichesLotService.MAX_FICHES
            + " visites ou avec des doublons (rien n'est renvoye)")
    @ApiResponse(responseCode = "403", description = "Reserve au medecin senior")
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @PostMapping("/renvoi")
    public DecisionLotResponse renvoyer(@Valid @RequestBody RenvoiLotRequest request) {
        return visaFicheSigneeMapper.toResponse(visaFichesLotService.renvoyer(request.visiteIds(), request.motif()));
    }

    /**
     * Rapport PDF d'une fiche VISEE : page de garde puis scan signe, regenere a chaque appel. Tout
     * senior peut le telecharger (regle par defaut, a confirmer). Le nom du fichier ne contient que
     * l'identifiant de la visite, aucune donnee du patient. Pas de "produces" sur l'adresse : les
     * erreurs (404, 409) restent ainsi rendues en JSON, au format habituel.
     */
    @ApiResponse(responseCode = "200", description = "Rapport au format PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "403", description = "Reserve au medecin senior")
    @ApiResponse(responseCode = "404", description = "Visite introuvable, ou aucune fiche importee")
    @ApiResponse(responseCode = "409", description = "Fiche qui n'est pas VISEE")
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @GetMapping("/{consultationId}/rapport")
    public ResponseEntity<Resource> rapport(@PathVariable UUID consultationId) {
        Resource pdf = rapportFicheService.generer(consultationId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("rapport-" + consultationId + ".pdf").build().toString())
                .body(pdf);
    }
}
