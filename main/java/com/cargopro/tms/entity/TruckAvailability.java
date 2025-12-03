package com.cargopro.tms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "truck_availability", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"transporter_id", "truck_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TruckAvailability {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "availability_id")
    private UUID availabilityId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;
    
    @Column(name = "truck_type", nullable = false)
    private String truckType;
    
    @Column(name = "count", nullable = false)
    private Integer count;
}

