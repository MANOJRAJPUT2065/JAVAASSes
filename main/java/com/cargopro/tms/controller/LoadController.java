package com.cargopro.tms.controller;

import com.cargopro.tms.dto.*;
import com.cargopro.tms.entity.Load;
import com.cargopro.tms.service.LoadService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/load")
public class LoadController {
    
    private final LoadService loadService;
    
    public LoadController(LoadService loadService) {
        this.loadService = loadService;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<LoadDTO>> createLoad(@Valid @RequestBody CreateLoadRequest request) {
        LoadDTO load = loadService.createLoad(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Load created successfully", load));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<LoadDTO>>> getLoads(
            @RequestParam(required = false) String shipperId,
            @RequestParam(required = false) Load.LoadStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LoadDTO> loads = loadService.getLoads(shipperId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(loads));
    }
    
    @GetMapping("/{loadId}")
    public ResponseEntity<ApiResponse<LoadDTO>> getLoadById(@PathVariable UUID loadId) {
        LoadDTO load = loadService.getLoadById(loadId);
        return ResponseEntity.ok(ApiResponse.success(load));
    }
    
    @PatchMapping("/{loadId}/cancel")
    public ResponseEntity<ApiResponse<LoadDTO>> cancelLoad(@PathVariable UUID loadId) {
        LoadDTO load = loadService.cancelLoad(loadId);
        return ResponseEntity.ok(ApiResponse.success("Load cancelled successfully", load));
    }
    
    @GetMapping("/{loadId}/best-bids")
    public ResponseEntity<ApiResponse<List<BidDTO>>> getBestBids(@PathVariable UUID loadId) {
        List<BidDTO> bids = loadService.getBestBids(loadId);
        return ResponseEntity.ok(ApiResponse.success(bids));
    }
}

