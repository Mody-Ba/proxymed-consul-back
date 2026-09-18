package com.proxymed.repository;

import com.proxymed.entity.SituationSociale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SituationSocialeRepository extends JpaRepository<SituationSociale, Long> {

    List<SituationSociale> findByActifTrueOrderByLibelleAsc();
}
