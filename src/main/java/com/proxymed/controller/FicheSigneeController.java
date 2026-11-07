package com.proxymed.controller;

import com.proxymed.mappers.FicheSigneeMapper;
import com.proxymed.mappers.ImportLotMapper;
import com.proxymed.service.FicheSigneeService;
import com.proxymed.service.ImportFichesLotService;
import com.proxymed.service.model.ImportLotResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
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
    private final ImportFichesLotService importFichesLotService;
    private final ImportLotMapper importLotMapper;

    /**
     * Import en lot par l'admin : un ou plusieurs PDF scannes (partie "fichiers", repetee), au plus
     * 20 par envoi, 10 Mo par fichier. Chaque fichier est range d'apres son QR code dans la visite
     * de son patient, statut EN_ATTENTE (une fiche RENVOYEE est remplacee), dans sa propre
     * transaction : un fichier refuse n'empeche pas les autres. Toujours 200 si l'envoi lui-meme est
     * valide ; le detail est dans le resultat de chaque fichier (code 201 ou code de l'erreur).
     * L'admin ne recoit que des
     * identifiants, noms de fichier et codes, jamais le contenu d'une fiche.
     */
    @ApiResponse(responseCode = "200", description = "Un resultat par fichier (IMPORTEE ou ERREUR avec son code : "
            + "400 QR non PROXYMED ou PDF illisible, 404 visite ou patient introuvable, 409 fiche deja importee, "
            + "413 fichier de plus de 10 Mo, 415 fichier non PDF, 422 aucun QR code)")
    @ApiResponse(responseCode = "400", description = "Aucun fichier, ou plus de " + ImportFichesLotService.MAX_FICHIERS
            + " fichiers (rien n'est importe)")
    @ApiResponse(responseCode = "403", description = "Reserve a l'admin")
    @ApiResponse(responseCode = "413", description = "Envoi de plus de 100 Mo au total (rien n'est importe)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/fiches-signees/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportLotResponse importer(@RequestPart("fichiers") List<MultipartFile> fichiers) {
        return importLotMapper.toResponse(importFichesLotService.importer(importLotMapper.toModels(fichiers)));
    }

    /**
     * Fiches signees du dossier du patient, de la plus recemment importee a la plus ancienne.
     */
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @GetMapping("/patients/{patientId}/fiches-signees")
    public List<FicheSigneeResponse> findByPatient(@PathVariable UUID patientId) {
        return ficheSigneeService.findByPatient(patientId).stream().map(ficheSigneeMapper::toResponse).toList();
    }

    /**
     * Scan signe de la consultation (medecin senior uniquement).
     */
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @ApiResponse(responseCode = "200", description = "Fiche signee de la consultation, au format PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/consultations/{consultationId}/fiche-signee", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> telecharger(@PathVariable UUID consultationId) {
        FicheSigneeModel fiche = ficheSigneeService.telecharger(consultationId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fiche.nomFichier()).build().toString())
                .body(new ByteArrayResource(fiche.contenu()));
    }
}
