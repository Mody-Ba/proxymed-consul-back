package com.proxymed.repository;

import com.proxymed.entity.FicheSignee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FicheSigneeRepository extends JpaRepository<FicheSignee, UUID> {
}
