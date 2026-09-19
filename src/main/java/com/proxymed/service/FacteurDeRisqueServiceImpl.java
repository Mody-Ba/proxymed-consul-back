package com.proxymed.service;

import com.proxymed.entity.FacteurDeRisque;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.FacteurDeRisqueModel;
import com.proxymed.repository.FacteurDeRisqueRepository;
import com.proxymed.service.mappers.FacteurDeRisqueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacteurDeRisqueServiceImpl implements FacteurDeRisqueService {

    private final FacteurDeRisqueRepository facteurDeRisqueRepository;
    private final FacteurDeRisqueMapper facteurDeRisqueMapper;

    @Override
    public List<FacteurDeRisqueModel> findActifs() {
        return facteurDeRisqueMapper.toModelList(facteurDeRisqueRepository.findByActifTrueOrderByLibelleAsc());
    }

    @Override
    public FacteurDeRisqueModel findById(Long id) {
        FacteurDeRisque entity = facteurDeRisqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facteur de risque introuvable : " + id));
        return facteurDeRisqueMapper.toModel(entity);
    }
}
