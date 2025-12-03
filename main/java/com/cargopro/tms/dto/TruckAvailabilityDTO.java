package com.cargopro.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TruckAvailabilityDTO {
    private String truckType;
    private Integer count;
}

