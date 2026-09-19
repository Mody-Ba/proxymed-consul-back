package com.proxymed.repository;

import com.proxymed.entity.AntecedentMaladie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AntecedentMaladieRepository extends JpaRepository<AntecedentMaladie, Long> {

    List<AntecedentMaladie> findByConsultationId(UUID consultationId);
}
