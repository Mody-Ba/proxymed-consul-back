package com.proxymed.service;

import com.proxymed.enums.RoleMedecin;
import com.proxymed.service.model.MedecinModel;

import java.util.List;

public interface MedecinService {

    List<MedecinModel> findAll(RoleMedecin role);

    MedecinModel findById(Long id);

    MedecinModel create(MedecinModel model);

    MedecinModel update(Long id, MedecinModel model);

    void delete(Long id);
}
