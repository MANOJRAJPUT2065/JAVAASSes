package com.cargopro.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransporterDTO {
    private UUID transporterId;
    private String companyName;
    private Double rating;
    private List<TruckAvailabilityDTO> availableTrucks;
}

