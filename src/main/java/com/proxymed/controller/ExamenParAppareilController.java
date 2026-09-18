package com.proxymed.controller;

import com.proxymed.mappers.ExamenParAppareilMapper;
import com.proxymed.service.model.ExamenParAppareilRequest;
import com.proxymed.service.model.ExamenParAppareilResponse;
import com.proxymed.service.ExamenParAppareilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/consultations/{consultationId}/examen-par-appareil")
@RequiredArgsConstructor
public class ExamenParAppareilController {

    private final ExamenParAppareilService examenParAppareilService;
    private final ExamenParAppareilMapper examenParAppareilMapper;

    @GetMapping
    public ExamenParAppareilResponse findOne(@PathVariable UUID consultationId) {
        return examenParAppareilMapper.toResponse(examenParAppareilService.findByConsultation(consultationId));
    }

    @PutMapping
    public ExamenParAppareilResponse creerOuMettreAJour(@PathVariable UUID consultationId,
                                                         @Valid @RequestBody ExamenParAppareilRequest request) {
        return examenParAppareilMapper.toResponse(
                examenParAppareilService.creerOuMettreAJour(consultationId, examenParAppareilMapper.toModel(request)));
    }
}
