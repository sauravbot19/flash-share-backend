# FlashShare — Distributed High-Throughput Ride-Sharing & Geospatial Matching Platform

FlashShare is an enterprise-grade, event-driven microservices platform designed to handle high-frequency location tracking, low-latency spatial driver matching, and asynchronous event propagation at scale.

Instead of simple CRUD operations over isolated profiles, the platform provides an end-to-end hyper-local dynamic routing engine (**Flash-Share**) that coordinates riders and moving vehicles in real time. It balances high-velocity data streaming with absolute transactional integrity across a decoupled distributed infrastructure.

---

## 🏗️ Complete System Architecture Overview

The platform transitions from stateful connection edges down through decoupled business domains. Synchronous operations are minimized via an asynchronous message backplane, eliminating runtime coupling and protecting core data structures.

```
                        [ Mobile / Web Clients ]
                           │              ▲
               (HTTP/REST) │              │ (Persistent WebSockets)
                           ▼              │
                 ┌────────────────────────────────┐
                 │          API Gateway           │ (Security, Rate Limiting, CORS)
                 └──────────────┬─────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│ Rider Service │       │ Driver Service│       │ Trip Service  │ (State Controller)
│ (Port: 8081)  │       │ (Port: 8082)  │       │ (Port: 8083)  │
└───────────────┘       └───────────────┘       └───────┬───────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│ Matching Serv.│       │ Websocket Srv.│       │ Notification  │
│ (Route Math)  │       │ (Live Streams)│       │ (Push/Alerts) │
└───────┬───────┘       └───────┬───────┘       └───────┬───────┘
        │                       │                       │
        ├───────────────────────┴───────────────────────┤
        ▼ (Transactional)       ▼ (Geospatial Cache)    ▼ (Pub-Sub Log)
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│   MySQL 8.0   │       │  Redis Stack  │       │ Apache Kafka  │
│ (Primary Store)       │ (Spatial RAM) │       │ (Event Bus)   │
└───────────────┘       └───────────────┘       └───────────────┘

```

---

## 🛠️ Technology Stack

* **Core Framework:** Java 17, Spring Boot 3.x, Spring Cloud Gateway
* **Event Streaming & Messaging:** Apache Kafka (Distributed Append-Only Commit Log)
* **High-Speed Cache & Spatial Indexing:** Redis Stack (Geospatial Geohashes & Sorted Sets)
* **Real-Time Client Duplexing:** Spring WebSockets, STOMP Messaging Protocol
* **Relational Storage:** MySQL 8.0 (ACID-compliant transactional store)
* **Database Evolution:** Flyway / Liquibase version-controlled migration scripts
* **Fault Tolerance & Resilience:** Resilience4j (Circuit Breakers, Retries, Rate Limiters)
* **Security Layer:** Spring Security, JSON Web Tokens (JWT) Stateless Authentication
* **Infrastructure Pipeline:** Docker, Multi-Stage Optimized Dockerfiles, Docker Compose

---

## 🌟 Domain Architecture & Key Design Patterns

### 1. Flash-Share Routing & Proximity Matching Engine

* **Sub-Millisecond Geohashing:** Utilizes Redis Sorted Sets via `opsForGeo().radius()` to query active driver coordinates within dynamic bounding fields.
* **Overlapping Vector Math:** The Matching Service evaluates a rider's destination against a driver's active route vector. It isolates ongoing commutes passing within a 500-meter threshold of the request corridor.
* **Hydration Pattern:** Redis filters near-field entity IDs in memory. The service then passes clean ID arrays to the database layer to perform isolated record hydration (`SELECT ... WHERE id IN (...)`), preserving database memory overhead.

### 2. Transactional State Control (Trip Service)

* **State Machine Isolation:** Handles the strict lifecycle progression of a ride (`PENDING` $\rightarrow$ `ACCEPTED` $\rightarrow$ `STARTED` $\rightarrow$ `COMPLETED`).
* **Concurrency Protection:** Protects inventory assets (such as empty vehicle seats) by utilizing pessimistic locking mechanisms during critical acceptance handshakes.

### 3. Asynchronous Event Choreography

* **Decoupled Workflows:** Business components interact completely out-of-band using high-performance Kafka topics. The `Trip Service` issues success packets to the client immediately after updating the database, handing the execution off to Kafka.
* **Idempotent Delivery:** Configured with idempotent producers and unique message tracking IDs to prevent duplicate actions (like multiple acceptance signals for a single trip) across network boundaries.
* **Partition Tuning:** Topics use a multi-partition strategy mapped against trip IDs, guaranteeing sequential execution order for single rides while processing multiple trips concurrently.

### 4. Telemetry Bypass Engine

* **High-Frequency Routing:** To prevent database connection pooling exhaustion, driver location updates (sent every 3 seconds) bypass the relational database entirely.
* **Live Ingestion:** Coordinates hit the Redis spatial index via `GEOADD`. The data shifts out to the WebSocket layer instantly, preserving MySQL CPU cycles for strict transactional operations.

---

## 📂 Multi-Module Project Structure

```text
flash-share-parent/
 │
 ├── pom.xml                        # Master Parent Maven configuration
 ├── docker-compose.yml             # Unified local infrastructure configuration
 │
 ├── api-gateway/                   # Edge routing, SSL termination, and JWT validation
 │
 ├── rider-service/                 # Manages Rider Accounts, Profiles, and Cache-Aside lookup logic
 │
 ├── driver-service/                # Manages Driver details, shift status, and registration
 │
 ├── flash-share-matching-service/  # Stateless microservice for spatial math & Redis GEO queries
 │
 ├── trip-service/                  # State Machine controller for trip lifecycles (MySQL driver)
 │
 ├── websocket-server/              # Manages persistent stateful connections to active clients
 │
 └── notification-service/          # Consumes Kafka event streams to fire push notifications/SMS

```

---

## 🚦 Complete Component API & Stream Specifications

### 1. Ingress & Connection Layers

#### 🔀 API Gateway (`Port: 8080`)

* Proxies all inbound public REST traffic to downstream services.

#### 🔌 WebSocket Server (`Port: 8085`)

* `WS /ws/tracking` — Persistent connection endpoint for real-time map updates.
* **Subscribe Topic:** `/topic/trips/{tripId}` — Inbound pipe for riders tracking driver approach paths.

---

### 2. Microservice Domains

#### 📱 Rider Service (`Port: 8081`)

* `POST /api/v1/riders` — Registers profile records.
* `GET /api/v1/riders/{id}` — Returns rider profiles using a **Cache-Aside Redis strategy**.

#### 🚗 Driver Service (`Port: 8082`)

* `POST /api/v1/drivers` — Provisions driver assets and vehicle vacancy states.
* `PATCH /api/v1/drivers/{id}/telemetry` — Low-overhead ingest pipe for 3-second GPS updates.

#### 🧠 Flash-Share Matching Service (`Port: 8083`)

* `GET /api/v1/match` — Compares an active position against moving vehicle vectors. Returns optimized candidate lists.

#### ⚙️ Trip Service (`Port: 8084`)

* `POST /api/v1/trips/request` — Instantiates a ride request (`FLASH_REQUEST_PENDING`).
* `PATCH /api/v1/trips/{id}/accept` — Executes atomic assignment transition to `FLASH_REQUEST_ACCEPTED`.
* `PATCH /api/v1/trips/{id}/start` — Transitions status to `RIDE_STARTED`.

---

### 3. Kafka Messaging Fabric & Event Specifications

```
┌───────────────────────┬───────────────────────────────┬───────────────────────────────┐
│ Topic Name            │ Emitting Component            │ Consuming Components          │
├───────────────────────┼───────────────────────────────┼───────────────────────────────┤
│ flash-pickup-request  │ Trip Service                  │ Notification Service          │
│ flash-pickup-accepted │ Trip Service                  │ WebSocket Server / Rerouter   │
│ driver-telemetry-raw  │ Driver Service                │ Matching Service / Redis      │
└───────────────────────┴───────────────────────────────┴───────────────────────────────┘

```

---

## 🚀 Environment Initialization & Bootstrapping

### Prerequisites

* Java 17 Development Kit (JDK)
* Apache Maven 3.8+
* Docker Desktop

### 1. Provision Infrastructure Dependencies

Spin up the integrated database, messaging, and orchestration containers:

```bash
docker compose up -d

```

> **Verification Check:** Ensure MySQL (`3307`), Redis (`6379`), RedisInsight (`8001`), and Apache Kafka (`9092`) are ready for connections before starting application services.

### 2. Compile and Build Modules

Execute a clean compilation from the parent container directory:

```bash
mvn clean install

```

### 3. Orchestrate Services Locally

Run the business modules in priority order using separate terminal shells:

```bash
# 1. Boot Ingress Edge
cd api-gateway && mvn spring-boot:run

# 2. Boot Core Domain Catalogs
cd rider-service && mvn spring-boot:run
cd driver-service && mvn spring-boot:run

# 3. Boot State Engines & Math Evaluators
cd trip-service && mvn spring-boot:run
cd flash-share-matching-service && mvn spring-boot:run

# 4. Boot Push & Dynamic Infrastructure Edges
cd websocket-server && mvn spring-boot:run
cd notification-service && mvn spring-boot:run

```

### 4. End-to-End Flow Verification Trace

To validate the full platform pipeline without client hardware:

1. **Simulate Ongoing Driver Route:** Use Postman to stream coordinate pairs into the telemetry endpoint (`PATCH /api/v1/drivers/{id}/telemetry`).
2. **Execute Matching Query:** Hit the matching router (`GET /api/v1/match`) with a target location to ensure the driver is detected by the Redis geo-index.
3. **Initiate Trip Lifecycle:** Send a request booking payload to the Gateway (`POST /api/v1/trips/request`).
4. **Trace Internal Event Logs:** Observe the `notification-service` console to verify the asynchronous collection of the Kafka event and the generation of the downstream push notify payload.
