package com.proxymed.controller;

import com.proxymed.mappers.FicheSigneeMapper;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/consultations/{consultationId}/fiche-signee")
@RequiredArgsConstructor
public class FicheSigneeController {

    private final FicheSigneeService ficheSigneeService;
    private final FicheSigneeMapper ficheSigneeMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FicheSigneeResponse importer(@PathVariable UUID consultationId,
                                        @RequestPart("fichier") MultipartFile fichier) {
        return ficheSigneeMapper.toResponse(
                ficheSigneeService.importer(consultationId, ficheSigneeMapper.toModel(fichier)));
    }

    @ApiResponse(responseCode = "200", description = "Fiche signee importee la plus recente, au format PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> telecharger(@PathVariable UUID consultationId) {
        FicheSigneeModel fiche = ficheSigneeService.telecharger(consultationId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fiche.nomFichier()).build().toString())
                .body(new ByteArrayResource(fiche.contenu()));
    }
}
