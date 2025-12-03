package com.cargopro.tms.dto;

import com.cargopro.tms.entity.Load;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoadRequest {
    @NotBlank(message = "Shipper ID is required")
    private String shipperId;
    
    @NotBlank(message = "Loading city is required")
    private String loadingCity;
    
    @NotBlank(message = "Unloading city is required")
    private String unloadingCity;
    
    @NotNull(message = "Loading date is required")
    @Future(message = "Loading date must be in the future")
    private LocalDateTime loadingDate;
    
    @NotBlank(message = "Product type is required")
    private String productType;
    
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private Double weight;
    
    @NotNull(message = "Weight unit is required")
    private Load.WeightUnit weightUnit;
    
    @NotBlank(message = "Truck type is required")
    private String truckType;
    
    @NotNull(message = "Number of trucks is required")
    @Min(value = 1, message = "At least 1 truck is required")
    private Integer noOfTrucks;
}

