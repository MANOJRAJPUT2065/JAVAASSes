package com.cargopro.tms.dto;

import com.cargopro.tms.entity.Load;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadDTO {
    private UUID loadId;
    private String shipperId;
    private String loadingCity;
    private String unloadingCity;
    private LocalDateTime loadingDate;
    private String productType;
    private Double weight;
    private Load.WeightUnit weightUnit;
    private String truckType;
    private Integer noOfTrucks;
    private Load.LoadStatus status;
    private LocalDateTime datePosted;
    private List<BidDTO> activeBids;
    private Integer remainingTrucks;
}

