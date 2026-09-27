package com.proxymed.service;

import com.proxymed.entity.MaladieChronique;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.MaladieChroniqueRepository;
import com.proxymed.service.mappers.MaladieChroniqueMapper;
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
class MaladieChroniqueServiceTest {

    @Mock
    private MaladieChroniqueRepository maladieChroniqueRepository;

    private MaladieChroniqueService maladieChroniqueService;

    @BeforeEach
    void setUp() {
        maladieChroniqueService = new MaladieChroniqueServiceImpl(maladieChroniqueRepository, new MaladieChroniqueMapper());
    }

    @Test
    void findActives_retourneLesMaladiesActivesMappees() {
        MaladieChronique diabete = MaladieChronique.builder().id(1L).libelle("Diabete").actif(true).build();
        when(maladieChroniqueRepository.findByActifTrueOrderByLibelleAsc()).thenReturn(List.of(diabete));

        var resultat = maladieChroniqueService.findActives();

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).libelle()).isEqualTo("Diabete");
        assertThat(resultat.get(0).actif()).isTrue();
    }

    @Test
    void findById_retourneLaMaladie() {
        MaladieChronique hta = MaladieChronique.builder().id(2L).libelle("HTA").actif(true).build();
        when(maladieChroniqueRepository.findById(2L)).thenReturn(Optional.of(hta));

        var resultat = maladieChroniqueService.findById(2L);

        assertThat(resultat.id()).isEqualTo(2L);
        assertThat(resultat.libelle()).isEqualTo("HTA");
    }

    @Test
    void findById_rejette_siInexistant() {
        when(maladieChroniqueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maladieChroniqueService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
