package com.proxymed.service;

import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.Sexe;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.PatientRepository;
import com.proxymed.service.mappers.PatientMapper;
import com.proxymed.service.model.PatientModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientService = new PatientServiceImpl(patientRepository, new PatientMapper());
        lenient().when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void findAll_calculeLAgeDeChaquePatient() {
        LocalDate naissance = LocalDate.now().minusYears(30);
        Patient patient = Patient.builder().id(UUID.randomUUID()).numeroDossierProxymed("D001")
                .nomComplet("Awa Diop").dateNaissance(naissance).sexe(Sexe.F).build();
        when(patientRepository.findAll()).thenReturn(List.of(patient));

        var resultat = patientService.findAll();

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).age()).isEqualTo(30);
    }

    @Test
    void findById_rejette_siInexistant() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_retourneLePatientAvecAgeCalcule() {
        LocalDate naissance = LocalDate.of(1990, 5, 1);
        UUID id = UUID.randomUUID();
        Patient patient = Patient.builder().id(id).numeroDossierProxymed("D002")
                .nomComplet("Omar Fall").dateNaissance(naissance).sexe(Sexe.M).build();
        when(patientRepository.findById(id)).thenReturn(Optional.of(patient));

        var resultat = patientService.findById(id);

        int ageAttendu = Period.between(naissance, LocalDate.now()).getYears();
        assertThat(resultat.age()).isEqualTo(ageAttendu);
    }

    @Test
    void create_persisteEtRetourneLeModeleAvecAge() {
        LocalDate naissance = LocalDate.now().minusYears(45);
        PatientModel model = PatientModel.builder()
                .numeroDossierProxymed("D003")
                .nomComplet("Fatou Ndiaye")
                .dateNaissance(naissance)
                .sexe(Sexe.F)
                .couvertureSociale(CouvertureSociale.CMU)
                .build();

        var resultat = patientService.create(model);

        assertThat(resultat.nomComplet()).isEqualTo("Fatou Ndiaye");
        assertThat(resultat.age()).isEqualTo(45);
    }

    @Test
    void update_rejette_siPatientIntrouvable() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        PatientModel model = PatientModel.builder().nomComplet("Inconnu").build();

        assertThatThrownBy(() -> patientService.update(id, model))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_appliqueLesModificationsSurLEntiteExistante() {
        UUID id = UUID.randomUUID();
        Patient existant = Patient.builder().id(id).numeroDossierProxymed("D004")
                .nomComplet("Ancien Nom").dateNaissance(LocalDate.now().minusYears(20)).sexe(Sexe.M).build();
        when(patientRepository.findById(id)).thenReturn(Optional.of(existant));

        LocalDate nouvelleNaissance = LocalDate.now().minusYears(25);
        PatientModel model = PatientModel.builder()
                .numeroDossierProxymed("D004")
                .nomComplet("Nouveau Nom")
                .dateNaissance(nouvelleNaissance)
                .sexe(Sexe.M)
                .build();

        var resultat = patientService.update(id, model);

        assertThat(resultat.nomComplet()).isEqualTo("Nouveau Nom");
        assertThat(resultat.age()).isEqualTo(25);
    }
}
