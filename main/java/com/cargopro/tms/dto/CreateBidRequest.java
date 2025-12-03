package com.cargopro.tms.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBidRequest {
    @NotNull(message = "Load ID is required")
    private UUID loadId;
    
    @NotNull(message = "Transporter ID is required")
    private UUID transporterId;
    
    @NotNull(message = "Proposed rate is required")
    @Positive(message = "Proposed rate must be positive")
    private Double proposedRate;
    
    @NotNull(message = "Trucks offered is required")
    @Min(value = 1, message = "At least 1 truck must be offered")
    private Integer trucksOffered;
}

