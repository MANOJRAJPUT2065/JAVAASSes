package com.cargopro.tms.controller;

import com.cargopro.tms.dto.*;
import com.cargopro.tms.service.TransporterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transporter")
public class TransporterController {
    
    private final TransporterService transporterService;
    
    public TransporterController(TransporterService transporterService) {
        this.transporterService = transporterService;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<TransporterDTO>> createTransporter(
            @Valid @RequestBody CreateTransporterRequest request) {
        TransporterDTO transporter = transporterService.createTransporter(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transporter registered successfully", transporter));
    }
    
    @GetMapping("/{transporterId}")
    public ResponseEntity<ApiResponse<TransporterDTO>> getTransporterById(@PathVariable UUID transporterId) {
        TransporterDTO transporter = transporterService.getTransporterById(transporterId);
        return ResponseEntity.ok(ApiResponse.success(transporter));
    }
    
    @PutMapping("/{transporterId}/trucks")
    public ResponseEntity<ApiResponse<TransporterDTO>> updateTrucks(
            @PathVariable UUID transporterId,
            @Valid @RequestBody UpdateTrucksRequest request) {
        TransporterDTO transporter = transporterService.updateTrucks(transporterId, request);
        return ResponseEntity.ok(ApiResponse.success("Trucks updated successfully", transporter));
    }
}

