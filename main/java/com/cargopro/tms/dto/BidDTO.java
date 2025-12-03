package com.cargopro.tms.dto;

import com.cargopro.tms.entity.Bid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidDTO {
    private UUID bidId;
    private UUID loadId;
    private UUID transporterId;
    private String transporterCompanyName;
    private Double transporterRating;
    private Double proposedRate;
    private Integer trucksOffered;
    private Bid.BidStatus status;
    private LocalDateTime submittedAt;
    private Double bidScore; // For best-bids calculation
}

