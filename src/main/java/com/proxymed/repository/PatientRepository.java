package com.proxymed.repository;

import com.proxymed.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByNumeroDossierProxymed(String numeroDossierProxymed);

    Optional<Patient> findByNumeroDmi(String numeroDmi);
}
