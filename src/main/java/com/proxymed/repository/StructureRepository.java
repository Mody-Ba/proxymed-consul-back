package com.proxymed.repository;

import com.proxymed.entity.Structure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StructureRepository extends JpaRepository<Structure, Long> {

    List<Structure> findByActifTrueOrderByLibelleAsc();
}
