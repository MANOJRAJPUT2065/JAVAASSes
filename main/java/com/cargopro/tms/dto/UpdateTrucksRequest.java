package com.cargopro.tms.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTrucksRequest {
    @NotEmpty(message = "At least one truck availability is required")
    private List<TruckAvailabilityDTO> availableTrucks;
}

