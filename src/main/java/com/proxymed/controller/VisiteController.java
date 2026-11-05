package com.proxymed.controller;

import com.proxymed.mappers.VisiteMapper;
import com.proxymed.service.ConsultationService;
import com.proxymed.service.ImpressionFichesService;
import com.proxymed.service.model.FichesImprimeesModel;
import com.proxymed.service.model.ImpressionFichesRequest;
import com.proxymed.service.model.VisiteViergeRequest;
import com.proxymed.service.model.VisiteViergeResponse;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Visites creees par l'admin au moment d'imprimer les fiches (nouveau parcours). L'admin ne lit
 * aucune donnee medicale : la reponse ne contient que des identifiants et le type de visite.
 */
@RestController
@RequestMapping("/api/visites")
public class VisiteController {

    /** Ids des visites creees, separes par des virgules, dans l'ordre des fiches du PDF (expose par le CORS). */
    public static final String EN_TETE_VISITES = "X-Visites-Ids";

    private final ConsultationService consultationService;
    private final ImpressionFichesService impressionFichesService;
    private final VisiteMapper visiteMapper;
    private final ZoneId fuseauHoraire;

    public VisiteController(ConsultationService consultationService,
                            ImpressionFichesService impressionFichesService,
                            VisiteMapper visiteMapper,
                            @Value("${proxymed.pdf.fuseau-horaire:Africa/Dakar}") ZoneId fuseauHoraire) {
        this.consultationService = consultationService;
        this.impressionFichesService = impressionFichesService;
        this.visiteMapper = visiteMapper;
        this.fuseauHoraire = fuseauHoraire;
    }

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

    /**
     * Impression de fiches vierges : une visite vierge creee par demande, et un seul PDF combine en
     * retour (une fiche par demande, chacune sur une nouvelle page, dans l'ordre de la liste). Les
     * ids des visites creees sont dans l'en-tete X-Visites-Ids, dans le meme ordre. Tout ou rien.
     */
    @ApiResponse(responseCode = "200", description = "PDF combine des fiches vierges",
            headers = @Header(name = EN_TETE_VISITES, description = "Ids des visites creees, dans l'ordre des fiches"),
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = "Liste vide, de plus de " + ImpressionFichesRequest.MAX_FICHES
            + " fiches, demande incomplete ou en double, type de visite inconnu")
    @ApiResponse(responseCode = "403", description = "Reserve a l'admin")
    @ApiResponse(responseCode = "404", description = "Un patient est introuvable (aucune visite n'est creee)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/impression")
    public ResponseEntity<Resource> imprimer(@Valid @RequestBody ImpressionFichesRequest request) {
        FichesImprimeesModel resultat = impressionFichesService.imprimer(visiteMapper.toDemandes(request));
        String nomFichier = "fiches-" + LocalDate.now(fuseauHoraire) + "-" + resultat.visiteIds().size() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nomFichier).build().toString())
                .header(EN_TETE_VISITES, resultat.visiteIds().stream().map(UUID::toString).collect(Collectors.joining(",")))
                .body(resultat.pdf());
    }
}
