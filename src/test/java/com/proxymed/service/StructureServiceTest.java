package com.proxymed.service;

import com.proxymed.entity.Structure;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.StructureRepository;
import com.proxymed.service.mappers.StructureMapper;
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
class StructureServiceTest {

    @Mock
    private StructureRepository structureRepository;

    private StructureService structureService;

    @BeforeEach
    void setUp() {
        structureService = new StructureServiceImpl(structureRepository, new StructureMapper());
    }

    @Test
    void findActives_retourneLesStructuresActivesMappees() {
        Structure chu = Structure.builder().id(1L).libelle("CHU").actif(true).build();
        when(structureRepository.findByActifTrueOrderByLibelleAsc()).thenReturn(List.of(chu));

        var resultat = structureService.findActives();

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).libelle()).isEqualTo("CHU");
        assertThat(resultat.get(0).actif()).isTrue();
    }

    @Test
    void findById_retourneLaStructure() {
        Structure clinique = Structure.builder().id(2L).libelle("Clinique").actif(true).build();
        when(structureRepository.findById(2L)).thenReturn(Optional.of(clinique));

        var resultat = structureService.findById(2L);

        assertThat(resultat.id()).isEqualTo(2L);
        assertThat(resultat.libelle()).isEqualTo("Clinique");
    }

    @Test
    void findById_rejette_siInexistant() {
        when(structureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> structureService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
