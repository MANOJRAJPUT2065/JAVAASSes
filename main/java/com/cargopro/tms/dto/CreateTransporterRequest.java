package com.cargopro.tms.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransporterRequest {
    @NotBlank(message = "Company name is required")
    private String companyName;
    
    @NotNull(message = "Rating is required")
    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5.0")
    private Double rating;
    
    @NotEmpty(message = "At least one truck availability is required")
    private List<TruckAvailabilityDTO> availableTrucks;
}

