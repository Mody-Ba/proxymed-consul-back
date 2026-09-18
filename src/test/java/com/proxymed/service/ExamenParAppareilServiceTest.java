package com.proxymed.service;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.exception.ConflitEtatException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.service.model.ExamenParAppareilModel;
import com.proxymed.repository.ExamenParAppareilRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamenParAppareilServiceTest {

    @Mock
    private ExamenParAppareilRepository examenParAppareilRepository;
    @Mock
    private ConsultationService consultationService;

    private ExamenParAppareilService examenParAppareilService;

    @BeforeEach
    void setUp() {
        examenParAppareilService = new ExamenParAppareilService(
                examenParAppareilRepository, consultationService, new com.proxymed.service.mappers.ExamenParAppareilMapper());
        lenient().when(examenParAppareilRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void findByConsultation_rejette_siInexistant() {
        UUID consultationId = UUID.randomUUID();
        when(examenParAppareilRepository.findByConsultationId(consultationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examenParAppareilService.findByConsultation(consultationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void creerOuMettreAJour_rejette_siFicheSignee() {
        UUID consultationId = UUID.randomUUID();
        when(consultationService.getModifiableEntityById(consultationId))
                .thenThrow(new ConflitEtatException("La fiche est signee et n'est plus modifiable"));

        ExamenParAppareilModel model = ExamenParAppareilModel.builder().etatGeneral("bon").build();

        assertThatThrownBy(() -> examenParAppareilService.creerOuMettreAJour(consultationId, model))
                .isInstanceOf(ConflitEtatException.class);
    }

    @Test
    void creerOuMettreAJour_creeLExamenSiAbsent() {
        ConsultationInitiale consultation = ConsultationInitiale.builder().id(UUID.randomUUID()).build();
        when(consultationService.getModifiableEntityById(consultation.getId())).thenReturn(consultation);

        ExamenParAppareilModel model = ExamenParAppareilModel.builder().etatGeneral("altere").cardioVasculaire("RAS").build();
        var resultat = examenParAppareilService.creerOuMettreAJour(consultation.getId(), model);

        assertThat(resultat.etatGeneral()).isEqualTo("altere");
        assertThat(consultation.getExamenParAppareil()).isNotNull();
    }
}
