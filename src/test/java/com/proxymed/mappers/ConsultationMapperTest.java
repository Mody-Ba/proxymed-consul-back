package com.proxymed.mappers;

import com.proxymed.enums.NiveauAutonomie;
import com.proxymed.enums.OrigineDemande;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.ConsultationUpdateRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste que le mapper API construit bien une intention de mise a jour PARTIELLE :
 * seuls les champs presents dans la requete sont renseignes dans le modele produit,
 * le reste reste absent (null) pour que le service sache qu'il ne doit pas y toucher.
 */
class ConsultationMapperTest {

    private final ConsultationMapper mapper = new ConsultationMapper(
            new PatientMapper(), new MedecinMapper(), new FacteurDeRisqueMapper(), new SituationSocialeMapper(),
            new ConstanteVitaleMapper(), new ExamenParAppareilMapper(), new AntecedentMaladieMapper());

    @Test
    void toModel_neRenseigneQueLesChampsPresentsDansLaRequete() {
        ConsultationUpdateRequest request = ConsultationUpdateRequest.builder()
                .niveauAutonomie(NiveauAutonomie.TOTALEMENT_AUTONOME)
                .build();

        ConsultationModel model = mapper.toModel(request);

        assertThat(model.niveauAutonomie()).isEqualTo(NiveauAutonomie.TOTALEMENT_AUTONOME);
        assertThat(model.origineDemande()).isNull();
        assertThat(model.motifPrincipalConsultation()).isNull();
        assertThat(model.patient()).isNull();
        assertThat(model.statut()).isNull();
    }

    @Test
    void toModel_construitDesReferencesPartielsPourLesRelations() {
        ConsultationUpdateRequest request = ConsultationUpdateRequest.builder()
                .origineDemande(OrigineDemande.SAMU)
                .facteursDeRisqueIds(java.util.List.of(1L, 2L))
                .medecinJuniorAffecteId(9L)
                .build();

        ConsultationModel model = mapper.toModel(request);

        assertThat(model.origineDemande()).isEqualTo(OrigineDemande.SAMU);
        assertThat(model.facteursDeRisque()).extracting("id").containsExactly(1L, 2L);
        assertThat(model.medecinJuniorAffecte().id()).isEqualTo(9L);
    }
}
