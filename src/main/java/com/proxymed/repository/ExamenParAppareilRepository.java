package com.proxymed.repository;

import com.proxymed.entity.ExamenParAppareil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamenParAppareilRepository extends JpaRepository<ExamenParAppareil, Long> {

    Optional<ExamenParAppareil> findByConsultationId(UUID consultationId);
}
