package com.proxymed.repository;

import com.proxymed.entity.ConstanteVitale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConstanteVitaleRepository extends JpaRepository<ConstanteVitale, Long> {

    List<ConstanteVitale> findByConsultationId(UUID consultationId);
}
