package com.proxymed.service;

import com.proxymed.service.model.PatientModel;

import java.util.List;
import java.util.UUID;

public interface PatientService {

    List<PatientModel> findAll();

    PatientModel findById(UUID id);

    PatientModel create(PatientModel model);

    PatientModel update(UUID id, PatientModel model);
}
