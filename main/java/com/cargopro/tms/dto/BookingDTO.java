package com.cargopro.tms.dto;

import com.cargopro.tms.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private UUID bookingId;
    private UUID loadId;
    private UUID bidId;
    private UUID transporterId;
    private String transporterCompanyName;
    private Integer allocatedTrucks;
    private Double finalRate;
    private Booking.BookingStatus status;
    private LocalDateTime bookedAt;
}

