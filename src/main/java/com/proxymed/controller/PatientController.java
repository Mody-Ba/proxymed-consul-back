package com.proxymed.controller;

import com.proxymed.mappers.PatientMapper;
import com.proxymed.service.model.PatientRequest;
import com.proxymed.service.model.PatientResponse;
import com.proxymed.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final PatientMapper patientMapper;

    @GetMapping
    public List<PatientResponse> findAll() {
        return patientService.findAll().stream().map(patientMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PatientResponse findById(@PathVariable UUID id) {
        return patientMapper.toResponse(patientService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(@Valid @RequestBody PatientRequest request) {
        return patientMapper.toResponse(patientService.create(patientMapper.toModel(request)));
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable UUID id, @Valid @RequestBody PatientRequest request) {
        return patientMapper.toResponse(patientService.update(id, patientMapper.toModel(request)));
    }
}
