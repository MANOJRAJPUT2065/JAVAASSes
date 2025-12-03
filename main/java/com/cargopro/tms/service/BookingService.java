package com.cargopro.tms.service;

import com.cargopro.tms.dto.*;
import com.cargopro.tms.entity.Bid;
import com.cargopro.tms.entity.Booking;
import com.cargopro.tms.entity.Load;
import com.cargopro.tms.exception.InsufficientCapacityException;
import com.cargopro.tms.exception.InvalidStatusTransitionException;
import com.cargopro.tms.exception.LoadAlreadyBookedException;
import com.cargopro.tms.exception.ResourceNotFoundException;
import com.cargopro.tms.repository.BookingRepository;
import com.cargopro.tms.repository.LoadRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final LoadRepository loadRepository;
    private final BidService bidService;
    private final LoadService loadService;
    private final TransporterService transporterService;
    
    public BookingService(BookingRepository bookingRepository, LoadRepository loadRepository,
                          BidService bidService, LoadService loadService, 
                          TransporterService transporterService) {
        this.bookingRepository = bookingRepository;
        this.loadRepository = loadRepository;
        this.bidService = bidService;
        this.loadService = loadService;
        this.transporterService = transporterService;
    }
    
    public BookingDTO createBooking(CreateBookingRequest request) {
        Bid bid = bidService.getBidEntity(request.getBidId());
        
        if (bid.getStatus() != Bid.BidStatus.PENDING) {
            throw new InvalidStatusTransitionException("Can only accept PENDING bids");
        }
        
        Load load = bid.getLoad();
        
        // Check load status
        if (load.getStatus() == Load.LoadStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Cannot book a CANCELLED load");
        }
        
        // Check remaining capacity
        Integer totalAllocated = bookingRepository.getTotalAllocatedTrucks(load.getLoadId());
        int remainingTrucks = load.getNoOfTrucks() - (totalAllocated != null ? totalAllocated : 0);
        
        if (bid.getTrucksOffered() > remainingTrucks) {
            throw new InsufficientCapacityException(
                    String.format("Insufficient capacity. Remaining: %d, Requested: %d", 
                            remainingTrucks, bid.getTrucksOffered()));
        }
        
        // Check transporter capacity
        Integer availableTrucks = transporterService.getAvailableTruckCount(
                bid.getTransporter().getTransporterId(), load.getTruckType());
        if (bid.getTrucksOffered() > availableTrucks) {
            throw new InsufficientCapacityException(
                    String.format("Transporter has insufficient capacity. Available: %d, Requested: %d", 
                            availableTrucks, bid.getTrucksOffered()));
        }
        
        // Optimistic locking - reload load to get latest version
        try {
            Load freshLoad = loadRepository.findById(load.getLoadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Load not found"));
            
            // Double-check capacity after reload
            Integer freshTotalAllocated = bookingRepository.getTotalAllocatedTrucks(freshLoad.getLoadId());
            int freshRemainingTrucks = freshLoad.getNoOfTrucks() - (freshTotalAllocated != null ? freshTotalAllocated : 0);
            
            if (bid.getTrucksOffered() > freshRemainingTrucks) {
                throw new LoadAlreadyBookedException(
                        "Load capacity was allocated by another transaction. Please refresh and try again.");
            }
            
            // Create booking
            Booking booking = new Booking();
            booking.setLoad(freshLoad);
            booking.setBid(bid);
            booking.setTransporter(bid.getTransporter());
            booking.setAllocatedTrucks(bid.getTrucksOffered());
            booking.setFinalRate(bid.getProposedRate());
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            booking.setBookedAt(LocalDateTime.now());
            
            Booking savedBooking = bookingRepository.save(booking);
            
            // Update bid status
            bidService.acceptBid(bid);
            
            // Deduct trucks from transporter
            transporterService.deductTrucks(
                    bid.getTransporter().getTransporterId(), 
                    load.getTruckType(), 
                    bid.getTrucksOffered());
            
            // Update load status
            loadService.updateLoadStatus(freshLoad.getLoadId());
            
            return convertToDTO(savedBooking);
            
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new LoadAlreadyBookedException(
                    "Load was modified by another transaction. Please refresh and try again.");
        }
    }
    
    public BookingDTO getBookingById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        return convertToDTO(booking);
    }
    
    public BookingDTO cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Booking is already CANCELLED");
        }
        
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("Cannot cancel a COMPLETED booking");
        }
        
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);
        
        // Restore trucks to transporter
        transporterService.restoreTrucks(
                booking.getTransporter().getTransporterId(),
                booking.getLoad().getTruckType(),
                booking.getAllocatedTrucks());
        
        // Update load status - check if still has capacity
        loadService.updateLoadStatus(booking.getLoad().getLoadId());
        
        return convertToDTO(savedBooking);
    }
    
    private BookingDTO convertToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setBookingId(booking.getBookingId());
        dto.setLoadId(booking.getLoad().getLoadId());
        dto.setBidId(booking.getBid().getBidId());
        dto.setTransporterId(booking.getTransporter().getTransporterId());
        dto.setTransporterCompanyName(booking.getTransporter().getCompanyName());
        dto.setAllocatedTrucks(booking.getAllocatedTrucks());
        dto.setFinalRate(booking.getFinalRate());
        dto.setStatus(booking.getStatus());
        dto.setBookedAt(booking.getBookedAt());
        return dto;
    }
}

