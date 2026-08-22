# Settle

![CI Status](https://github.com/deepakrjain/settle/actions/workflows/ci.yml/badge.svg)

**Settle** is a group expense-splitting and debt-settlement platform built with Java 17, Spring Boot 3.3, PostgreSQL, Flyway, and React 18. Designed as a production-grade **Modular Monolith**, Settle provides flexible split strategy engines, immutable append-only ledger tracking, debt simplification algorithms, real-time WebSocket updates, and idempotent payment processing.

---

## Overview

Managing shared group expenses (dinners, vacation trips, apartment utilities) often involves complex calculations, currency rounding drift, and tangled web debts where everyone owes everyone. **Settle** solves this by:
1. Supporting 5 flexible split strategies (Equal, Percentage, Exact, Shares, Itemized receipts) with 100% financial precision (`BigDecimal`).
2. Maintaining an immutable, append-only ledger for transparent financial auditability.
3. Automatically running debt minimization algorithms (Greedy $O(N \log N)$ and Optimal $O(2^N)$ subset-sum backtracking) to simplify complex group debts down to the fewest possible transactions.
4. Broadcasting live balance updates over WebSockets without HTTP polling.

---

## Architecture: Why a Modular Monolith?

Settle is structured strictly **by feature** (`com.settle.user`, `com.settle.group`, `com.settle.expense`, `com.settle.ledger`) within a single deployment unit.

```
com.settle
├── user/         # User entities, BCrypt security, DTOs, and controllers
├── group/        # Group & GroupMember entities, @GroupMemberOnly security guards
├── expense/      # Expense engine, strategy implementations (Equal, Percentage, etc.)
├── ledger/       # Immutable LedgerEntry, debt minimization algorithms, settlements
└── common/       # SecurityConfig, JWT auth filters, WebSockets, OpenApiConfig
```

### The Transactional Consistency Tradeoff
In an expense platform, when a user logs an expense:
1. An `Expense` entity and its `ExpenseSplit` records must be saved.
2. Corresponding `LedgerEntry` rows (recording participant debts to the payer) must be written.

If these two operations were split across two separate microservices (e.g. `ExpenseService` REST API calling `LedgerService` REST API):
- A network partition or crash between the calls would leave the system in an inconsistent state: an expense exists in history, but nobody owes money in the ledger.
- Solving this in a microservices architecture requires complex distributed sagas, compensating transactions, outbox patterns, and eventual consistency handling.

By choosing a **Modular Monolith architecture**, Spring Boot executes expense creation and ledger entry generation inside a **single ACID database transaction** (`@Transactional`). If any step fails, PostgreSQL issues a complete rollback — guaranteeing 100% data consistency with zero partial writes, zero network latency, and simple operational maintenance.

---

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.3.x (Spring Web, Spring Security, Spring Data JPA, Spring Retry, Spring WebSocket STOMP)
- **Database & Migrations:** PostgreSQL 15, Flyway Schema Versioning
- **Frontend:** React 18, TypeScript, Vite, Custom Glassmorphic CSS System, Lucide Icons
- **Testing:** JUnit 5, Mockito, Testcontainers (real Dockerized PostgreSQL testing)
- **Containerization & CI:** Docker, Docker Compose, GitHub Actions

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Node.js 18+ (for frontend dev server)

### 1. Local Development with Docker Compose
Start PostgreSQL and the Spring Boot application simultaneously:
```bash
docker compose up --build
```
- **Backend API:** `http://localhost:8080`
- **Interactive Swagger UI:** `http://localhost:8080/swagger-ui.html`

### 2. Running Frontend
```bash
cd frontend
npm install
npm run dev
```
- **Vite Frontend:** `http://localhost:3000` (automatically proxies `/api` and `/ws` to `localhost:8080`)

### 3. Running Automated Test Suite
To run all unit tests (Mockito) and Testcontainers integration tests against a real PostgreSQL container:
```bash
mvn test
```

---

## API Overview

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/users/register` | Public | Register a new user account |
| `POST` | `/api/auth/login` | Public | Authenticate user & receive JWT access token |
| `GET` | `/api/users/me` | JWT | Get current authenticated user profile |
| `POST` | `/api/groups` | JWT | Create a new group (creator attached automatically) |
| `GET` | `/api/groups` | JWT | List groups for the authenticated user |
| `GET` | `/api/groups/{id}` | JWT | Get group details and member roster |
| `POST` | `/api/groups/{id}/members` | JWT | Add a user to a group roster |
| `POST` | `/api/groups/{groupId}/expenses` | JWT | Log an expense (EQUAL, PERCENTAGE, EXACT, SHARES, ITEMIZED) |
| `GET` | `/api/groups/{groupId}/expenses` | JWT | List expenses for a group (paginated) |
| `GET` | `/api/groups/{groupId}/balances` | JWT | Get net balances for all group members |
| `GET` | `/api/groups/{groupId}/settlement-plan` | JWT | Get calculated Greedy and Optimal debt settlement plans |
| `POST` | `/api/groups/{groupId}/settlements` | JWT | Record an idempotent debt settlement payment |

*Full interactive documentation with JWT authorization testing is available via Swagger UI at `/swagger-ui.html`.*

---

## Key Design Decisions & Interview Notes

### 1. Strategy Pattern for Flexible Expense Splitting
To support multiple split mechanisms (`EQUAL`, `PERCENTAGE`, `EXACT`, `SHARES`, `ITEMIZED`) without writing monolithic `switch` statements, Settle uses the **Strategy Pattern**:
- `SplitStrategy` defines the common contract: `calculateSplits(BigDecimal totalAmount, CreateExpenseRequest request)`.
- Individual strategy implementations (`EqualSplitStrategy`, `PercentageSplitStrategy`, etc.) encapsulate specific calculation and validation logic.
- A `SplitStrategyFactory` backed by Spring dependency injection (`Map<String, SplitStrategy>`) dynamically resolves the appropriate bean based on `SplitType`.
- **Open-Closed Principle (OCP):** Adding a new split type (e.g., `TIME_BASED`) simply requires adding a new `SplitStrategy` bean without mutating existing service code.

### 2. Append-Only Immutable Ledger
Financial systems require absolute auditability. In Settle:
- `ledger_entries` table is **append-only**. Rows are inserted when expenses or settlements occur, but never updated or deleted.
- `LedgerEntryRepository` deliberately extends Spring's bare `Repository` interface (rather than `JpaRepository` or `CrudRepository`), exposing **only** `save()` and read methods.
- Compiler-enforced immutability: Developers literally cannot call `delete()` or update methods because those signatures do not exist on the repository interface.

### 3. Settlement Minimization Algorithms (Greedy vs. Optimal)
Minimizing transaction count across $N$ group members is equivalent to the **Subset Sum Problem** (NP-Hard in decision form).
- **Greedy Algorithm ($O(N \log N)$):** Uses priority queues to repeatedly pair the max debtor with the max creditor. Fast, sub-millisecond execution, and scales seamlessly to thousands of users.
- **Optimal Algorithm ($O(2^N)$):** Exact subset-sum partitioning using recursive backtracking to find zero-sum sub-groups. Because of its exponential time complexity, it is restricted to groups with under 10 active balances.
- **Production Tradeoff:** Real-world platforms (like Splitwise) default to Greedy at scale because $O(N \log N)$ avoids CPU exhaustion while delivering transaction counts within 1 payment of the theoretical minimum.

### 4. Idempotency & Database Unique Constraints
Application-level `existsByIdempotencyKey()` checks are vulnerable to race conditions when concurrent duplicate requests arrive before the first transaction commits.
- Settle enforces a database-level `UNIQUE` constraint on `idempotency_key`.
- Under race conditions, PostgreSQL index locks force the second insert to fail with `DataIntegrityViolationException`.
- `SettlementService` catches `DataIntegrityViolationException`, retrieves the already-committed `Settlement` record, and returns it gracefully without surfacing a 500 error or creating duplicate payments.

### 5. Payment Gateway Retries & Exponential Backoff
External payment processing can suffer from transient network timeouts:
- Annotated with `@Retryable(retryFor = PaymentGatewayException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))`.
- **Backoff Delays:** Attempt 1 ($t=0$ ms), Attempt 2 ($t=500$ ms delay), Attempt 3 ($t=1000$ ms delay). Total time: $1500$ ms.
- **Circuit-Breaker Recovery (`@Recover`):** If all 3 attempts fail, `@Recover` sets settlement status to `FAILED` and skips creating debt-reversing ledger entries, preventing ambiguous state.

### 6. Transaction-Aware Real-Time WebSockets
Broadcasting WebSocket updates *inside* an active transaction risks publishing "phantom" balance notifications if the transaction later rolls back.
- Settle uses Spring's `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
- STOMP messages to `/topic/groups/{groupId}/balances` are deferred until PostgreSQL confirms the transaction commit, guaranteeing eventual consistency across connected clients without polling.

---

## License
MIT License. Built for educational and portfolio demonstration purposes.
