package com.cargopro.tms.service;

import com.cargopro.tms.dto.*;
import com.cargopro.tms.entity.Bid;
import com.cargopro.tms.entity.Load;
import com.cargopro.tms.entity.Transporter;
import com.cargopro.tms.exception.InsufficientCapacityException;
import com.cargopro.tms.exception.InvalidStatusTransitionException;
import com.cargopro.tms.exception.ResourceNotFoundException;
import com.cargopro.tms.repository.BidRepository;
import com.cargopro.tms.repository.LoadRepository;
import com.cargopro.tms.repository.TransporterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BidService {
    
    private final BidRepository bidRepository;
    private final LoadRepository loadRepository;
    private final TransporterRepository transporterRepository;
    private final LoadService loadService;
    
    public BidService(BidRepository bidRepository, LoadRepository loadRepository,
                     TransporterRepository transporterRepository, LoadService loadService) {
        this.bidRepository = bidRepository;
        this.loadRepository = loadRepository;
        this.transporterRepository = transporterRepository;
        this.loadService = loadService;
    }
    
    public BidDTO submitBid(CreateBidRequest request) {
        Load load = loadRepository.findById(request.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load not found with id: " + request.getLoadId()));
        
        Transporter transporter = transporterRepository.findById(request.getTransporterId())
                .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + request.getTransporterId()));
        
        // Validate load status
        if (load.getStatus() == Load.LoadStatus.CANCELLED || load.getStatus() == Load.LoadStatus.BOOKED) {
            throw new InvalidStatusTransitionException("Cannot bid on a " + load.getStatus() + " load");
        }
        
        // Validate capacity
        Integer availableTrucks = transporter.getAvailableTruckCount(load.getTruckType());
        if (request.getTrucksOffered() > availableTrucks) {
            throw new InsufficientCapacityException(
                    String.format("Insufficient capacity. Available: %d, Requested: %d", 
                            availableTrucks, request.getTrucksOffered()));
        }
        
        Bid bid = new Bid();
        bid.setLoad(load);
        bid.setTransporter(transporter);
        bid.setProposedRate(request.getProposedRate());
        bid.setTrucksOffered(request.getTrucksOffered());
        bid.setStatus(Bid.BidStatus.PENDING);
        bid.setSubmittedAt(LocalDateTime.now());
        
        Bid savedBid = bidRepository.save(bid);
        
        // Update load status to OPEN_FOR_BIDS if first bid
        loadService.updateLoadStatus(load.getLoadId());
        
        return convertToDTO(savedBid);
    }
    
    public List<BidDTO> getBids(UUID loadId, UUID transporterId, Bid.BidStatus status) {
        List<Bid> bids;
        
        if (loadId != null && transporterId != null && status != null) {
            bids = bidRepository.findByLoad_LoadIdAndTransporter_TransporterId(loadId, transporterId);
            bids = bids.stream()
                    .filter(b -> b.getStatus() == status)
                    .collect(Collectors.toList());
        } else if (loadId != null && transporterId != null) {
            bids = bidRepository.findByLoad_LoadIdAndTransporter_TransporterId(loadId, transporterId);
        } else if (loadId != null && status != null) {
            bids = bidRepository.findByLoad_LoadIdAndStatus(loadId, status);
        } else if (transporterId != null && status != null) {
            bids = bidRepository.findByTransporter_TransporterIdAndStatus(transporterId, status);
        } else if (loadId != null) {
            bids = bidRepository.findByLoad_LoadId(loadId);
        } else if (transporterId != null) {
            bids = bidRepository.findByTransporter_TransporterId(transporterId);
        } else if (status != null) {
            bids = bidRepository.findAll().stream()
                    .filter(b -> b.getStatus() == status)
                    .collect(Collectors.toList());
        } else {
            bids = bidRepository.findAll();
        }
        
        return bids.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public BidDTO getBidById(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found with id: " + bidId));
        return convertToDTO(bid);
    }
    
    public BidDTO rejectBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found with id: " + bidId));
        
        if (bid.getStatus() != Bid.BidStatus.PENDING) {
            throw new InvalidStatusTransitionException("Can only reject PENDING bids");
        }
        
        bid.setStatus(Bid.BidStatus.REJECTED);
        Bid savedBid = bidRepository.save(bid);
        return convertToDTO(savedBid);
    }
    
    public Bid getBidEntity(UUID bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found with id: " + bidId));
    }
    
    public void acceptBid(Bid bid) {
        bid.setStatus(Bid.BidStatus.ACCEPTED);
        bidRepository.save(bid);
    }
    
    private BidDTO convertToDTO(Bid bid) {
        BidDTO dto = new BidDTO();
        dto.setBidId(bid.getBidId());
        dto.setLoadId(bid.getLoad().getLoadId());
        dto.setTransporterId(bid.getTransporter().getTransporterId());
        dto.setTransporterCompanyName(bid.getTransporter().getCompanyName());
        dto.setTransporterRating(bid.getTransporter().getRating());
        dto.setProposedRate(bid.getProposedRate());
        dto.setTrucksOffered(bid.getTrucksOffered());
        dto.setStatus(bid.getStatus());
        dto.setSubmittedAt(bid.getSubmittedAt());
        return dto;
    }
}

