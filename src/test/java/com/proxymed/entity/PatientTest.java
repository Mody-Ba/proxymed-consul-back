package com.proxymed.entity;

import com.proxymed.enums.Sexe;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PatientTest {

    @Test
    void getAge_calculeDepuisLaDateDeNaissance() {
        Patient patient = Patient.builder()
                .dateNaissance(LocalDate.now().minusYears(72).minusDays(1))
                .sexe(Sexe.F)
                .build();

        assertThat(patient.getAge()).isEqualTo(72);
    }

    @Test
    void getAge_anniversairePasEncorePasseCetteAnnee() {
        Patient patient = Patient.builder()
                .dateNaissance(LocalDate.now().minusYears(72).plusDays(1))
                .sexe(Sexe.M)
                .build();

        assertThat(patient.getAge()).isEqualTo(71);
    }

    @Test
    void getAge_null_siPasDeDateDeNaissance() {
        Patient patient = Patient.builder().build();

        assertThat(patient.getAge()).isNull();
    }
}
