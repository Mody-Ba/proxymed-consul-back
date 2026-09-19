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
public class MaladieChroniqueServiceImpl implements MaladieChroniqueService {

    private final MaladieChroniqueRepository maladieChroniqueRepository;
    private final MaladieChroniqueMapper maladieChroniqueMapper;

    @Override
    public List<MaladieChroniqueModel> findActives() {
        return maladieChroniqueMapper.toModelList(maladieChroniqueRepository.findByActifTrueOrderByLibelleAsc());
    }

    @Override
    public MaladieChroniqueModel findById(Long id) {
        MaladieChronique entity = maladieChroniqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maladie chronique introuvable : " + id));
        return maladieChroniqueMapper.toModel(entity);
    }
}
