package com.proxymed.repository;

import com.proxymed.entity.MaladieChronique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaladieChroniqueRepository extends JpaRepository<MaladieChronique, Long> {

    List<MaladieChronique> findByActifTrueOrderByLibelleAsc();
}
