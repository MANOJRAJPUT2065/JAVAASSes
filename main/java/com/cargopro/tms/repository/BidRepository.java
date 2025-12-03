package com.cargopro.tms.repository;

import com.cargopro.tms.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {
    
    List<Bid> findByLoad_LoadId(UUID loadId);
    
    List<Bid> findByTransporter_TransporterId(UUID transporterId);
    
    List<Bid> findByLoad_LoadIdAndStatus(UUID loadId, Bid.BidStatus status);
    
    List<Bid> findByTransporter_TransporterIdAndStatus(UUID transporterId, Bid.BidStatus status);
    
    List<Bid> findByLoad_LoadIdAndTransporter_TransporterId(UUID loadId, UUID transporterId);
    
    @Query("SELECT b FROM Bid b WHERE b.load.loadId = :loadId AND b.status = 'PENDING'")
    List<Bid> findActiveBidsByLoadId(@Param("loadId") UUID loadId);
    
    @Query("SELECT COUNT(b) > 0 FROM Bid b WHERE b.load.loadId = :loadId AND b.status = 'ACCEPTED'")
    boolean existsAcceptedBidForLoad(@Param("loadId") UUID loadId);
    
    Optional<Bid> findByBidIdAndStatus(UUID bidId, Bid.BidStatus status);
}

