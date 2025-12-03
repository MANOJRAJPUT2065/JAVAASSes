package com.cargopro.tms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bids", indexes = {
    @Index(name = "idx_bid_load_id", columnList = "loadId"),
    @Index(name = "idx_bid_transporter_id", columnList = "transporterId"),
    @Index(name = "idx_bid_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bid {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bid_id")
    private UUID bidId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id", nullable = false)
    private Load load;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;
    
    @Column(name = "proposed_rate", nullable = false)
    private Double proposedRate;
    
    @Column(name = "trucks_offered", nullable = false)
    private Integer trucksOffered;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BidStatus status = BidStatus.PENDING;
    
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    
    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BidStatus.PENDING;
        }
    }
    
    public enum BidStatus {
        PENDING, ACCEPTED, REJECTED
    }
}

