package com.proxymed.service;

import com.proxymed.service.model.ConstanteVitaleModel;

import java.util.UUID;

public interface ConstanteVitaleService {

    ConstanteVitaleModel ajouter(UUID consultationId, ConstanteVitaleModel model);

    ConstanteVitaleModel modifier(UUID consultationId, Long constanteId, ConstanteVitaleModel model);
}
