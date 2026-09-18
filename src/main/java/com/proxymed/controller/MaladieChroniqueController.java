package com.proxymed.controller;

import com.proxymed.mappers.MaladieChroniqueMapper;
import com.proxymed.service.model.MaladieChroniqueResponse;
import com.proxymed.service.MaladieChroniqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/referentiels/maladies-chroniques")
@RequiredArgsConstructor
public class MaladieChroniqueController {

    private final MaladieChroniqueService maladieChroniqueService;
    private final MaladieChroniqueMapper maladieChroniqueMapper;

    @GetMapping
    public List<MaladieChroniqueResponse> findAll() {
        return maladieChroniqueMapper.toResponseList(maladieChroniqueService.findActives());
    }
}
