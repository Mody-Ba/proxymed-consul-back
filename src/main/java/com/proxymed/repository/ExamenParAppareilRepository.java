package com.proxymed.repository;

import com.proxymed.entity.ExamenParAppareil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamenParAppareilRepository extends JpaRepository<ExamenParAppareil, Long> {

    Optional<ExamenParAppareil> findByConsultationId(UUID consultationId);
}
