package com.proxymed.service;

import com.proxymed.entity.Structure;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.StructureModel;
import com.proxymed.repository.StructureRepository;
import com.proxymed.service.mappers.StructureMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StructureServiceImpl implements StructureService {

    private final StructureRepository structureRepository;
    private final StructureMapper structureMapper;

    @Override
    public List<StructureModel> findActives() {
        return structureMapper.toModelList(structureRepository.findByActifTrueOrderByLibelleAsc());
    }

    @Override
    public StructureModel findById(Long id) {
        Structure entity = structureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Structure introuvable : " + id));
        return structureMapper.toModel(entity);
    }
}
