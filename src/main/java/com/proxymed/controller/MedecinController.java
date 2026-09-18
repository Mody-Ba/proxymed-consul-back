package com.proxymed.controller;

import com.proxymed.enums.RoleMedecin;
import com.proxymed.mappers.MedecinMapper;
import com.proxymed.service.model.MedecinRequest;
import com.proxymed.service.model.MedecinResponse;
import com.proxymed.service.MedecinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medecins")
@RequiredArgsConstructor
public class MedecinController {

    private final MedecinService medecinService;
    private final MedecinMapper medecinMapper;

    @GetMapping
    public List<MedecinResponse> findAll(@RequestParam(required = false) RoleMedecin role) {
        return medecinService.findAll(role).stream().map(medecinMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MedecinResponse findById(@PathVariable Long id) {
        return medecinMapper.toResponse(medecinService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedecinResponse create(@Valid @RequestBody MedecinRequest request) {
        return medecinMapper.toResponse(medecinService.create(medecinMapper.toModel(request)));
    }
}
