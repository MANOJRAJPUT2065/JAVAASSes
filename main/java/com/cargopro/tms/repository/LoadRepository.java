package com.cargopro.tms.repository;

import com.cargopro.tms.entity.Load;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoadRepository extends JpaRepository<Load, UUID> {
    
    Page<Load> findByShipperIdAndStatus(String shipperId, Load.LoadStatus status, Pageable pageable);
    
    Page<Load> findByShipperId(String shipperId, Pageable pageable);
    
    Page<Load> findByStatus(Load.LoadStatus status, Pageable pageable);
    
    @Query("SELECT l FROM Load l WHERE l.loadId = :loadId")
    Optional<Load> findByIdWithBids(@Param("loadId") UUID loadId);
    
    @Query("SELECT COALESCE(SUM(b.allocatedTrucks), 0) FROM Booking b WHERE b.load.loadId = :loadId AND b.status != 'CANCELLED'")
    Integer getTotalAllocatedTrucks(@Param("loadId") UUID loadId);
}

