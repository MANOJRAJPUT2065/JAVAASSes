# TMS Backend - Testing Guide

## Unit Tests

### Running Unit Tests
```bash
mvn clean test
```

### Test Coverage
- Service Layer Tests: Tests for all business logic including capacity validation, status transitions, and multi-truck allocation
- Repository Tests: Tests for custom query methods
- Exception Handling Tests: Tests for all custom exceptions

## Integration Tests

### Test Scenarios

#### 1. Load Creation and Bid Process
- Create a load (POST /load)
- Submit bids from multiple transporters (POST /bid)
- Verify load status transitions from POSTED -> OPEN_FOR_BIDS

#### 2. Capacity Validation
- Create a transporter with limited truck capacity
- Submit bid exceeding available trucks -> Should fail with InsufficientCapacityException
- Submit bid within capacity -> Should succeed

#### 3. Multi-Truck Allocation
- Create load requiring 3 trucks
- Accept first booking for 2 trucks (load still OPEN_FOR_BIDS)
- Accept second booking for 1 truck (load becomes BOOKED)
- Verify remaining trucks = 0

#### 4. Optimistic Locking
- Simulate concurrent booking attempts
- First transaction should succeed
- Second transaction should fail with LoadAlreadyBookedException (409 Conflict)

#### 5. Booking Cancellation
- Create booking and confirm
- Verify trucks deducted from transporter
- Cancel booking
- Verify trucks restored to transporter
- Verify load status reverts to OPEN_FOR_BIDS if partially allocated

#### 6. Best Bid Scoring
- Submit bids with different rates and from transporters with different ratings
- Retrieve best bids (GET /load/{loadId}/best-bids)
- Verify sorting by score: (1/rate) * 0.7 + (rating/5) * 0.3

## Manual Testing with Postman/cURL

### 1. Create Transporter
```bash
curl -X POST http://localhost:8080/transporter \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "ABC Transport",
    "rating": 4.5,
    "availableTrucks": [{"truckType": "Container", "count": 10}]
  }'
```

### 2. Create Load
```bash
curl -X POST http://localhost:8080/load \
  -H "Content-Type: application/json" \
  -d '{
    "shipperId": "SHIPPER001",
    "loadingCity": "Mumbai",
    "unloadingCity": "Delhi",
    "loadingDate": "2024-02-15T10:00:00",
    "productType": "Electronics",
    "weight": 5000.0,
    "weightUnit": "KG",
    "truckType": "Container",
    "noOfTrucks": 2
  }'
```

### 3. Submit Bid
```bash
curl -X POST http://localhost:8080/bid \
  -H "Content-Type: application/json" \
  -d '{
    "loadId": "<LOAD_ID>",
    "transporterId": "<TRANSPORTER_ID>",
    "proposedRate": 50000.0,
    "trucksOffered": 2
  }'
```

### 4. Get Best Bids
```bash
curl http://localhost:8080/load/<LOAD_ID>/best-bids
```

### 5. Create Booking
```bash
curl -X POST http://localhost:8080/booking \
  -H "Content-Type: application/json" \
  -d '{"bidId": "<BID_ID>"}'
```

## Expected Responses

### Success Response (200/201)
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

### Error Response (400/409/404)
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

## Status Codes
- 200: OK
- 201: Created
- 400: Bad Request (validation errors, status transition errors)
- 404: Not Found
- 409: Conflict (optimistic locking failure, load already booked)
- 500: Internal Server Error

## Key Business Rules to Test

1. ✅ Cannot bid on CANCELLED or already BOOKED loads
2. ✅ Cannot cancel a BOOKED load
3. ✅ Truck capacity must be validated before booking
4. ✅ Trucks must be deducted when booking confirmed
5. ✅ Trucks must be restored when booking cancelled
6. ✅ Load status: POSTED → OPEN_FOR_BIDS → BOOKED
7. ✅ Optimistic locking prevents double-booking
8. ✅ Multi-truck loads can have multiple partial bookings
9. ✅ Best bids calculated correctly with score formula
