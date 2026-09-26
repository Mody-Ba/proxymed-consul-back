package com.proxymed.service;

import com.proxymed.entity.SituationSociale;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.SituationSocialeRepository;
import com.proxymed.service.mappers.SituationSocialeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SituationSocialeServiceTest {

    @Mock
    private SituationSocialeRepository situationSocialeRepository;

    private SituationSocialeService situationSocialeService;

    @BeforeEach
    void setUp() {
        situationSocialeService = new SituationSocialeServiceImpl(situationSocialeRepository, new SituationSocialeMapper());
    }

    @Test
    void findActives_retourneLesSituationsActivesMappees() {
        SituationSociale isolement = SituationSociale.builder().id(1L).libelle("Isolement").actif(true).build();
        when(situationSocialeRepository.findByActifTrueOrderByLibelleAsc()).thenReturn(List.of(isolement));

        var resultat = situationSocialeService.findActives();

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).libelle()).isEqualTo("Isolement");
        assertThat(resultat.get(0).actif()).isTrue();
    }

    @Test
    void findById_retourneLaSituation() {
        SituationSociale precarite = SituationSociale.builder().id(2L).libelle("Precarite").actif(true).build();
        when(situationSocialeRepository.findById(2L)).thenReturn(Optional.of(precarite));

        var resultat = situationSocialeService.findById(2L);

        assertThat(resultat.id()).isEqualTo(2L);
        assertThat(resultat.libelle()).isEqualTo("Precarite");
    }

    @Test
    void findById_rejette_siInexistant() {
        when(situationSocialeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> situationSocialeService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
