package com.proxymed.service;

import com.proxymed.service.model.FacteurDeRisqueModel;

import java.util.List;

public interface FacteurDeRisqueService {

    List<FacteurDeRisqueModel> findActifs();

    FacteurDeRisqueModel findById(Long id);
}
