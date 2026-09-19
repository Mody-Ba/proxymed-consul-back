package com.proxymed.service;

import com.proxymed.service.model.SituationSocialeModel;

import java.util.List;

public interface SituationSocialeService {

    List<SituationSocialeModel> findActives();

    SituationSocialeModel findById(Long id);
}
