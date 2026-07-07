# FlashShare — Distributed High-Throughput Ride-Sharing & Geospatial Matching Platform

FlashShare is an enterprise-grade, event-driven microservices backend designed to handle high-frequency location tracking, low-latency spatial driver matching, and asynchronous event propagation at scale.

The platform leverages a hybrid storage strategy—combining relational consistency with ultra-fast in-memory caching—and choreographs communication across distributed services using an independent event bus.

---

## 🏗️ System Architecture Overview

The complete targeted architecture transitions from an edge layer down through isolated business domains, coordinated asynchronously via message brokers to eliminate tight runtime coupling.

```
              [ Mobile / Web Clients ]
                         │  (HTTP / REST)
                         ▼
                ┌─────────────────┐
                │   API Gateway   │  (Security, JWT Validation, Rate Limiting)
                └────────┬────────┘
                         │
        ┌────────────────┴────────────────┐
        ▼                                 ▼
┌───────────────────────┐         ┌───────────────────────┐
│     Rider Service     │         │    Driver Service     │
│     (Port: 8081)      │         │     (Port: 8082)      │
└───────────┬───────────┘         └───────────┬───────────┘
            │                                 │
            ├─────────────────────────────────┼────────────────────────────────┐
            ▼ (Transactional)                 ▼ (Geospatial / Caching)          ▼ (Pub-Sub Streaming)
┌───────────────────────┐         ┌───────────────────────┐         ┌───────────────────────┐
│       MySQL 8.0       │         │      Redis Stack      │         │     Apache Kafka      │
│ (Relational Storage)  │         │ (Spatial Index / RAM) │         │    (Event Broker)     │
└───────────────────────┘         └───────────────────────┘         └───────────────────────┘
```

---

## 🛠️ Technology Stack

* **Core Framework:** Java 17, Spring Boot 3.x, Spring Cloud Ecosystem (Gateway, Config Server)
* **Event Streaming & Messaging:** Apache Kafka (Distributed Pub-Sub Log)
* **High-Speed Cache & Spatial Indexing:** Redis Stack (Geospatial Geohashes & Sorted Sets)
* **Relational Storage:** MySQL 8.0 (ACID-compliant transactional store)
* **Database Evolution:** Flyway / Liquibase (Version-controlled schema migrations)
* **Fault Tolerance & Resilience:** Resilience4j (Circuit Breakers, Retries, Rate Limiters)
* **Security Layer:** Spring Security, JSON Web Tokens (JWT) Stateless Authentication
* **Data Layer Tools:** Spring Data JPA, Hibernate ORM, Jackson (JSON Serialization)
* **Infrastructure Pipeline:** Docker, Multi-Stage Dockerfiles, Docker Compose

---

## 🌟 Key Features & Architectural Patterns

### 1. Asynchronous Event Choreography (Implemented)
* **Decoupled Workflows:** Microservices interact out-of-band using high-performance **Kafka Event Streams** to process core actions like user registrations and active ride requests (`ride-requests`).
* **Data Integrity:** Configured **Idempotent Kafka Producers** to eliminate duplicate message processing and maintain reliable cross-service consistency under unstable network conditions.
* **Horizontal Scalability:** Implemented consumer group management with isolated worker thread pools (`@KafkaListener`) over a **3-partition layout**, enabling independent throughput scaling across instances.

### 2. High-Speed Geospatial Bounding Proximity Math (Implemented)
* **Sub-Millisecond Queries:** Utilizes Redis Sorted Sets via `opsForGeo().radius()` to compute spatial driver lookups within dynamic kilometer bounding radii in microseconds.
* **Hydration Pattern:** Redis serves as an ultra-fast structural filter to isolate near-field entity IDs, leaving the transactional layer to perform targeted data hydration (`SELECT ... WHERE id IN (...)`), protecting database memory overhead.

### 3. Distributed Cache-Aside (Lazy Loading) Strategy (Implemented)
* **Database Offloading:** Eliminates redundant MySQL read bottlenecks for high-traffic profiles by implementing a robust **Cache-Aside Pattern**.
* **Self-Healing Pools:** Enforces a strict **Time-To-Live (TTL)** strategy (10-minute automated cache invalidation window) to eliminate stale data pools and guarantee consistency with the master database.

### 4. Edge Routing & Centralized Identity Enforcement (Planned / Roadmap)
* **API Gateway Routing:** Implementation of a centralized **Spring Cloud Gateway** routing layer to manage cross-origin resource sharing (CORS), aggregate microservice entry points, and obscure interior network topologies.
* **Stateless JWT Verification:** Integration of **Spring Security** filters at the edge gateway to decrypt and validate incoming Bearer Tokens, managing identity propagation downstream via secure HTTP headers.

### 5. Resilient Fault Isolation & Self-Healing (Planned / Roadmap)
* **Circuit Breaking:** Implementation of **Resilience4j Circuit Breakers** on inter-service communications to intercept cascading connection failures and gracefully transition to fallback methods.
* **Active Rate Limiting:** Enforcing localized rate-limiting algorithms to protect high-traffic public endpoints from request spikes and Denial of Service (DoS) anomalies.

### 6. Version-Controlled Schema Evolutions (Planned / Roadmap)
* **Database Migrations:** Integration of **Flyway Migration Scripts** (`V1__init.sql`, `V2__add_index.sql`) to completely remove Hibernate's high-risk `ddl-auto: update` behavior from production pipelines.

---

## 📂 Project Structure

```text
flash-share-parent/
 │
 ├── pom.xml                        # Master Parent Maven configuration
 ├── docker-compose.yml             # Unified local infrastructure composition
 │
 ├── rider-service/                 # Handles Rider Profiles, Booking Requests & Cache-Aside Reads
 │    ├── Dockerfile                # Multi-stage optimized JRE runtime container build
 │    ├── pom.xml                   # Module-specific dependencies
 │    └── src/main/java/com/flashshare/riderservice/
 │
 ├── driver-service/                # Manages Driver States, Telemetry & Real-Time Matching Loops
 │    ├── pom.xml
 │    └── src/main/java/com/flashshare/driverservice/
 │
 └── api-gateway/ [Planned]         # Edge routing, Spring Security, and Stateless JWT Verification
```

---

## 🚦 Core API Specifications & Endpoints

### 🔹 Rider Service (`Port: 8081`)

* `POST /api/v1/riders` — Registers a rider, persists record to MySQL, and publishes telemetry to Kafka.
* `PATCH /api/v1/riders/{id}/location` — Updates moving spatial telemetry coordinates inside the Redis cache.
* `GET /api/v1/riders/{id}` — Returns active profile details via the **Cache-Aside Redis engine**.
* `GET /api/v1/riders/nearby` — Executes an instant geo-radius search against live Redis coordinates.
* `POST /api/v1/riders/{id}/request-ride` — Fires a live booking payload directly into the `ride-requests` Kafka stream.

### 🔹 Driver Service (`Port: 8082`)

* `@KafkaListener(topics = "ride-requests")` — Intercepts distributed ride-booking payloads completely out-of-band to initialize geographic search grids.

---

## 🚀 Local Installation & Execution Steps

### Prerequisites

* Java 17 Development Kit (JDK) installed
* Apache Maven 3.8+ installed
* Docker Desktop operational on host system

### 1. Launch Core Infrastructure Layers

Spin up the coordinated database, cache, and streaming infrastructure engines using Docker:

```bash
docker compose up -d
```

*Verify that MySQL (`3307`), Redis (`6379`), RedisInsight (`8001`), and Apache Kafka (`9092`) are active before starting the services.*

### 2. Launch the Application Modules Locally

Open separate terminal tabs for each individual service component and execute:

**Rider Service Engine:**

```bash
cd rider-service
mvn spring-boot:run
```

**Driver Service Engine:**

```bash
cd driver-service
mvn spring-boot:run
```

### 3. Verify End-to-End Execution Flow

Fire a sample trip request payload into the gateway using an API tool like Postman:

```http
POST http://localhost:8081/api/v1/riders/1/request-ride?destination=Airport_Terminal_3
```

Observe the asynchronous execution logs in the `driver-service` terminal to see the event stream intercepted, verified, and mapped in real time.
