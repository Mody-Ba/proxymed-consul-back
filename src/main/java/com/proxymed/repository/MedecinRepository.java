package com.proxymed.repository;

import com.proxymed.entity.Medecin;
import com.proxymed.enums.RoleMedecin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedecinRepository extends JpaRepository<Medecin, Long> {

    Optional<Medecin> findByNumeroOrdre(String numeroOrdre);

    List<Medecin> findByRole(RoleMedecin role);
}
