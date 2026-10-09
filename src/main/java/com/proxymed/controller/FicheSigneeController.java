package com.proxymed.controller;

import com.proxymed.mappers.FicheSigneeMapper;
import com.proxymed.service.FicheSigneeService;
import com.proxymed.service.model.FicheSigneeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
}
