package com.proxymed.service;

import com.proxymed.entity.MaladieChronique;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.MaladieChroniqueModel;
import com.proxymed.repository.MaladieChroniqueRepository;
import com.proxymed.service.mappers.MaladieChroniqueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaladieChroniqueService {

    private final MaladieChroniqueRepository maladieChroniqueRepository;
    private final MaladieChroniqueMapper maladieChroniqueMapper;

    public List<MaladieChroniqueModel> findActives() {
        return maladieChroniqueMapper.toModelList(maladieChroniqueRepository.findByActifTrueOrderByLibelleAsc());
    }

    public MaladieChronique getEntityById(Long id) {
        return maladieChroniqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maladie chronique introuvable : " + id));
    }
}
