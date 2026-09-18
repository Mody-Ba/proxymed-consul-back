package com.proxymed.controller;

import com.proxymed.mappers.AntecedentMaladieMapper;
import com.proxymed.service.model.AntecedentMaladieRequest;
import com.proxymed.service.model.AntecedentMaladieResponse;
import com.proxymed.service.AntecedentMaladieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultations/{consultationId}/antecedents-maladies")
@RequiredArgsConstructor
public class AntecedentMaladieController {

    private final AntecedentMaladieService antecedentMaladieService;
    private final AntecedentMaladieMapper antecedentMaladieMapper;

    @GetMapping
    public List<AntecedentMaladieResponse> findAll(@PathVariable UUID consultationId) {
        return antecedentMaladieMapper.toResponseList(antecedentMaladieService.findByConsultation(consultationId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AntecedentMaladieResponse ajouter(@PathVariable UUID consultationId,
                                              @Valid @RequestBody AntecedentMaladieRequest request) {
        return antecedentMaladieMapper.toResponse(
                antecedentMaladieService.ajouter(consultationId, antecedentMaladieMapper.toModel(request)));
    }

    @DeleteMapping("/{antecedentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID consultationId, @PathVariable Long antecedentId) {
        antecedentMaladieService.supprimer(consultationId, antecedentId);
    }
}
