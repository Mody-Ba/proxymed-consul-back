package com.proxymed.repository;

import com.proxymed.entity.FacteurDeRisque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacteurDeRisqueRepository extends JpaRepository<FacteurDeRisque, Long> {

    List<FacteurDeRisque> findByActifTrueOrderByLibelleAsc();
}
