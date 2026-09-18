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
public class SituationSocialeService {

    private final SituationSocialeRepository situationSocialeRepository;
    private final SituationSocialeMapper situationSocialeMapper;

    public List<SituationSocialeModel> findActives() {
        return situationSocialeMapper.toModelList(situationSocialeRepository.findByActifTrueOrderByLibelleAsc());
    }

    public SituationSociale getEntityById(Long id) {
        return situationSocialeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Situation sociale introuvable : " + id));
    }
}
