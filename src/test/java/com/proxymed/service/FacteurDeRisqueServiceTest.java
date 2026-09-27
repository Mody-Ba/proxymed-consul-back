package com.proxymed.service;

import com.proxymed.entity.FacteurDeRisque;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.FacteurDeRisqueRepository;
import com.proxymed.service.mappers.FacteurDeRisqueMapper;
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
class FacteurDeRisqueServiceTest {

    @Mock
    private FacteurDeRisqueRepository facteurDeRisqueRepository;

    private FacteurDeRisqueService facteurDeRisqueService;

    @BeforeEach
    void setUp() {
        facteurDeRisqueService = new FacteurDeRisqueServiceImpl(facteurDeRisqueRepository, new FacteurDeRisqueMapper());
    }

    @Test
    void findActifs_retourneLesFacteursActifsMappes() {
        FacteurDeRisque tabagisme = FacteurDeRisque.builder().id(1L).libelle("Tabagisme").actif(true).build();
        when(facteurDeRisqueRepository.findByActifTrueOrderByLibelleAsc()).thenReturn(List.of(tabagisme));

        var resultat = facteurDeRisqueService.findActifs();

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).libelle()).isEqualTo("Tabagisme");
        assertThat(resultat.get(0).actif()).isTrue();
    }

    @Test
    void findById_retourneLeFacteur() {
        FacteurDeRisque obesite = FacteurDeRisque.builder().id(2L).libelle("Obesite").actif(true).build();
        when(facteurDeRisqueRepository.findById(2L)).thenReturn(Optional.of(obesite));

        var resultat = facteurDeRisqueService.findById(2L);

        assertThat(resultat.id()).isEqualTo(2L);
        assertThat(resultat.libelle()).isEqualTo("Obesite");
    }

    @Test
    void findById_rejette_siInexistant() {
        when(facteurDeRisqueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facteurDeRisqueService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
