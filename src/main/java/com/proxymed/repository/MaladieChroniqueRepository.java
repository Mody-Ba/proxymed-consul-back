package com.proxymed.repository;

import com.proxymed.entity.MaladieChronique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaladieChroniqueRepository extends JpaRepository<MaladieChronique, Long> {

    List<MaladieChronique> findByActifTrueOrderByLibelleAsc();
}
