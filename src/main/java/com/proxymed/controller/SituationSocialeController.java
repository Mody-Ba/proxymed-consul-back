package com.proxymed.controller;

import com.proxymed.mappers.SituationSocialeMapper;
import com.proxymed.service.model.SituationSocialeResponse;
import com.proxymed.service.SituationSocialeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/referentiels/situations-sociales")
@RequiredArgsConstructor
public class SituationSocialeController {

    private final SituationSocialeService situationSocialeService;
    private final SituationSocialeMapper situationSocialeMapper;

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @GetMapping
    public List<SituationSocialeResponse> findAll() {
        return situationSocialeMapper.toResponseList(situationSocialeService.findActives());
    }
}
