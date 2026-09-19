package com.proxymed.service;

import com.proxymed.entity.SituationSociale;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.SituationSocialeModel;
import com.proxymed.repository.SituationSocialeRepository;
import com.proxymed.service.mappers.SituationSocialeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SituationSocialeServiceImpl implements SituationSocialeService {

    private final SituationSocialeRepository situationSocialeRepository;
    private final SituationSocialeMapper situationSocialeMapper;

    @Override
    public List<SituationSocialeModel> findActives() {
        return situationSocialeMapper.toModelList(situationSocialeRepository.findByActifTrueOrderByLibelleAsc());
    }

    @Override
    public SituationSocialeModel findById(Long id) {
        SituationSociale entity = situationSocialeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Situation sociale introuvable : " + id));
        return situationSocialeMapper.toModel(entity);
    }
}
