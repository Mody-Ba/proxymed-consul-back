package com.proxymed.service;

import com.proxymed.service.model.StructureModel;

import java.util.List;

public interface StructureService {

    List<StructureModel> findActives();

    StructureModel findById(Long id);
}
