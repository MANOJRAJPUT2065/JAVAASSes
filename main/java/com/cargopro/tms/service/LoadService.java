package com.cargopro.tms.service;

import com.cargopro.tms.dto.*;
import com.cargopro.tms.entity.Bid;
import com.cargopro.tms.entity.Booking;
import com.cargopro.tms.entity.Load;
import com.cargopro.tms.exception.InvalidStatusTransitionException;
import com.cargopro.tms.exception.ResourceNotFoundException;
import com.cargopro.tms.repository.BidRepository;
import com.cargopro.tms.repository.BookingRepository;
import com.cargopro.tms.repository.LoadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoadService {
    
    private final LoadRepository loadRepository;
    private final BidRepository bidRepository;
    private final BookingRepository bookingRepository;
    
    public LoadService(LoadRepository loadRepository, BidRepository bidRepository, 
                      BookingRepository bookingRepository) {
        this.loadRepository = loadRepository;
        this.bidRepository = bidRepository;
        this.bookingRepository = bookingRepository;
    }
    
    public LoadDTO createLoad(CreateLoadRequest request) {
        Load load = new Load();
        load.setShipperId(request.getShipperId());
        load.setLoadingCity(request.getLoadingCity());
        load.setUnloadingCity(request.getUnloadingCity());
        load.setLoadingDate(request.getLoadingDate());
        load.setProductType(request.getProductType());
        load.setWeight(request.getWeight());
        load.setWeightUnit(request.getWeightUnit());
        load.setTruckType(request.getTruckType());
        load.setNoOfTrucks(request.getNoOfTrucks());
        load.setStatus(Load.LoadStatus.POSTED);
        load.setDatePosted(LocalDateTime.now());
        
        Load savedLoad = loadRepository.save(load);
        return convertToDTO(savedLoad);
    }
    
    public Page<LoadDTO> getLoads(String shipperId, Load.LoadStatus status, Pageable pageable) {
        Page<Load> loads;
        
        if (shipperId != null && status != null) {
            loads = loadRepository.findByShipperIdAndStatus(shipperId, status, pageable);
        } else if (shipperId != null) {
            loads = loadRepository.findByShipperId(shipperId, pageable);
        } else if (status != null) {
            loads = loadRepository.findByStatus(status, pageable);
        } else {
            loads = loadRepository.findAll(pageable);
        }
        
        return loads.map(this::convertToDTO);
    }
    
    public LoadDTO getLoadById(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found with id: " + loadId));
        
        LoadDTO dto = convertToDTO(load);
        
        // Get active bids
        List<Bid> activeBids = bidRepository.findActiveBidsByLoadId(loadId);
        dto.setActiveBids(activeBids.stream()
                .map(this::convertBidToDTO)
                .collect(Collectors.toList()));
        
        // Calculate remaining trucks
        Integer totalAllocated = bookingRepository.getTotalAllocatedTrucks(loadId);
        dto.setRemainingTrucks(load.getNoOfTrucks() - totalAllocated);
        
        return dto;
    }
    
    public LoadDTO cancelLoad(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found with id: " + loadId));
        
        if (load.getStatus() == Load.LoadStatus.BOOKED) {
            throw new InvalidStatusTransitionException("Cannot cancel a load that is already BOOKED");
        }
        
        if (load.getStatus() == Load.LoadStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Load is already CANCELLED");
        }
        
        load.setStatus(Load.LoadStatus.CANCELLED);
        Load savedLoad = loadRepository.save(load);
        return convertToDTO(savedLoad);
    }
    
    public List<BidDTO> getBestBids(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found with id: " + loadId));
        
        List<Bid> activeBids = bidRepository.findActiveBidsByLoadId(loadId);
        
        return activeBids.stream()
                .map(bid -> {
                    BidDTO dto = convertBidToDTO(bid);
                    // Calculate bid score: (1 / proposedRate) * 0.7 + (rating / 5) * 0.3
                    double rateScore = (1.0 / bid.getProposedRate()) * 0.7;
                    double ratingScore = (bid.getTransporter().getRating() / 5.0) * 0.3;
                    dto.setBidScore(rateScore + ratingScore);
                    return dto;
                })
                .sorted((b1, b2) -> Double.compare(b2.getBidScore(), b1.getBidScore())) // Higher score = better
                .collect(Collectors.toList());
    }
    
    public void updateLoadStatus(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found with id: " + loadId));
        
        // Check if first bid received
        if (load.getStatus() == Load.LoadStatus.POSTED) {
            long activeBidCount = bidRepository.findActiveBidsByLoadId(loadId).size();
            if (activeBidCount > 0) {
                load.setStatus(Load.LoadStatus.OPEN_FOR_BIDS);
                loadRepository.save(load);
            }
        }
        
        // Check if fully booked
        Integer totalAllocated = bookingRepository.getTotalAllocatedTrucks(loadId);
        if (totalAllocated != null && totalAllocated >= load.getNoOfTrucks()) {
            if (load.getStatus() != Load.LoadStatus.BOOKED) {
                load.setStatus(Load.LoadStatus.BOOKED);
                loadRepository.save(load);
            }
        }
    }
    
    private LoadDTO convertToDTO(Load load) {
        LoadDTO dto = new LoadDTO();
        dto.setLoadId(load.getLoadId());
        dto.setShipperId(load.getShipperId());
        dto.setLoadingCity(load.getLoadingCity());
        dto.setUnloadingCity(load.getUnloadingCity());
        dto.setLoadingDate(load.getLoadingDate());
        dto.setProductType(load.getProductType());
        dto.setWeight(load.getWeight());
        dto.setWeightUnit(load.getWeightUnit());
        dto.setTruckType(load.getTruckType());
        dto.setNoOfTrucks(load.getNoOfTrucks());
        dto.setStatus(load.getStatus());
        dto.setDatePosted(load.getDatePosted());
        return dto;
    }
    
    private BidDTO convertBidToDTO(Bid bid) {
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

