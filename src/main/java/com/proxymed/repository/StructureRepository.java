package com.proxymed.repository;

import com.proxymed.entity.Structure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StructureRepository extends JpaRepository<Structure, Long> {

    List<Structure> findByActifTrueOrderByLibelleAsc();
}
