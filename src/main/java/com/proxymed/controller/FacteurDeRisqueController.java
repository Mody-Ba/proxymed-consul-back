package com.proxymed.controller;

import com.proxymed.mappers.FacteurDeRisqueMapper;
import com.proxymed.service.model.FacteurDeRisqueResponse;
import com.proxymed.service.FacteurDeRisqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/referentiels/facteurs-risque")
@RequiredArgsConstructor
public class FacteurDeRisqueController {

    private final FacteurDeRisqueService facteurDeRisqueService;
    private final FacteurDeRisqueMapper facteurDeRisqueMapper;

    @GetMapping
    public List<FacteurDeRisqueResponse> findAll() {
        return facteurDeRisqueMapper.toResponseList(facteurDeRisqueService.findActifs());
    }
}
