# Transport Management System (TMS) – Architecture Documentation

## 1. System Overview

The Transport Management System (TMS) is a Spring Boot + PostgreSQL backend application designed to manage logistics operations for loads, transporters, bids, and bookings with strict business rules enforcement.

### Key Features
- **Load lifecycle management**: POSTED → OPEN_FOR_BIDS → BOOKED → CANCELLED
- **Capacity-based bidding**: Transporters can only bid if they have sufficient truck capacity
- **Multi-truck allocation**: Multiple bookings allowed until load is fully allocated
- **Best bid scoring**: Combines rate and transporter rating for smart bid suggestions
- **Optimistic locking**: Prevents double-booking using @Version annotations

---

## 2. High-Level Architecture

### 2.1 Layered Architecture Pattern

```
┌─────────────────────────────────────────────────────┐
│               Controller Layer                       │
│  (HTTP Endpoints, DTOs, Request/Response Mapping)   │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│                Service Layer                         │
│   (Business Logic, Validation, Rule Enforcement)    │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│              Repository Layer                        │
│        (Data Access, JPA Queries, DB Ops)           │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│               Entity Layer                           │
│         (JPA Entities, Relationships, @Version)     │
└─────────────────────────────────────────────────────┘
```

### 2.2 Cross-Cutting Concerns
- **Global Exception Handling**: @ControllerAdvice for consistent error responses
- **Validation**: @Valid annotations on DTOs with Bean Validation
- **Transaction Management**: @Transactional on service methods
- **API Documentation**: Springdoc OpenAPI 3 (Swagger UI)

---

## 3. Package Structure & File Organization

```
src/main/java/com/cargopro/tms/
├── controller/
│   ├── LoadController.java
│   ├── TransporterController.java
│   ├── BidController.java
│   └── BookingController.java
├── dto/
│   ├── request/
│   │   ├── CreateLoadRequest.java
│   │   ├── CreateBidRequest.java
│   │   ├── CreateBookingRequest.java
│   │   └── UpdateTransporterTrucksRequest.java
│   └── response/
│       ├── LoadResponse.java
│       ├── BidResponse.java
│       ├── BestBidResponse.java
│       └── BookingResponse.java
├── service/
│   ├── LoadService.java
│   ├── TransporterService.java
│   ├── BidService.java
│   └── BookingService.java
├── repository/
│   ├── LoadRepository.java
│   ├── TransporterRepository.java
│   ├── TransporterTruckCapacityRepository.java
│   ├── BidRepository.java
│   └── BookingRepository.java
├── entity/
│   ├── Load.java
│   ├── Transporter.java
│   ├── TransporterTruckCapacity.java
│   ├── Bid.java
│   └── Booking.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── InvalidStatusTransitionException.java
│   ├── InsufficientCapacityException.java
│   ├── LoadAlreadyBookedException.java
│   └── ResourceNotFoundException.java
└── TmsApplication.java
```

---

## 4. Core Components Detailed Explanation

### 4.1 Controller Layer

**Responsibility**:
- Map HTTP requests to service methods
- Validate incoming DTOs using @Valid
- Transform service responses to DTOs
- Handle HTTP status codes

**Key Files**:

#### LoadController.java
- `POST /load` – Create new load (status = POSTED)
- `GET /load` – List loads with filtering and pagination
- `GET /load/{loadId}` – Get load details with active bids
- `PATCH /load/{loadId}/cancel` – Cancel load (status validation)
- `GET /load/{loadId}/best-bids` – Get sorted bid suggestions

#### TransporterController.java
- `POST /transporter` – Register new transporter with truck capacities
- `GET /transporter/{transporterId}` – Get transporter details
- `PUT /transporter/{transporterId}/trucks` – Update available trucks

#### BidController.java
- `POST /bid` – Submit bid (capacity and status validation)
- `GET /bid` – Filter bids by loadId, transporterId, status
- `GET /bid/{bidId}` – Get bid details
- `PATCH /bid/{bidId}/reject` – Reject bid

#### BookingController.java
- `POST /booking` – Accept bid, create booking, handle concurrency
- `GET /booking/{bookingId}` – Get booking details
- `PATCH /booking/{bookingId}/cancel` – Cancel booking, restore capacity

---

### 4.2 DTO Layer

**Responsibility**:
- Decouple API contract from internal entity structure
- Apply validation constraints (@NotNull, @Positive, @Min, @Max)
- Transform data for client consumption

**Key Patterns**:
- Request DTOs contain only fields needed for creation/update
- Response DTOs may include derived fields (e.g., remainingTrucks)
- Nested DTOs for complex structures (e.g., availableTrucks list)

---

### 4.3 Service Layer

**Responsibility**:
- **Core business logic enforcement**
- **Transaction boundaries** (@Transactional)
- **Rule validation** before persistence
- **Orchestration** between multiple repositories

**Key Files**:

#### LoadService.java
- `createLoad()`: Create load with status POSTED
- `getLoadDetails()`: Fetch load with active bids
- `cancelLoad()`: Validate status (cannot cancel if BOOKED)
- `getBestBids()`: Calculate score = (1/proposedRate) * 0.7 + (rating/5) * 0.3 and sort
- `calculateRemainingTrucks()`: noOfTrucks - SUM(allocatedTrucks from CONFIRMED bookings)

#### TransporterService.java
- `registerTransporter()`: Create transporter with initial truck capacities
- `updateTruckCapacity()`: Modify available trucks per truck type
- `validateCapacity()`: Check if trucksOffered <= available count

#### BidService.java
- `submitBid()`:
  1. Validate load status (not CANCELLED or BOOKED)
  2. Validate transporter capacity
  3. Create bid with PENDING status
  4. If first bid → update load status to OPEN_FOR_BIDS

- `rejectBid()`: Update bid status to REJECTED

#### BookingService.java
- `createBooking()`:
  1. Fetch load (optimistic lock with @Version)
  2. Validate remainingTrucks >= allocatedTrucks
  3. Deduct allocatedTrucks from transporter capacity
  4. Create booking with status CONFIRMED
  5. If remainingTrucks == 0 → update load status to BOOKED
  6. Update bid status to ACCEPTED
  7. Handle OptimisticLockException → throw LoadAlreadyBookedException

- `cancelBooking()`:
  1. Validate booking exists and is cancellable
  2. Restore allocatedTrucks to transporter capacity
  3. Update booking status to CANCELLED
  4. Recalculate load status (if all bookings cancelled → OPEN_FOR_BIDS or POSTED)

---

### 4.4 Repository Layer

**Responsibility**:
- Extend JpaRepository<Entity, UUID>
- Define custom query methods using Spring Data JPA naming conventions
- Execute JPQL/Native queries for complex filters

**Key Files**:

- **LoadRepository.java**
  - `findByShipperId(String shipperId, Pageable pageable)`
  - `findByStatusAndShipperId(...)`
  - Custom query for loads with active bids

- **TransporterRepository.java**
  - Standard CRUD operations

- **TransporterTruckCapacityRepository.java**
  - `findByTransporterIdAndTruckType(UUID transporterId, String truckType)`
  - Update capacity methods

- **BidRepository.java**
  - `findByLoadId(UUID loadId)`
  - `findByTransporterIdAndLoadId(...)`
  - `findByStatus(BidStatus status)`
  - Custom query for best bids with transporter rating

- **BookingRepository.java**
  - `findByLoadIdAndStatus(UUID loadId, BookingStatus status)`
  - `sumAllocatedTrucksByLoadId(UUID loadId)`

---

### 4.5 Entity Layer

**Responsibility**:
- Map Java objects to database tables using JPA annotations
- Define relationships (@OneToMany, @ManyToOne)
- Implement optimistic locking (@Version)
- Enforce constraints at entity level

**Key Entities**:

#### Load.java
```java
@Entity
@Table(name = "load", indexes = {
    @Index(name = "idx_load_shipper_status", columnList = "shipper_id, status")
})
public class Load {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    private String shipperId;
    private String loadingCity;
    private String unloadingCity;
    private LocalDateTime loadingDate;
    private String productType;
    private Double weight;
    
    @Enumerated(EnumType.STRING)
    private WeightUnit weightUnit; // KG, TON
    
    private String truckType;
    private Integer noOfTrucks;
    
    @Enumerated(EnumType.STRING)
    private LoadStatus status; // POSTED, OPEN_FOR_BIDS, BOOKED, CANCELLED
    
    private LocalDateTime datePosted;
    
    @Version
    private Long version; // Optimistic locking
    
    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL)
    private List<Bid> bids = new ArrayList<>();
    
    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL)
    private List<Booking> bookings = new ArrayList<>();
}
```

#### Transporter.java
```java
@Entity
@Table(name = "transporter")
public class Transporter {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    private String companyName;
    
    @Min(1)
    @Max(5)
    private Double rating;
    
    @OneToMany(mappedBy = "transporter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransporterTruckCapacity> capacities = new ArrayList<>();
    
    @OneToMany(mappedBy = "transporter")
    private List<Bid> bids = new ArrayList<>();
    
    @OneToMany(mappedBy = "transporter")
    private List<Booking> bookings = new ArrayList<>();
}
```

#### TransporterTruckCapacity.java
```java
@Entity
@Table(name = "transporter_truck_capacity", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"transporter_id", "truck_type"}))
public class TransporterTruckCapacity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;
    
    private String truckType;
    private Integer count; // Available trucks
}
```

#### Bid.java
```java
@Entity
@Table(name = "bid", indexes = {
    @Index(name = "idx_bid_load", columnList = "load_id"),
    @Index(name = "idx_bid_transporter", columnList = "transporter_id"),
    @Index(name = "idx_bid_status", columnList = "status")
})
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id", nullable = false)
    private Load load;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;
    
    private Double proposedRate;
    private Integer trucksOffered;
    
    @Enumerated(EnumType.STRING)
    private BidStatus status; // PENDING, ACCEPTED, REJECTED
    
    private LocalDateTime submittedAt;
}
```

**Business Constraint**: Only one ACCEPTED bid per load (enforced in service layer + database unique index where supported).

#### Booking.java
```java
@Entity
@Table(name = "booking", indexes = {
    @Index(name = "idx_booking_load", columnList = "load_id"),
    @Index(name = "idx_booking_transporter", columnList = "transporter_id"),
    @Index(name = "idx_booking_status", columnList = "status")
})
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id", nullable = false)
    private Load load;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false, unique = true)
    private Bid bid;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;
    
    private Integer allocatedTrucks;
    private Double finalRate;
    
    @Enumerated(EnumType.STRING)
    private BookingStatus status; // CONFIRMED, COMPLETED, CANCELLED
    
    private LocalDateTime bookedAt;
}
```

---

### 4.6 Exception Layer

**Responsibility**:
- Define custom business exceptions
- Map exceptions to HTTP status codes
- Provide consistent error responses to clients

**Key Files**:

#### GlobalExceptionHandler.java
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        ErrorResponse error = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InsufficientCapacityException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCapacity(InsufficientCapacityException ex) {
        // Similar pattern, return 400
    }

    @ExceptionHandler(LoadAlreadyBookedException.class)
    public ResponseEntity<ErrorResponse> handleLoadAlreadyBooked(LoadAlreadyBookedException ex) {
        // Return 409 CONFLICT
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        // Return 404 NOT FOUND
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        // Wrap into LoadAlreadyBookedException and return 409
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Return 500 INTERNAL SERVER ERROR with safe message
    }
}
```

#### Custom Exceptions

- **InvalidStatusTransitionException**: Thrown when load status transition violates business rules (e.g., canceling BOOKED load)
- **InsufficientCapacityException**: Thrown when transporter doesn't have enough trucks to fulfill bid
- **LoadAlreadyBookedException**: Thrown when concurrent booking attempt fails due to optimistic locking
- **ResourceNotFoundException**: Thrown when load/bid/booking/transporter not found by ID

---
