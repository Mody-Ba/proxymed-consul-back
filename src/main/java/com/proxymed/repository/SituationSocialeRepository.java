package com.proxymed.repository;

import com.proxymed.entity.SituationSociale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SituationSocialeRepository extends JpaRepository<SituationSociale, Long> {

    List<SituationSociale> findByActifTrueOrderByLibelleAsc();
}
