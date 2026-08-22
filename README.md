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
When a user logs an expense, an `Expense` entity, its `ExpenseSplit` records, and the corresponding `LedgerEntry` debt rows must all be saved together.

In a microservices setup with separate `ExpenseService` and `LedgerService` databases, network failures or crashes mid-request leave data inconsistent unless complex distributed sagas or outbox patterns are implemented.

By adopting a **Modular Monolith architecture**, Spring Boot executes expense creation and ledger entry generation inside a **single ACID database transaction** (`@Transactional`). If any operation fails, PostgreSQL performs an immediate rollback — guaranteeing 100% data consistency with zero partial writes and zero inter-service network latency.

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

## Key Design Decisions

- **Strategy Pattern for Splitting:** Encapsulates split algorithms (`EQUAL`, `PERCENTAGE`, `EXACT`, `SHARES`, `ITEMIZED`) in dedicated `SplitStrategy` beans. New split types can be added without modifying core service code (Adheres to Open-Closed Principle).
- **Append-Only Immutable Ledger:** `ledger_entries` records are strictly immutable. `LedgerEntryRepository` extends Spring's bare `Repository` interface (excluding update/delete signatures), enforcing immutability at compile time.
- **Debt Minimization Algorithms (Greedy vs. Optimal):**
  - **Greedy ($O(N \log N)$):** Uses priority queues to settle largest creditors/debtors first. Sub-millisecond execution, scales to thousands of members.
  - **Optimal ($O(2^N)$):** Backtracking subset-sum algorithm that guarantees true minimal transaction count for small groups ($<10$ members).
- **Idempotency & Race Condition Defense:** Concurrent duplicate payments are caught at the database level via a `UNIQUE` constraint on `idempotency_key`, recovering from `DataIntegrityViolationException` without generating duplicate ledger entries.
- **Payment Retries & Backoff:** Uses `@Retryable` with exponential backoff ($500$ ms delay, $2\times$ multiplier) for gateway timeouts, with `@Recover` setting payment status to `FAILED` if exhausted.
- **Transaction-Aware WebSockets:** Balance updates are published via `@TransactionalEventListener(phase = AFTER_COMMIT)` to ensure STOMP frames are sent only after PostgreSQL commits, preventing phantom updates on rollback.

---

## License
MIT License. Built for educational and portfolio demonstration purposes.
