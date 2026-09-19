package com.proxymed.service;

import com.proxymed.service.model.MaladieChroniqueModel;

import java.util.List;

public interface MaladieChroniqueService {

    List<MaladieChroniqueModel> findActives();

    MaladieChroniqueModel findById(Long id);
}
