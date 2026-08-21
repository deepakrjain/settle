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

### JWT Request Lifecycle (Phase 2)

Here's a step-by-step trace of what happens when a request with a
`Authorization: Bearer <token>` header hits the server:

1. **Tomcat receives the HTTP request** — Spring Boot's embedded Tomcat
   accepts the raw TCP connection, parses HTTP, and wraps it in a
   `HttpServletRequest` object.

2. **Spring Security's FilterChain kicks in** — Before any controller
   runs, the request passes through a chain of servlet filters. Our
   `SecurityFilterChain` bean defines this chain's behavior.

3. **JwtAuthenticationFilter executes (our custom filter)** — Because we
   registered it *before* `UsernamePasswordAuthenticationFilter`, it runs
   early. It:
   - Reads the `Authorization` header.
   - Strips the `"Bearer "` prefix to get the raw JWT string.
   - Calls `JwtService.parseToken(token)`, which verifies the HMAC-SHA
     signature using our secret key and checks the expiry claim.
   - If valid, extracts the user's UUID from the token's `subject` claim.
   - Creates a `UsernamePasswordAuthenticationToken` with that UUID as
     the *principal* and sets it on `SecurityContextHolder.getContext()`.
   - If invalid (expired, tampered, malformed), it does nothing — the
     request continues as unauthenticated.

4. **Spring Security's authorization check** — The `authorizeHttpRequests`
   configuration runs. For a protected endpoint (anything not in the
   `permitAll()` list), Spring checks whether the `SecurityContext`
   contains a valid `Authentication` object.
   - If yes → request proceeds.
   - If no → Spring returns a **403 Forbidden** immediately. The
     controller never executes.

5. **DispatcherServlet routes to the controller** — Spring's central
   dispatcher uses `@RequestMapping` annotations to find the right
   controller method (e.g., `UserController.getMe()`).

6. **Controller method runs** — The method can now call
   `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`
   to retrieve the authenticated user's UUID that was placed there in
   step 3.

7. **Response flows back** — The controller returns a `ResponseEntity`,
   which Spring serializes to JSON and sends back through the filter
   chain (in reverse) and out through Tomcat.

**Key insight:** The SecurityContext is *request-scoped* (tied to the
current thread). Because we set `SessionCreationPolicy.STATELESS`, no
HTTP session is ever created — every single request must carry its own
Bearer token. This is what makes JWT auth truly stateless.

### Group Membership Guard & Transaction Boundaries (Phase 3)

1. **Transactional Boundaries (`@Transactional`)**:
   - Creating a group involves two database inserts: saving the `Group` record and saving the initial `GroupMember` record (the creator).
   - Annotating `createGroup` with `@Transactional` ensures atomicity. If saving the membership fails (e.g. database constraint violation), the database transaction is rolled back, preventing orphaned groups with no members.

2. **Reusable Authorization Guard (`GroupSecurityGuard`)**:
   - Rather than scattering membership checks across controllers or services, `GroupSecurityGuard.checkMembership(groupId, userId)` encapsulates authorization logic.
   - Throws `AccessDeniedException` if a user attempts to access or mutate a group they do not belong to, which `GlobalExceptionHandler` cleanly maps to `403 Forbidden`.
   - This component will be reused across Expense and Ledger modules to secure group-scoped resources.

### Equal Split Remainder Allocation (Phase 4)

1. **The Remainder Problem**:
   - When splitting ₹100.00 equally among 3 participants, exact division yields ₹33.3333...
   - Naive rounding (e.g. `33.33 * 3 = 99.99`) creates floating-point / rounding drift where 1 paisa is lost into thin air.
   - `float` and `double` must **never** be used for currency due to IEEE 754 binary floating-point representation inaccuracies. We strictly use Java `BigDecimal` with 2 decimal place scale.

2. **Deterministic Allocation Rule**:
   - Total amount is divided using `RoundingMode.DOWN` to 2 decimal places to get the base share per participant (`baseShare = ₹33.33`).
   - The total allocated base amount (`baseShare * count = ₹99.99`) is subtracted from the original total amount (`₹100.00`) to find the leftover remainder in smallest currency units (`1 paisa`).
   - Participant UUIDs are sorted in ascending natural order (`Collections.sort(sortedParticipants)`).
   - Extra paisa/cents (+₹0.01) are allocated one by one to the first $N$ participants in the sorted list until the remainder is fully distributed.
   - Example (₹100.00 / 3 participants): User A gets ₹33.34, User B gets ₹33.33, User C gets ₹33.33. Sum: ₹33.34 + ₹33.33 + ₹33.33 = **₹100.00 exact**.

### Strategy Pattern & Flexible Split Types (Phase 5)

1. **Strategy Pattern over Monolithic Switch Statements**:
   - Rather than maintaining a giant method with nested `switch` branches handling `EQUAL`, `PERCENTAGE`, `EXACT`, `SHARES`, and `ITEMIZED` splits, we defined a clean `SplitStrategy` interface.
   - Each strategy (`EqualSplitStrategy`, `PercentageSplitStrategy`, `ExactSplitStrategy`, `SharesSplitStrategy`, `ItemizedSplitStrategy`) is a dedicated Spring `@Component`.
   - `SplitStrategyFactory` automatically injects all strategy beans into a Map keyed by `SplitType`.
   - **Benefit (Open-Closed Principle):** Adding a new split type in the future requires creating a new strategy class without modifying any existing business logic or service code.

2. **Itemized Split Calculation & Tax/Tip Proportionality**:
   - Itemized splits allow specifying individual items with their own custom participant lists.
   - **Step A:** Each item's cost is divided equally among its participants (reusing deterministic equal split calculation).
   - **Step B:** Each user's item subtotal is accumulated across all receipt items.
   - **Step C:** If tax & tip are included, the tax/tip amount is distributed **proportionally** according to each user's item subtotal ratio: `userTaxTipShare = taxAndTip * (userItemSubtotal / totalItemsSubtotal)`.
   - **Step D:** Any rounding remainder from proportional tax/tip distribution is allocated paisa by paisa over sorted participant UUIDs.
   - **Result:** Each participant pays for their specific items plus an exact, mathematically fair share of overall tax and tip.
