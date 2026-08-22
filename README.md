# Settle

A group expense-splitting and debt-settlement platform built with Java 17 and Spring Boot.

## Overview
**Settle** simplifies tracking and balancing shared expenses within groups. Whether splitting dinner bills, trip expenses, or monthly apartment utilities, Settle provides flexible split strategies, robust security, and accurate financial calculations.

## Key Features

### 🔐 Authentication & User Security
- **BCrypt Password Hashing:** Raw passwords are never stored; passwords are cryptographically hashed using BCrypt.
- **Stateless JWT Auth:** Secure JSON Web Token authentication using HMAC-SHA signing.
- **User Profiles:** Registration, login, and authenticated user profile retrieval (`/api/users/me`).

### 👥 Group Management & Security
- **Atomic Group Creation:** Group creation automatically attaches the creator as the first member in a single `@Transactional` operation.
- **Membership Authorization:** Reusable security guard (`GroupSecurityGuard`) preventing unauthorized users from accessing or modifying group resources (returns HTTP 403 Forbidden).
- **Member Invitations:** Group members can invite new users to existing groups.

### 💰 Flexible Expense Engine (Strategy Pattern)
Supports 5 distinct calculation strategies with 100% financial precision (`BigDecimal`):
- **Equal:** Splits expenses evenly among participants. Handles remainder pennies/paisa deterministically to ensure zero monetary drift.
- **Percentage:** Splits by custom user percentages, strictly validating a 100% total sum.
- **Exact:** Assigns exact custom amounts per user, validating that individual shares match the total expense amount.
- **Shares:** Distributes costs proportionally based on custom share counts (e.g., 2 shares vs 1 share).
- **Itemized Receipts:** Item-by-item breakdown allowing custom participant lists per item, plus proportional distribution of tax and tip based on each participant's subtotal.

### 🛠️ Architecture & Validation
- **Custom Payload Validation:** Custom Bean Validation (`@ValidSplitData`) ensuring request payloads strictly match the chosen split strategy before hitting service logic.
- **Global Exception Handler:** Centralized `@ControllerAdvice` translating validation errors and security violations into clean, consistent JSON error responses.
- **Database Migrations:** Managed database schema versioning with Flyway (`V1` to `V4`).

## Tech Stack
- **Language:** Java 17
- **Framework:** Spring Boot 3.3.x (Spring Web, Spring Security, Spring Data JPA)
- **Database:** PostgreSQL
- **Migrations:** Flyway
- **Build Tool:** Maven
- **Containerization:** Docker Compose (local dev)

## Project Structure
Organized **by feature** for high cohesion:
- `com.settle.user` — User management, registration, and user DTOs
- `com.settle.group` — Group lifecycle, membership queries, and security guards
- `com.settle.expense` — Expense entities, split strategy implementations, and endpoints
- `com.settle.ledger` — Debt tracking and settlement logic *(Upcoming)*
- `com.settle.common` — Security config, JWT service/filter, and global exception advice

## API Overview

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/users/register` | Public | Register a new user account |
| `POST` | `/api/auth/login` | Public | Authenticate user & receive JWT |
| `GET` | `/api/users/me` | JWT | Get current authenticated user profile |
| `POST` | `/api/groups` | JWT | Create a new group |
| `GET` | `/api/groups` | JWT | List groups for the authenticated user |
| `GET` | `/api/groups/{id}` | JWT | Get group details and member roster |
| `POST` | `/api/groups/{id}/members` | JWT | Add a user to a group |
| `POST` | `/api/groups/{groupId}/expenses` | JWT | Log an expense (EQUAL, PERCENTAGE, EXACT, SHARES, ITEMIZED) |
| `GET` | `/api/groups/{groupId}/expenses` | JWT | List expenses for a group (paginated) |
| `GET` | `/api/groups/{groupId}/balances` | JWT | Get net balances for all group members |
| `GET` | `/api/groups/{groupId}/settlement-plan` | JWT | Get calculated greedy and optimal debt settlement plans |
| `POST` | `/api/groups/{groupId}/settlements` | JWT | Record an idempotent debt settlement payment |

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
   *Flyway automatically runs database migrations on startup.*

## Key Design Decisions
- **Stateless JWT Authentication:** Request-scoped security via custom filter chain integration without HTTP sessions.
- **Strategy Pattern for Splitting:** Encapsulated split algorithms (`SplitStrategy`) adhering to the Open-Closed Principle.
- **Financial Precision:** Strict use of Java `BigDecimal` and deterministic remainder distribution to prevent floating-point rounding errors.
- **Append-Only Ledger:** Financial history is preserved using immutable `ledger_entries` records (enforced by extending a bare `Repository` with no update/delete methods).
- **Settlement Minimization Algorithms (Greedy vs. Optimal):**
  - **Greedy ($O(N \log N)$):** Repeatedly settles the largest debtor with the largest creditor. Fast, predictable, and scales to thousands of users. In practice, it produces near-optimal transaction counts for almost all real-world group balances.
  - **Optimal ($O(2^N)$):** Solves the NP-Hard subset-sum partitioning problem using recursive backtracking to find the absolute minimum number of transactions. Because of its exponential complexity, it is restricted to groups with under 10 active balances.
  - **Production Choice:** Production platforms (like Splitwise) default to greedy algorithms at scale because $O(N \log N)$ executes in sub-millisecond time, avoiding CPU exhaustion while delivering a simple, intuitive debt simplification plan.
- **Idempotent Settlements & Race Condition Defense (Phase 8):**
  - **Why DB Constraints Matter:** Application-level `findByIdempotencyKey()` checks are subject to race conditions if two identical requests arrive simultaneously before either has committed. The database unique constraint on `idempotency_key` guarantees true atomicity via database page/index locks.
  - **Graceful Error Recovery:** When a race condition occurs, `SettlementService` catches `DataIntegrityViolationException`, retrieves the already-committed `Settlement` object, and returns it cleanly to the caller without surfacing a 500 error or creating duplicate payments.
- **Payment Gateway Retries & Exponential Backoff (Phase 9):**
  - **Exponential Backoff Multiplier:** Given `@Backoff(delay = 500, multiplier = 2)` and `maxAttempts = 3`:
    - **Attempt 1 ($t = 0$ ms):** Initial execution.
    - **Attempt 2 ($t = 500$ ms):** 1st retry executed after a $500$ ms delay ($500 \times 2^0$).
    - **Attempt 3 ($t = 1500$ ms):** 2nd retry executed after a $1000$ ms delay ($500 \times 2^1$).
  - **Fallback Recovery (`@Recover`):** If all 3 attempts fail due to temporary network timeouts (`PaymentGatewayException`), Spring Retry invokes `@Recover`. The settlement is saved with status `FAILED` and no debt-reversing `LedgerEntry` is written, preventing ambiguous or partially-committed states.
- **Transaction-Aware Real-Time WebSocket Broadcasting (Phase 10):**
  - **Why `AFTER_COMMIT` is Critical:** Publishing WebSocket events inside an active, uncommitted transaction risks broadcasting "phantom" balance updates if the transaction rolls back. Additionally, clients receiving the push notification who immediately query the REST API might hit the database before the writing transaction commits, leading to dirty/stale reads.
  - **Spring Transaction Synchronization:** Using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`, Spring defers broadcasting STOMP messages to `/topic/groups/{groupId}/balances` until PostgreSQL has confirmed the transaction commit. This guarantees eventual consistency across all connected clients without polling.
- **Comprehensive Integration Testing with Testcontainers (Phase 11):**
  - **Real Database Integration:** Utilizes Testcontainers to launch a real Dockerized PostgreSQL instance for integration tests, automatically applying all Flyway migrations (`V1` to `V6`).
  - **Transaction Rollback Proof:** Validates that `@Transactional` boundaries guarantee zero partial writes if an operation fails mid-execution.
  - **Real Concurrency & Constraint Testing:** Tests idempotency against PostgreSQL's actual database unique constraint engine rather than mocked behavior.



