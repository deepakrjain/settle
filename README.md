# Settle

A group expense-splitting and debt-settlement platform built with Java 17 and Spring Boot.

## Overview
**Settle** simplifies tracking shared expenses among groups. It supports multiple flexible expense-splitting algorithms, automated debt calculation, and secure group management.

## Key Features
- **User Authentication:** Registration and stateless JWT-based authentication.
- **Group Management:** Group creation, member invitations, and membership authorization.
- **Flexible Expense Splitting:**
  - **Equal:** Split expenses evenly among participants with exact penny/paisa remainder handling.
  - **Percentage:** Split by custom percentages summing to 100%.
  - **Exact:** Split by exact predefined monetary amounts.
  - **Shares:** Split proportionally based on custom share counts.
  - **Itemized:** Split itemized receipts with proportional tax and tip distribution.

## Architecture & Tech Stack
- **Language:** Java 17
- **Framework:** Spring Boot 3.3.x (Spring Web, Spring Security, Spring Data JPA)
- **Database:** PostgreSQL
- **Migrations:** Flyway
- **Build Tool:** Maven
- **Containerization:** Docker Compose (local dev)

## Project Structure
Organized **by feature** for high cohesion and modular design:
- `com.settle.user` — User management and profile endpoints
- `com.settle.group` — Group lifecycle and membership authorization
- `com.settle.expense` — Expense creation, split strategies, and history
- `com.settle.ledger` — Debt calculation and settlement tracking
- `com.settle.common` — Security, JWT filters, and global exception handling

## Getting Started

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Maven

### Local Setup
1. **Start PostgreSQL Container:**
   ```bash
   docker-compose up -d
   ```
2. **Run the Application:**
   ```bash
   mvn spring-boot:run
   ```
   *Flyway will automatically run database migrations on application start.*

## Key Design Decisions
- **Stateless JWT Authentication:** Request-scoped security using custom filter chain integration without HTTP sessions.
- **Strategy Pattern for Splitting:** Encapsulated split algorithms (`SplitStrategy`) for clean extensibility adherence to the Open-Closed Principle.
- **Financial Precision:** Strict use of Java `BigDecimal` and deterministic remainder distribution to prevent floating-point rounding errors.
- **Schema Control:** Version-controlled database migrations via Flyway instead of automatic ORM schema generation.
