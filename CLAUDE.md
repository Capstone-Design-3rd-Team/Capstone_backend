# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Test
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run tests for a specific class
./gradlew test --tests "CapstoneJavaApplicationTests"

# Clean and rebuild
./gradlew clean build

# Run the application
./gradlew bootRun
```

### Infrastructure Management
```bash
# Start all services (Kafka, Zookeeper, MySQL, Kafka UI)
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f kafka
docker-compose logs -f mysql

# Access Kafka UI for monitoring
# http://localhost:8090
```

### Database
- MySQL runs on port 3307 (not default 3306)
- Database: `capstone_db`
- Credentials: root/rkwhr123
- JPA DDL is set to `update` mode

## Architecture Overview

This project implements **Hexagonal Architecture** (Ports & Adapters) with **Domain-Driven Design** patterns within a Spring Boot application focused on web crawling and event-driven processing.

### Core Architectural Patterns

**Hexagonal Architecture Structure:**
```
website/
├── domain/           # Core business logic (entities, value objects, events)
├── application/      # Use cases and application services
│   ├── port/in/     # Inbound ports (use case interfaces)
│   ├── port/out/    # Outbound ports (repository/external service interfaces)
│   └── service/     # Use case implementations
├── adapter/         # Infrastructure implementations
│   ├── in/          # Inbound adapters (REST controllers, Kafka consumers)
│   └── out/         # Outbound adapters (repositories, external services)
└── global/          # Cross-cutting concerns (config, constants)
```

**Key Design Principles:**
- Domain entities are immutable with factory methods and state transition methods
- Ports define interfaces, adapters provide implementations
- Events drive asynchronous processing between bounded contexts
- MapStruct handles object mapping between layers

### Event-Driven Architecture

**Kafka Integration Pattern:**
- Events are defined as domain records in `domain/event/`
- Producer: `ExtractEventProducer` implements `PublishEventPort`
- Consumer: `ExtractionEventConsumer` handles domain events
- Topics, groups, and factories are defined as constants in `global/common/`

**Event Flow:**
1. Domain service publishes `ExtractionStartedEvent`
2. Kafka consumer triggers `CrawlUrlsUseCase`
3. Retry mechanism with exponential backoff
4. Dead Letter Topic (DLT) for failed events

### Domain Modeling

**Entity Pattern (`Website.java`):**
- Immutable entities with private final fields
- Static factory methods (`create()`, `withId()`)
- State transition methods (`startExtraction()`, `markCompleted()`)
- Business logic encapsulated in domain methods

**Value Objects (`WebsiteId.java`):**
- Immutable wrappers around primitive types
- Used for type safety and domain clarity

**Domain Events:**
- Records implementing domain events
- Include partition key generation for Kafka
- Factory methods for convenient creation

## Key Implementation Details

### MapStruct Integration
- Mapper interfaces in `adapter/out/mapper/`
- Component model: "spring" for dependency injection
- Custom mapping methods using `@Named` qualifiers
- Handles entity ↔ domain object transformations

### Kafka Configuration
- Producer/Consumer configs in `global/config/`
- Constants for topics, groups, and factories prevent typos
- Retry policies with `@RetryableTopic`
- Manual acknowledgment for error handling

### Testing Approach
- Uses JUnit 5 (`@Test` annotation)
- Spring Boot test slices available (`@SpringBootTest`)
- Kafka testing support included in dependencies

### Port/Adapter Naming Conventions
- **Inbound Ports**: `*UseCase` interfaces (e.g., `ExtractUrlsUseCase`)
- **Outbound Ports**: `*Port` interfaces (e.g., `SaveWebsitePort`, `GetWebsitePort`)
- **Adapters**: `*Adapter` implementations (e.g., `WebsiteAdapter`, `JsoupAdapter`)
- **Services**: `*Service` classes implementing use cases

### Domain Events and State Management
- `ExtractionStatus` enum defines website processing states
- State transitions enforce business rules
- Events carry immutable data and metadata
- Partition keys enable horizontal scaling

## Development Workflow

1. **Infrastructure First**: Start Docker services before development
2. **Domain-First Development**: Model business logic in domain layer before implementation
3. **Test-Driven**: Write tests alongside domain logic
4. **Event Flow**: Consider event-driven interactions between bounded contexts
5. **Port-First**: Define interfaces (ports) before implementations (adapters)

## Current Development Context

- **Active Branch**: `Feat/crawl` - implementing URL crawling functionality
- **In Progress**: `UrlCrawlingService` implementation with Jsoup integration
- **Recent Additions**: URL validation service, Jsoup adapter, crawl events
- **Next**: Complete crawling use case and event handling

- Do not use emojis in your responses.