package com.cargopro.tms.service;

import com.cargopro.tms.dto.*;
import com.cargopro.tms.entity.TruckAvailability;
import com.cargopro.tms.entity.Transporter;
import com.cargopro.tms.exception.ResourceNotFoundException;
import com.cargopro.tms.repository.TransporterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransporterService {
    
    private final TransporterRepository transporterRepository;
    
    public TransporterService(TransporterRepository transporterRepository) {
        this.transporterRepository = transporterRepository;
    }
    
    public TransporterDTO createTransporter(CreateTransporterRequest request) {
        Transporter transporter = new Transporter();
        transporter.setCompanyName(request.getCompanyName());
        transporter.setRating(request.getRating());
        
        List<TruckAvailability> availabilities = request.getAvailableTrucks().stream()
                .map(dto -> {
                    TruckAvailability availability = new TruckAvailability();
                    availability.setTransporter(transporter);
                    availability.setTruckType(dto.getTruckType());
                    availability.setCount(dto.getCount());
                    return availability;
                })
                .collect(Collectors.toList());
        
        transporter.setAvailableTrucks(availabilities);
        
        Transporter savedTransporter = transporterRepository.save(transporter);
        return convertToDTO(savedTransporter);
    }
    
    public TransporterDTO getTransporterById(UUID transporterId) {
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + transporterId));
        return convertToDTO(transporter);
    }
    
    public TransporterDTO updateTrucks(UUID transporterId, UpdateTrucksRequest request) {
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + transporterId));
        
        // Clear existing trucks
        transporter.getAvailableTrucks().clear();
        
        // Add new trucks
        List<TruckAvailability> availabilities = request.getAvailableTrucks().stream()
                .map(dto -> {
                    TruckAvailability availability = new TruckAvailability();
                    availability.setTransporter(transporter);
                    availability.setTruckType(dto.getTruckType());
                    availability.setCount(dto.getCount());
                    return availability;
                })
                .collect(Collectors.toList());
        
        transporter.setAvailableTrucks(availabilities);
        
        Transporter savedTransporter = transporterRepository.save(transporter);
        return convertToDTO(savedTransporter);
    }
    
    public Integer getAvailableTruckCount(UUID transporterId, String truckType) {
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + transporterId));
        return transporter.getAvailableTruckCount(truckType);
    }
    
    public void deductTrucks(UUID transporterId, String truckType, int count) {
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + transporterId));
        transporter.updateTruckAvailability(truckType, -count);
        transporterRepository.save(transporter);
    }
    
    public void restoreTrucks(UUID transporterId, String truckType, int count) {
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with id: " + transporterId));
        transporter.updateTruckAvailability(truckType, count);
        transporterRepository.save(transporter);
    }
    
    private TransporterDTO convertToDTO(Transporter transporter) {
        TransporterDTO dto = new TransporterDTO();
        dto.setTransporterId(transporter.getTransporterId());
        dto.setCompanyName(transporter.getCompanyName());
        dto.setRating(transporter.getRating());
        
        List<TruckAvailabilityDTO> truckDTOs = transporter.getAvailableTrucks().stream()
                .map(ta -> new TruckAvailabilityDTO(ta.getTruckType(), ta.getCount()))
                .collect(Collectors.toList());
        
        dto.setAvailableTrucks(truckDTOs);
        return dto;
    }
}

