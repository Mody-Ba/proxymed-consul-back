package com.proxymed.repository;

import com.proxymed.entity.FacteurDeRisque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacteurDeRisqueRepository extends JpaRepository<FacteurDeRisque, Long> {

    List<FacteurDeRisque> findByActifTrueOrderByLibelleAsc();
}
