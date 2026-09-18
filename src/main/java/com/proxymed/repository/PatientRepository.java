package com.proxymed.repository;

import com.proxymed.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByNumeroDossierProxymed(String numeroDossierProxymed);

    Optional<Patient> findByNumeroDmi(String numeroDmi);
}
