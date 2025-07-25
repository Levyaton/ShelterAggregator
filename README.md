# ShelterAggregator

**Table of Contents**

1. [Introduction](#introduction)
2. [Purpose](#purpose)
3. [Tech Stack & Dependencies](#tech-stack--dependencies)
4. [Project Modules](#project-modules)
5. [API Endpoints](#api-endpoints)
6. [Local Development & Simulated Environment](#local-development--simulated-environment)
7. [Usage](#usage)
8. [Contributing](#contributing)
9. [License](#license)

---

## Introduction

ShelterAggregator is a multi-module application designed to aggregate data from Czech animal shelters and present a unified interface for discovering adoptable dogs. It achieves this without impacting the shelters’ own traffic or revenue by proxying and normalizing external data sources rather than redirecting users away from shelter websites.

## Purpose

- **Aggregator-first**: Collect and merge dog listings from multiple Czech shelter APIs and websites.
- **Non-intrusive**: Users browse listings through our UI, but visits to the original shelter pages remain intact, ensuring shelters keep full credit and web traffic.
- **Extensible**: Easily add new shelter integrations via the `integrations` module without changing core logic.

## Tech Stack & Dependencies

### Core Backend (Java / Spring Boot)

- **Language**: Java 24 (with preview features enabled)
- **Framework**: Spring Boot 3.5.3
- **Build Tool**: Maven 3.8+

**Key Dependencies**:

- Spring Web, Spring Data JPA, Spring Validation
- Docker, or some other virtualization tool for local development
- Apache Kafka (integration via Spring Kafka)
- PostgreSQL JDBC Driver
- Flyway for database migrations
- Lombok for boilerplate reduction
- Apache Commons Math (statistics)
- Testcontainers, JUnit5 & Rest Assured (testing)

### Frontend (React & Node.js)

- **Runtime**: Node.js 16+ (for the React dev server & proxy)
- **Framework**: React 18
- **Bundler**: Create React App (react-scripts 5.0.1)
- **Dev Server**: Express 4.x (for API proxying)

**Key Dependencies**:

- axios, react-fast-marquee
- concurrently, cross-env
- cors, express, node-fetch

### Scripts & Local Proxy (Node.js)

- **Runtime**: Node.js 16+ (LTS recommended)
- **Framework**: Express 4.x
- **Dependencies**: cors, node-fetch

### Infrastructure

- **Database**: PostgreSQL 15 (via Docker Compose)
- **Messaging**: Apache Kafka (if enabled)

## Project Modules

Each module contains its own README with detailed instructions.

| Module         | Description                                        | Docs Link                                       |
| -------------- | -------------------------------------------------- | ----------------------------------------------- |
| `frontend`     | React UI, Node.js dev server & proxy               | [Frontend README](./frontend/README.md)         |
| `backend`      | Spring Boot REST API                               | [Backend README](./backend/README.md)           |
| `integrations` | Adapter layer for external shelter sources         | [Integrations README](./integrations/README.md) |

## API Endpoints

### Backend Service (`/dogs`)

| Method | Endpoint     | Description                                                          |
|--------| ------------ |----------------------------------------------------------------------|
| GET    | `/dogs`      | Retrieve paginated list of dogs.                  (Public endpoint)  |
| GET    | `/dogs/{id}` | Retrieve details for a single dog by internal ID. (Public endpoint)  |
| POST   | `/dogs`      | Create a new dog entry.                           (Private endpoint) |
| DELETE | `/dogs/{id}` | Delete the specified dog entry                    (Private endpoint) |
| PUT    | `/dogs/{id}` | Update the specified dog entry                    (Private endpoint) |

**Query Parameters for GET **``:

- `page` (int, default `0`): Page number
- `size` (int, default `10`): Items per page
- `order` (`asc`/`desc`, default `asc`): Sort direction
- `sort` (e.g. `age`, `createdAt`): Field to sort by
- `ageMin`, `ageMax` (int): Age range filter
- `sex` (`MALE`/`FEMALE`): Sex filter
- `dogSize` (`SMALL`/`MEDIUM`/`LARGE`): Size filter
- `randomise` (boolean): Return a random selection instead of paginated list

### Frontend Proxy (`/api/dogs`)

| Method | Endpoint    | Description                                                               |
| ------ | ----------- | ------------------------------------------------------------------------- |
| GET    | `/api/dogs` | Proxies calls to backend, fetches images, returns Data URIs for frontend. |

## Local Development & Simulated Environment

1. **Clone the repository**

   ```bash
   git clone https://github.com/Levyaton/ShelterAggregator.git
   cd ShelterAggregator
   ```

2. **Database Setup**

   ```bash
   docker-compose up -d
   ```

   - Starts PostgreSQL on `localhost:5432` with DB `shelterdb`, user `user`, password `123456`.

3. **Backend Service**

   ```bash
   mvn clean install -DskipTests -Penable-preview
   mvn spring-boot:run -pl backend -Dspring-boot.run.jvmArguments="--enable-preview"
   ```

   - Ensure Java 24 is installed and `JAVA_HOME` points to it.
   - Spring Boot runs on `http://localhost:8080`.

4. **Integration Adapters**

   ```bash
   mvn install -pl integrations -Penable-preview
   ```

   - Ensures all shelter integrations compile and are available.

5. **Frontend & Proxy**

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   - `npm run dev` starts both React on `:3001` and proxy server on `:5000`.
   - React app will call `/api/dogs` to fetch data via the proxy.

> **Why use the simulated environment?**
>
> - Ensures you can develop and test without hitting real shelter APIs or altering production data.
> - Local proxy handles image conversion and error resilience (e.g., unavailable images).

## Usage

1. Navigate to `http://localhost:3001` in your browser.
2. Browse and filter dog listings.
3. Click on a dog to view detailed information.

## TODO
- [ ] Implement more shelter integrations.
- [ ] Add Kafka integration. Each integration in the integrations module should publish messages to Kafka topics, which can be consumed by our main backend module. This will be the primary way to update, create and delete data, as opposed to our rest endpoints, which will be used for administrative purposes.

## Contributing

We welcome contributions! Please see our [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE). Feel free to use and modify!

