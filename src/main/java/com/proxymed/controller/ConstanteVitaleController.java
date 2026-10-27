package com.proxymed.controller;

import com.proxymed.mappers.ConstanteVitaleMapper;
import com.proxymed.service.model.ConstanteVitaleRequest;
import com.proxymed.service.model.ConstanteVitaleResponse;
import com.proxymed.service.ConstanteVitaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/consultations/{consultationId}/constantes")
@RequiredArgsConstructor
public class ConstanteVitaleController {

    private final ConstanteVitaleService constanteVitaleService;
    private final ConstanteVitaleMapper constanteVitaleMapper;

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConstanteVitaleResponse ajouter(@PathVariable UUID consultationId,
                                            @Valid @RequestBody ConstanteVitaleRequest request) {
        return constanteVitaleMapper.toResponse(
                constanteVitaleService.ajouter(consultationId, constanteVitaleMapper.toModel(request)));
    }

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @PutMapping("/{constanteId}")
    public ConstanteVitaleResponse modifier(@PathVariable UUID consultationId, @PathVariable Long constanteId,
                                             @Valid @RequestBody ConstanteVitaleRequest request) {
        return constanteVitaleMapper.toResponse(
                constanteVitaleService.modifier(consultationId, constanteId, constanteVitaleMapper.toModel(request)));
    }

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @DeleteMapping("/{constanteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID consultationId, @PathVariable Long constanteId) {
        constanteVitaleService.supprimer(consultationId, constanteId);
    }
}
