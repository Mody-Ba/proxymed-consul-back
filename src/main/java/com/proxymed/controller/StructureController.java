package com.proxymed.controller;

import com.proxymed.mappers.StructureMapper;
import com.proxymed.service.model.StructureResponse;
import com.proxymed.service.StructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/referentiels/structures")
@RequiredArgsConstructor
public class StructureController {

    private final StructureService structureService;
    private final StructureMapper structureMapper;

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @GetMapping
    public List<StructureResponse> findAll() {
        return structureMapper.toResponseList(structureService.findActives());
    }
}
