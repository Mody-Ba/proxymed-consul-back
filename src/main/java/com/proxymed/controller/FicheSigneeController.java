package com.proxymed.controller;

import com.proxymed.configs.security.UtilisateurConnecte;
import com.proxymed.mappers.FicheSigneeMapper;
import com.proxymed.service.DemandeConsultationService;
import com.proxymed.service.FicheSigneeService;
import com.proxymed.service.model.FicheSigneeResponse;
import com.proxymed.service.model.FicheSigneeModel;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FicheSigneeController {

    private final FicheSigneeService ficheSigneeService;
    private final FicheSigneeMapper ficheSigneeMapper;
    private final DemandeConsultationService demandeConsultationService;

    /**
     * Importe la fiche remplie, signee et scannee (PDF, 10 Mo au maximum), sans aucun id : le
     * patient et la consultation sont lus dans le QR code de la fiche. La reponse indique ou elle a ete rangee.
     * Un medecin junior ne peut importer que la fiche d'une consultation qui lui est assignee.
     */
    @ApiResponse(responseCode = "201", description = "Fiche rangee dans le dossier du patient")
    @ApiResponse(responseCode = "400", description = "PDF illisible ; QR non PROXYMED, d'ancien format ou d'un autre "
            + "type de fiche ; patient du QR different de celui de la consultation")
    @ApiResponse(responseCode = "403", description = "Consultation du QR assignee a un autre medecin junior")
    @ApiResponse(responseCode = "404", description = "Patient ou consultation du QR code introuvable")
    @ApiResponse(responseCode = "409", description = "La consultation a deja une fiche signee")
    @ApiResponse(responseCode = "413", description = "Fichier de plus de 10 Mo")
    @ApiResponse(responseCode = "415", description = "Fichier qui n'est pas un PDF")
    @ApiResponse(responseCode = "422", description = "Aucun code QR trouve dans le PDF")
    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @PostMapping(value = "/fiches-signees/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FicheSigneeResponse importer(@RequestPart("fichier") MultipartFile fichier,
                                        @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        UUID juniorAControlerId = utilisateur.accesLimiteAuxConsultationsAssignees() ? utilisateur.id() : null;
        return ficheSigneeMapper.toResponse(
                ficheSigneeService.importer(ficheSigneeMapper.toModel(fichier), juniorAControlerId));
    }

    /**
     * Fiches signees du dossier du patient, de la plus recemment importee a la plus ancienne.
     */
    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @GetMapping("/patients/{patientId}/fiches-signees")
    public List<FicheSigneeResponse> findByPatient(@PathVariable UUID patientId) {
        return ficheSigneeService.findByPatient(patientId).stream().map(ficheSigneeMapper::toResponse).toList();
    }

    /**
     * Un medecin junior n'obtient que la fiche d'une consultation qui lui est assignee (via la
     * demande liee) ; le senior, et toute consultation sans demande liee, gardent l'acces d'avant.
     */
    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @ApiResponse(responseCode = "200", description = "Fiche signee de la consultation, au format PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "403", description = "Consultation assignee a un autre medecin junior")
    @GetMapping(value = "/consultations/{consultationId}/fiche-signee", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> telecharger(@PathVariable UUID consultationId,
                                                @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        if (utilisateur.accesLimiteAuxConsultationsAssignees()) {
            demandeConsultationService.verifierJuniorAssigne(consultationId, utilisateur.id());
        }
        FicheSigneeModel fiche = ficheSigneeService.telecharger(consultationId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fiche.nomFichier()).build().toString())
                .body(new ByteArrayResource(fiche.contenu()));
    }
}
