package com.proxymed.service;

import com.proxymed.service.model.AntecedentMaladieModel;

import java.util.List;
import java.util.UUID;

/**
 * Gere les antecedents (maladies chroniques cochees) d'une consultation (section 3).
 */
public interface AntecedentMaladieService {

    List<AntecedentMaladieModel> findByConsultation(UUID consultationId);

    AntecedentMaladieModel ajouter(UUID consultationId, AntecedentMaladieModel model);

    void supprimer(UUID consultationId, Long antecedentId);
}
