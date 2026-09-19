package com.proxymed.service.mappers;

import com.proxymed.entity.ExamenParAppareil;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.service.model.ExamenParAppareilModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper DB : convertit uniquement entre ExamenParAppareilModel et l'entite JPA ExamenParAppareil.
 * Ne doit jamais connaitre ExamenParAppareilRequest/Response.
 * Resout lui-meme la reference JPA vers la consultation parente (getReferenceById, simple
 * traduction d'un id en relation) : le service qui l'appelle ne manipule ainsi jamais
 * l'entite ConsultationInitiale, seulement l'id.
 */
@Component("dbExamenParAppareilMapper")
@RequiredArgsConstructor
public class ExamenParAppareilMapper {

    private final ConsultationRepository consultationRepository;

    public ExamenParAppareil toEntity(ExamenParAppareilModel model, UUID consultationId) {
        return ExamenParAppareil.builder()
                .consultation(consultationRepository.getReferenceById(consultationId))
                .etatGeneral(model.etatGeneral())
                .cardioVasculaire(model.cardioVasculaire())
                .respiratoire(model.respiratoire())
                .digestif(model.digestif())
                .neurologique(model.neurologique())
                .locomoteurCutaneAutre(model.locomoteurCutaneAutre())
                .build();
    }

    /**
     * Applique le modele sur une entite managee existante (mutation en place).
     */
    public void applyToEntity(ExamenParAppareil entity, ExamenParAppareilModel model) {
        entity.setEtatGeneral(model.etatGeneral());
        entity.setCardioVasculaire(model.cardioVasculaire());
        entity.setRespiratoire(model.respiratoire());
        entity.setDigestif(model.digestif());
        entity.setNeurologique(model.neurologique());
        entity.setLocomoteurCutaneAutre(model.locomoteurCutaneAutre());
    }

    public ExamenParAppareilModel toModel(ExamenParAppareil entity) {
        if (entity == null) {
            return null;
        }
        return ExamenParAppareilModel.builder()
                .id(entity.getId())
                .etatGeneral(entity.getEtatGeneral())
                .cardioVasculaire(entity.getCardioVasculaire())
                .respiratoire(entity.getRespiratoire())
                .digestif(entity.getDigestif())
                .neurologique(entity.getNeurologique())
                .locomoteurCutaneAutre(entity.getLocomoteurCutaneAutre())
                .build();
    }

    /**
     * Rattache l'examen a la consultation parente cote inverse (1-1 mappedBy). Indispensable
     * si la meme entite/session est relue plus tard dans la meme transaction (ex. tests
     * d'integration @Transactional, ou un futur appel groupe dans la meme requete) : sans
     * cela, une lecture ulterieure de consultation.getExamenParAppareil() resterait perimee.
     * Pure bascule de persistance JPA, pas une regle metier : c'est pourquoi elle vit dans
     * ce mapper plutot que dans le service, qui ne doit jamais toucher l'entite
     * ConsultationInitiale.
     */
    public void synchroniserAvecConsultation(UUID consultationId, ExamenParAppareil examen) {
        consultationRepository.getReferenceById(consultationId).setExamenParAppareil(examen);
    }
}
