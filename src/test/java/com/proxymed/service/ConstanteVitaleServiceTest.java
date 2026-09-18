package com.proxymed.service;

import com.proxymed.entity.ConstanteVitale;
import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.TypeConstanteVitale;
import com.proxymed.exception.ConflitEtatException;
import com.proxymed.service.model.ConstanteVitaleModel;
import com.proxymed.repository.ConstanteVitaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstanteVitaleServiceTest {

    @Mock
    private ConstanteVitaleRepository constanteVitaleRepository;
    @Mock
    private ConsultationService consultationService;

    private ConstanteVitaleService constanteVitaleService;

    @BeforeEach
    void setUp() {
        constanteVitaleService = new ConstanteVitaleService(
                constanteVitaleRepository, consultationService, new com.proxymed.service.mappers.ConstanteVitaleMapper());
        lenient().when(constanteVitaleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void ajouter_rejette_siFicheSignee() {
        UUID consultationId = UUID.randomUUID();
        when(consultationService.getModifiableEntityById(consultationId))
                .thenThrow(new ConflitEtatException("La fiche est signee et n'est plus modifiable"));

        ConstanteVitaleModel model = ConstanteVitaleModel.builder().type(TypeConstanteVitale.FC).valeur(BigDecimal.valueOf(80)).build();

        assertThatThrownBy(() -> constanteVitaleService.ajouter(consultationId, model))
                .isInstanceOf(ConflitEtatException.class);
    }

    @Test
    void ajouter_calculeAutomatiquementLimc_quandPoidsEtTailleConnus() {
        ConsultationInitiale consultation = ConsultationInitiale.builder()
                .id(UUID.randomUUID())
                .statut(StatutConsultation.BROUILLON)
                .build();
        when(consultationService.getModifiableEntityById(consultation.getId())).thenReturn(consultation);

        ConstanteVitale poidsEntity = ConstanteVitale.builder().id(1L).consultation(consultation)
                .type(TypeConstanteVitale.POIDS).valeur(BigDecimal.valueOf(70)).build();
        ConstanteVitale tailleEntity = ConstanteVitale.builder().id(2L).consultation(consultation)
                .type(TypeConstanteVitale.TAILLE).valeur(BigDecimal.valueOf(175)).build();

        // Apres le premier ajout (POIDS), seule cette constante existe encore en base.
        lenient().when(constanteVitaleRepository.findByConsultationId(consultation.getId()))
                .thenReturn(List.of(poidsEntity));
        constanteVitaleService.ajouter(consultation.getId(),
                ConstanteVitaleModel.builder().type(TypeConstanteVitale.POIDS).valeur(BigDecimal.valueOf(70)).build());

        // Apres le second ajout (TAILLE), les deux constantes sont connues -> IMC calculable.
        when(constanteVitaleRepository.findByConsultationId(consultation.getId()))
                .thenReturn(List.of(poidsEntity, tailleEntity));
        constanteVitaleService.ajouter(consultation.getId(),
                ConstanteVitaleModel.builder().type(TypeConstanteVitale.TAILLE).valeur(BigDecimal.valueOf(175)).build());

        ArgumentCaptor<ConstanteVitale> captor = ArgumentCaptor.forClass(ConstanteVitale.class);
        verify(constanteVitaleRepository, atLeastOnce()).save(captor.capture());
        boolean imcSaved = captor.getAllValues().stream()
                .anyMatch(c -> c.getType() == TypeConstanteVitale.IMC
                        && c.getValeur() != null && c.getValeur().compareTo(new BigDecimal("22.9")) == 0);
        assertThat(imcSaved).isTrue();
    }
}
