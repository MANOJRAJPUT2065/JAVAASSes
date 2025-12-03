package com.cargopro.tms.controller;

import com.cargopro.tms.dto.*;
import com.cargopro.tms.entity.Bid;
import com.cargopro.tms.service.BidService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bid")
public class BidController {
    
    private final BidService bidService;
    
    public BidController(BidService bidService) {
        this.bidService = bidService;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<BidDTO>> submitBid(@Valid @RequestBody CreateBidRequest request) {
        BidDTO bid = bidService.submitBid(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bid submitted successfully", bid));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<BidDTO>>> getBids(
            @RequestParam(required = false) UUID loadId,
            @RequestParam(required = false) UUID transporterId,
            @RequestParam(required = false) Bid.BidStatus status) {
        List<BidDTO> bids = bidService.getBids(loadId, transporterId, status);
        return ResponseEntity.ok(ApiResponse.success(bids));
    }
    
    @GetMapping("/{bidId}")
    public ResponseEntity<ApiResponse<BidDTO>> getBidById(@PathVariable UUID bidId) {
        BidDTO bid = bidService.getBidById(bidId);
        return ResponseEntity.ok(ApiResponse.success(bid));
    }
    
    @PatchMapping("/{bidId}/reject")
    public ResponseEntity<ApiResponse<BidDTO>> rejectBid(@PathVariable UUID bidId) {
        BidDTO bid = bidService.rejectBid(bidId);
        return ResponseEntity.ok(ApiResponse.success("Bid rejected successfully", bid));
    }
}

