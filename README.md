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

