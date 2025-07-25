# Backend Module

This module provides the core REST API and data processing for the ShelterAggregator application.

## Purpose

- **Data Ingestion**: Receives normalized dog data from the integrations layer.
- **Persistence**: Stores and manages dog records in PostgreSQL with schema migrations via Flyway.
- **Business Logic**: Applies filtering, sorting, pagination, and randomization of dog listings.
- **API Exposure**: Exposes endpoints for CRUD operations on dog entities for the frontend and external clients.

## Specifications

- **Language & Framework**: Java 24 (preview features) with Spring Boot 3.5.3
- **Build Tool**: Maven 3.8+
- **Database**: PostgreSQL 15
- **Messaging**: Apache Kafka (optional)

### Key Components

- **Controllers** (`/src/main/java/.../controller`)

  - `DogController`: Handles HTTP requests for `/dogs` endpoints.

- **Services** (`/src/main/java/.../service`)

  - `DogService`: Encapsulates business logic (filters, sorting, pagination).

- **Repositories** (`/src/main/java/.../repository`)

  - `DogRepository`: Spring Data JPA interface for CRUD operations.

- **Entities** (`/src/main/java/.../entity`)

  - `Dog`: JPA entity mapping to the `dogs` table.

- **Migrations** (`/src/main/resources/db/migration`)

  - Flyway scripts to create and alter tables.

- **Configuration** (`/src/main/resources/application.yml`)

  - Database connection, Kafka settings, and JVM preview flags.

## Endpoints

| Method | URI          | Description                           |
| ------ | ------------ | ------------------------------------- |
| GET    | `/dogs`      | List dogs (with filters, pagination). |
| GET    | `/dogs/{id}` | Get details for a single dog.         |
| POST   | `/dogs`      | Create new dog record.                |
| PUT    | `/dogs/{id}` | Update existing dog record.           |
| DELETE | `/dogs/{id}` | Remove dog record.                    |

## Running Locally

1. Ensure Java 24 is installed and `JAVA_HOME` points to it.
2. Configure PostgreSQL in `application.yml` or via environment variables.
3. Build and run:
   ```bash
   mvn clean install -DskipTests -Penable-preview
   mvn spring-boot:run -Dspring-boot.run.jvmArguments="--enable-preview"
   ```
4. API available at `http://localhost:8080`.

## Testing

- **Unit Tests**: JUnit 5 in `src/test/java`.
- **Integration Tests**: Testcontainers for PostgreSQL and Kafka.

---

*For more details, see parent README.*

