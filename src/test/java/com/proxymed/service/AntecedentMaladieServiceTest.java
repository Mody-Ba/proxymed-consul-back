package com.proxymed.service;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.MaladieChronique;
import com.proxymed.exception.ConflitEtatException;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.MaladieChroniqueRepository;
import com.proxymed.service.model.AntecedentMaladieModel;
import com.proxymed.service.model.MaladieChroniqueModel;
import com.proxymed.repository.AntecedentMaladieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AntecedentMaladieServiceTest {

    @Mock
    private AntecedentMaladieRepository antecedentMaladieRepository;
    @Mock
    private ConsultationRepository consultationRepository;
    @Mock
    private MaladieChroniqueRepository maladieChroniqueRepository;
    @Mock
    private MaladieChroniqueService maladieChroniqueService;
    @Mock
    private ConsultationService consultationService;

    private AntecedentMaladieService antecedentMaladieService;

    @BeforeEach
    void setUp() {
        antecedentMaladieService = new AntecedentMaladieServiceImpl(
                antecedentMaladieRepository, maladieChroniqueService, consultationService,
                new com.proxymed.service.mappers.AntecedentMaladieMapper(consultationRepository, maladieChroniqueRepository));
        lenient().when(antecedentMaladieRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void ajouter_rejette_siFicheSignee() {
        UUID consultationId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ConflitEtatException("La fiche est signee et n'est plus modifiable"))
                .when(consultationService).verifierModifiable(consultationId);

        AntecedentMaladieModel model = AntecedentMaladieModel.builder().maladieChroniqueId(1L).precision("precision").build();

        assertThatThrownBy(() -> antecedentMaladieService.ajouter(consultationId, model))
                .isInstanceOf(ConflitEtatException.class);
    }

    @Test
    void ajouter_associeLaMaladieChroniqueALaConsultation() {
        ConsultationInitiale consultation = ConsultationInitiale.builder().id(UUID.randomUUID()).build();
        MaladieChronique diabete = MaladieChronique.builder().id(5L).libelle("Diabete").build();
        when(maladieChroniqueService.findById(5L)).thenReturn(
                MaladieChroniqueModel.builder().id(5L).libelle("Diabete").actif(true).build());
        when(consultationRepository.getReferenceById(consultation.getId())).thenReturn(consultation);
        when(maladieChroniqueRepository.getReferenceById(5L)).thenReturn(diabete);

        AntecedentMaladieModel model = AntecedentMaladieModel.builder().maladieChroniqueId(5L).precision("depuis 2019").build();
        var resultat = antecedentMaladieService.ajouter(consultation.getId(), model);

        assertThat(resultat.maladieChroniqueLibelle()).isEqualTo("Diabete");
        assertThat(resultat.precision()).isEqualTo("depuis 2019");
    }
}
