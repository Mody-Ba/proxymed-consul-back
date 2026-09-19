package com.proxymed.service;

import com.proxymed.entity.Medecin;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.MedecinModel;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.service.mappers.MedecinMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * La seule entite JPA visible dans cette classe est Medecin (l'aggregat propre a ce
 * service). Structure (aggregat voisin) n'est jamais manipulee ici : son existence est
 * verifiee via StructureService.findById (Model), et le rattachement JPA (id -> reference)
 * est delegue au mapper DB.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MedecinServiceImpl implements MedecinService {

    private final MedecinRepository medecinRepository;
    private final StructureService structureService;
    private final MedecinMapper medecinMapper;

    @Override
    public List<MedecinModel> findAll(RoleMedecin role) {
        List<Medecin> medecins = role != null ? medecinRepository.findByRole(role) : medecinRepository.findAll();
        return medecins.stream().map(medecinMapper::toModel).toList();
    }

    @Override
    public MedecinModel findById(Long id) {
        return medecinMapper.toModel(getEntityById(id));
    }

    @Override
    public MedecinModel create(MedecinModel model) {
        if (model.structureRattachementId() != null) {
            structureService.findById(model.structureRattachementId());
        }
        Medecin medecin = medecinMapper.toEntity(model);
        return medecinMapper.toModel(medecinRepository.save(medecin));
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private Medecin getEntityById(Long id) {
        return medecinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medecin introuvable : " + id));
    }
}
