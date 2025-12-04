# Transport Management System (TMS) - Backend

A comprehensive backend system for managing transportation loads, transporters, bids, and bookings built with Spring Boot 3.2+ and PostgreSQL.

## 🚀 Tech Stack

- **Java**: 17+
- **Spring Boot**: 3.2.0
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Build Tool**: Maven

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- IDE (IntelliJ IDEA / Eclipse / VS Code)

## 🛠️ Setup Instructions

### 1. Database Setup

```sql
-- Create database
CREATE DATABASE tms_db;

-- Connect to database
\c tms_db;
```

### 2. Configure Application

Update `src/main/resources/application.properties` with your PostgreSQL credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tms_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Build and Run

```bash
# Navigate to project directory
cd tms-backend

# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access API Documentation

Once the application is running, access Swagger UI at:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs JSON**: http://localhost:8080/api-docs

## 📊 Database Schema

### Entity Relationship Diagram

```
┌─────────────────┐         ┌──────────────────┐
│      Load       │         │   Transporter    │
├─────────────────┤         ├──────────────────┤
│ loadId (PK)     │         │ transporterId(PK)│
│ shipperId       │         │ companyName      │
│ loadingCity     │         │ rating           │
│ unloadingCity   │         └──────────────────┘
│ loadingDate     │                  │
│ productType     │                  │
│ weight          │                  │
│ weightUnit      │                  │
│ truckType       │                  │
│ noOfTrucks      │                  │
│ status          │                  │
│ datePosted      │                  │
│ version (Lock)  │                  │
└─────────────────┘                  │
       │                              │
       │                              │
       │ 1:N                          │ 1:N
       │                              │
       ▼                              ▼
┌─────────────────┐         ┌──────────────────┐
│      Bid        │         │ TruckAvailability │
├─────────────────┤         ├──────────────────┤
│ bidId (PK)      │         │ availabilityId(PK)│
│ loadId (FK)     │         │ transporterId(FK) │
│ transporterId(FK)│         │ truckType         │
│ proposedRate    │         │ count             │
│ trucksOffered   │         └──────────────────┘
│ status          │
│ submittedAt     │
└─────────────────┘
       │
       │ 1:1
       │
       ▼
┌─────────────────┐
│    Booking      │
├─────────────────┤
│ bookingId (PK)  │
│ loadId (FK)     │
│ bidId (FK, UNIQUE)│
│ transporterId(FK)│
│ allocatedTrucks │
│ finalRate       │
│ status          │
│ bookedAt        │
└─────────────────┘
```

### Key Constraints

- **Foreign Keys**: All relationships properly defined with CASCADE operations
- **Unique Constraint**: One ACCEPTED bid per load (enforced via bidId uniqueness in Booking)
- **Indexes**:
  - `loads`: shipperId, status
  - `bids`: loadId, transporterId, status
  - `bookings`: loadId, transporterId, status
- **Optimistic Locking**: `version` column in Load entity prevents concurrent modifications

## 🔥 Business Rules Implementation

### Rule 1: Capacity Validation ✅

- Transporter can only bid if `trucksOffered ≤ availableTrucks`
- When booking confirmed, `allocatedTrucks` deducted from transporter's pool
- When booking cancelled, trucks restored to available pool

### Rule 2: Load Status Transitions ✅

- `POSTED → OPEN_FOR_BIDS` (when first bid received)
- `OPEN_FOR_BIDS → BOOKED` (when fully allocated)
- `BOOKED → CANCELLED` (if booking cancelled)
- Cannot bid on `CANCELLED` or `BOOKED` loads
- Cannot cancel load that's already `BOOKED`

### Rule 3: Multi-Truck Allocation ✅

- Tracks `remainingTrucks = noOfTrucks - SUM(allocatedTrucks)`
- Load becomes `BOOKED` only when `remainingTrucks == 0`
- Multiple bookings allowed until fully allocated

### Rule 4: Concurrent Booking Prevention ✅

- Uses `@Version` optimistic locking in Load entity
- First transaction wins, second fails with `ConflictException`

### Rule 5: Best Bid Calculation ✅

- Score formula: `(1 / proposedRate) * 0.7 + (rating / 5) * 0.3`
- Higher score = better bid
- Sorted descending by score

## 📡 API Endpoints

### Load APIs (5 endpoints)

#### 1. Create Load

```http
POST /load
Content-Type: application/json

{
  "shipperId": "SHIPPER001",
  "loadingCity": "Mumbai",
  "unloadingCity": "Delhi",
  "loadingDate": "2024-02-15T10:00:00",
  "productType": "Electronics",
  "weight": 5000.0,
  "weightUnit": "KG",
  "truckType": "Container",
  "noOfTrucks": 3
}
```

#### 2. List Loads (with pagination)

```http
GET /load?shipperId=SHIPPER001&status=OPEN_FOR_BIDS&page=0&size=10
```

#### 3. Get Load by ID

```http
GET /load/{loadId}
```

#### 4. Cancel Load

```http
PATCH /load/{loadId}/cancel
```

#### 5. Get Best Bids

```http
GET /load/{loadId}/best-bids
```

### Transporter APIs (3 endpoints)

#### 1. Register Transporter

```http
POST /transporter
Content-Type: application/json

{
  "companyName": "ABC Transport",
  "rating": 4.5,
  "availableTrucks": [
    {"truckType": "Container", "count": 10},
    {"truckType": "Flatbed", "count": 5}
  ]
}
```

#### 2. Get Transporter Details

```http
GET /transporter/{transporterId}
```

#### 3. Update Available Trucks

```http
PUT /transporter/{transporterId}/trucks
Content-Type: application/json

{
  "availableTrucks": [
    {"truckType": "Container", "count": 12},
    {"truckType": "Flatbed", "count": 6}
  ]
}
```

### Bid APIs (4 endpoints)

#### 1. Submit Bid

```http
POST /bid
Content-Type: application/json

{
  "loadId": "uuid-here",
  "transporterId": "uuid-here",
  "proposedRate": 50000.0,
  "trucksOffered": 2
}
```

#### 2. Filter Bids

```http
GET /bid?loadId=uuid&transporterId=uuid&status=PENDING
```

#### 3. Get Bid Details

```http
GET /bid/{bidId}
```

#### 4. Reject Bid

```http
PATCH /bid/{bidId}/reject
```

### Booking APIs (3 endpoints)

#### 1. Accept Bid & Create Booking

```http
POST /booking
Content-Type: application/json

{
  "bidId": "uuid-here"
}
```

#### 2. Get Booking Details

```http
GET /booking/{bookingId}
```

#### 3. Cancel Booking

```http
PATCH /booking/{bookingId}/cancel
```

## 📝 Response Format

All APIs return responses in the following format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

Error responses:

```json
{
  "success": false,
  "message": "Error message here",
  "data": null
}
```

## 🧪 Testing

### Manual Testing with cURL

```bash
# Create a transporter
curl -X POST http://localhost:8080/transporter \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "Test Transport",
    "rating": 4.5,
    "availableTrucks": [{"truckType": "Container", "count": 10}]
  }'

# Create a load
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
    "noOfTrucks": 3
  }'
```

### Unit Testing

```bash
mvn test
```

## 🏗️ Architecture

```
Controller Layer (REST APIs)
    ↓
DTO Layer (Data Transfer Objects)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Entity Layer (Domain Models)
    ↓
Database (PostgreSQL)
```

## 🔒 Exception Handling

Custom exceptions with proper HTTP status codes:

- `ResourceNotFoundException` → 404 Not Found
- `InvalidStatusTransitionException` → 400 Bad Request
- `InsufficientCapacityException` → 400 Bad Request
- `LoadAlreadyBookedException` → 409 Conflict
- `OptimisticLockingFailureException` → 409 Conflict

All exceptions are handled globally via `@ControllerAdvice`.

## 📦 Project Structure

```
tms-backend/
├── src/
│   ├── main/
│   │   ├── java/com/cargopro/tms/
│   │   │   ├── controller/      # REST Controllers
│   │   │   ├── service/         # Business Logic
│   │   │   ├── repository/      # Data Access Layer
│   │   │   ├── entity/          # JPA Entities
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── exception/       # Custom Exceptions
│   │   │   └── TmsApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/                    # Test files
├── pom.xml
└── README.md
```

## 🎯 Key Features

✅ All 15 required APIs implemented  
✅ Complex business rules enforced  
✅ Optimistic locking for concurrency control  
✅ Comprehensive exception handling  
✅ Input validation  
✅ Database constraints and indexes  
✅ Best bid scoring algorithm  
✅ Multi-truck allocation support  
✅ Status transition management

## 📧 Contact

For questions or issues, please contact : smanojsingh073@gmail.com

---

**Note**: This implementation follows SOLID principles, uses clean code practices, and includes proper error handling. All business rules are strictly enforced at the service layer.
