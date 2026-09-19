package com.proxymed.service.mappers;

import com.proxymed.entity.AntecedentMaladie;
import com.proxymed.entity.MaladieChronique;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.MaladieChroniqueRepository;
import com.proxymed.service.model.AntecedentMaladieModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Mapper DB : convertit uniquement entre AntecedentMaladieModel et l'entite JPA AntecedentMaladie.
 * Ne doit jamais connaitre AntecedentMaladieRequest/Response.
 * Resout lui-meme les references JPA vers la consultation et la maladie chronique
 * (getReferenceById, simple traduction d'un id en relation) : le service qui l'appelle ne
 * manipule ainsi jamais les entites ConsultationInitiale/MaladieChronique, seulement leurs id.
 */
@Component("dbAntecedentMaladieMapper")
@RequiredArgsConstructor
public class AntecedentMaladieMapper {

    private final ConsultationRepository consultationRepository;
    private final MaladieChroniqueRepository maladieChroniqueRepository;

    public AntecedentMaladie toEntity(AntecedentMaladieModel model, UUID consultationId) {
        return AntecedentMaladie.builder()
                .consultation(consultationRepository.getReferenceById(consultationId))
                .maladieChronique(maladieChroniqueRepository.getReferenceById(model.maladieChroniqueId()))
                .precision(model.precision())
                .build();
    }

    public AntecedentMaladieModel toModel(AntecedentMaladie entity) {
        MaladieChronique maladie = entity.getMaladieChronique();
        return AntecedentMaladieModel.builder()
                .id(entity.getId())
                .consultationId(entity.getConsultation().getId())
                .maladieChroniqueId(maladie.getId())
                .maladieChroniqueLibelle(maladie.getLibelle())
                .precision(entity.getPrecision())
                .build();
    }

    public List<AntecedentMaladieModel> toModelList(List<AntecedentMaladie> entities) {
        return entities.stream().map(this::toModel).toList();
    }

    /**
     * Rattache l'antecedent a la collection en memoire de la consultation parente.
     * Indispensable si la meme entite/session est relue plus tard dans la meme transaction
     * (ex. tests d'integration @Transactional, ou un futur appel groupe dans la meme requete) :
     * sans cela, la collection lazy deja chargee resterait perimee. Pure bascule de
     * persistance JPA, pas une regle metier : c'est pourquoi elle vit dans ce mapper plutot
     * que dans le service, qui ne doit jamais toucher l'entite ConsultationInitiale.
     */
    public void synchroniserAvecConsultation(UUID consultationId, AntecedentMaladie antecedent) {
        consultationRepository.getReferenceById(consultationId).getMaladiesChroniques().add(antecedent);
    }
}
