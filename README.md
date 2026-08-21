# Settle

## Overview
Settle is a group expense-splitting and debt-settlement platform, built incrementally as a comprehensive Java full-stack portfolio project. It handles groups, expenses, splits, and the underlying ledger logic to track and settle debts.

## Architecture
- **Backend:** Java 17, Spring Boot 3.x
- **Web Layer:** Spring Web
- **Data Access:** Spring Data JPA, PostgreSQL
- **Migrations:** Flyway
- **Security:** Spring Security
- **Build Tool:** Maven

## Setup
### Prerequisites
- Java 17+
- Maven
- Docker and Docker Compose

### Local Development
1. Start the PostgreSQL database using Docker Compose:
   ```bash
   docker-compose up -d
   ```
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```
   The application will connect to the local PostgreSQL database using default environment variables.

### Database Migrations (Flyway)
We use Flyway to version control our database schema. Migration scripts are located in `src/main/resources/db/migration`.
**Flyway Naming Convention:**
- Prefix `V` followed by a version number (e.g., `1`, `2`, `1.1`).
- Two underscores `__`.
- A descriptive name (e.g., `init`, `add_users`).
- Extension `.sql`.
Example: `V1__init_schema.sql`

Flyway runs automatically when the Spring Boot application starts, executing any pending migration scripts in order.

## Design Decisions
*This section will be updated as the project progresses through different phases.*
