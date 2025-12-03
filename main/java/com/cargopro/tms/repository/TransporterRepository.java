package com.cargopro.tms.repository;

import com.cargopro.tms.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransporterRepository extends JpaRepository<Transporter, UUID> {
    Optional<Transporter> findByCompanyName(String companyName);
}

