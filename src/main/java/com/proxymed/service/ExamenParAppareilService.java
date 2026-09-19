package com.proxymed.service;

import com.proxymed.service.model.ExamenParAppareilModel;

import java.util.UUID;

/**
 * Gere l'examen par appareil (section 5, relation 1-1) d'une consultation.
 */
public interface ExamenParAppareilService {

    ExamenParAppareilModel findByConsultation(UUID consultationId);

    ExamenParAppareilModel creerOuMettreAJour(UUID consultationId, ExamenParAppareilModel model);
}
