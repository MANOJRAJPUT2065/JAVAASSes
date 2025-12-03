package com.cargopro.tms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transporters", indexes = {
    @Index(name = "idx_transporter_id", columnList = "transporterId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transporter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transporter_id")
    private UUID transporterId;
    
    @Column(name = "company_name", nullable = false, unique = true)
    private String companyName;
    
    @Column(name = "rating", nullable = false)
    private Double rating;
    
    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TruckAvailability> availableTrucks = new ArrayList<>();
    
    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids = new ArrayList<>();
    
    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();
    
    @PrePersist
    @PreUpdate
    protected void validateRating() {
        if (rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
    
    public Integer getAvailableTruckCount(String truckType) {
        return availableTrucks.stream()
                .filter(ta -> ta.getTruckType().equals(truckType))
                .findFirst()
                .map(TruckAvailability::getCount)
                .orElse(0);
    }
    
    public void updateTruckAvailability(String truckType, int countChange) {
        TruckAvailability availability = availableTrucks.stream()
                .filter(ta -> ta.getTruckType().equals(truckType))
                .findFirst()
                .orElse(null);
        
        if (availability == null) {
            if (countChange > 0) {
                availability = new TruckAvailability();
                availability.setTransporter(this);
                availability.setTruckType(truckType);
                availability.setCount(countChange);
                availableTrucks.add(availability);
            }
        } else {
            int newCount = availability.getCount() + countChange;
            if (newCount < 0) {
                throw new IllegalArgumentException("Cannot have negative truck count");
            }
            if (newCount == 0) {
                availableTrucks.remove(availability);
            } else {
                availability.setCount(newCount);
            }
        }
    }
}

