package com.cargopro.tms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loads", indexes = {
    @Index(name = "idx_load_shipper_id", columnList = "shipperId"),
    @Index(name = "idx_load_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Load {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "load_id")
    private UUID loadId;
    
    @Column(name = "shipper_id", nullable = false)
    private String shipperId;
    
    @Column(name = "loading_city", nullable = false)
    private String loadingCity;
    
    @Column(name = "unloading_city", nullable = false)
    private String unloadingCity;
    
    @Column(name = "loading_date", nullable = false)
    private LocalDateTime loadingDate;
    
    @Column(name = "product_type", nullable = false)
    private String productType;
    
    @Column(name = "weight", nullable = false)
    private Double weight;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit", nullable = false)
    private WeightUnit weightUnit;
    
    @Column(name = "truck_type", nullable = false)
    private String truckType;
    
    @Column(name = "no_of_trucks", nullable = false)
    private Integer noOfTrucks;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoadStatus status = LoadStatus.POSTED;
    
    @Column(name = "date_posted", nullable = false)
    private LocalDateTime datePosted;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids = new ArrayList<>();
    
    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (datePosted == null) {
            datePosted = LocalDateTime.now();
        }
        if (status == null) {
            status = LoadStatus.POSTED;
        }
    }
    
    public enum LoadStatus {
        POSTED, OPEN_FOR_BIDS, BOOKED, CANCELLED
    }
    
    public enum WeightUnit {
        KG, TON
    }
}

