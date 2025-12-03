package com.cargopro.tms.repository;

import com.cargopro.tms.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    
    List<Booking> findByLoad_LoadId(UUID loadId);
    
    List<Booking> findByTransporter_TransporterId(UUID transporterId);
    
    List<Booking> findByLoad_LoadIdAndStatus(UUID loadId, Booking.BookingStatus status);
    
    @Query("SELECT COALESCE(SUM(b.allocatedTrucks), 0) FROM Booking b WHERE b.load.loadId = :loadId AND b.status != 'CANCELLED'")
    Integer getTotalAllocatedTrucks(@Param("loadId") UUID loadId);
    
    Optional<Booking> findByBid_BidId(UUID bidId);
}

