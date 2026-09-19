package com.proxymed.service.mappers;

import com.proxymed.entity.ConstanteVitale;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.service.model.ConstanteVitaleModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Mapper DB : convertit uniquement entre ConstanteVitaleModel et l'entite JPA ConstanteVitale.
 * Ne doit jamais connaitre ConstanteVitaleRequest/ConstanteVitaleResponse.
 * Resout lui-meme la reference JPA vers la consultation parente (getReferenceById, simple
 * traduction d'un id en relation) : le service qui l'appelle ne manipule ainsi jamais
 * l'entite ConsultationInitiale, seulement l'id.
 */
@Component("dbConstanteVitaleMapper")
@RequiredArgsConstructor
public class ConstanteVitaleMapper {

    private final ConsultationRepository consultationRepository;

    public ConstanteVitale toEntity(ConstanteVitaleModel model, UUID consultationId) {
        return ConstanteVitale.builder()
                .consultation(consultationRepository.getReferenceById(consultationId))
                .type(model.type())
                .valeur(model.valeur())
                .heure(model.heure())
                .estNormal(model.estNormal())
                .estAlerte(model.estAlerte())
                .commentaire(model.commentaire())
                .build();
    }

    /**
     * Applique le modele sur une entite managee existante (mutation en place).
     */
    public void applyToEntity(ConstanteVitale entity, ConstanteVitaleModel model) {
        entity.setType(model.type());
        entity.setValeur(model.valeur());
        entity.setHeure(model.heure());
        entity.setEstNormal(model.estNormal());
        entity.setEstAlerte(model.estAlerte());
        entity.setCommentaire(model.commentaire());
    }

    public ConstanteVitaleModel toModel(ConstanteVitale entity) {
        return ConstanteVitaleModel.builder()
                .id(entity.getId())
                .consultationId(entity.getConsultation().getId())
                .type(entity.getType())
                .valeur(entity.getValeur())
                .heure(entity.getHeure())
                .estNormal(entity.getEstNormal())
                .estAlerte(entity.getEstAlerte())
                .commentaire(entity.getCommentaire())
                .build();
    }

    public List<ConstanteVitaleModel> toModelList(List<ConstanteVitale> entities) {
        return entities.stream().map(this::toModel).toList();
    }

    /**
     * Rattache la constante a la collection en memoire de la consultation parente.
     * Indispensable si la meme entite/session est relue plus tard dans la meme transaction
     * (ex. tests d'integration @Transactional, ou un futur appel groupe dans la meme requete) :
     * sans cela, la collection lazy deja chargee resterait perimee. Pure bascule de
     * persistance JPA, pas une regle metier : c'est pourquoi elle vit dans ce mapper plutot
     * que dans le service, qui ne doit jamais toucher l'entite ConsultationInitiale.
     */
    public void synchroniserAvecConsultation(UUID consultationId, ConstanteVitale constante) {
        consultationRepository.getReferenceById(consultationId).getConstantesVitales().add(constante);
    }
}
