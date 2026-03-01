# spring-jwt-auth

**Production-ready JWT authentication microservice for Spring Boot.**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6-6DB33F?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=flat-square)](LICENSE)

A battle-hardened JWT authentication boilerplate with refresh token rotation, role-based access control (RBAC), and secure stateless session management. Drop it into any Spring Boot microservice as a starting point.

---

## Features

- **Access + Refresh Token Pair** — Short-lived access tokens (15 min) with long-lived refresh tokens (7 days).
- **Refresh Token Rotation** — Every refresh issues a new token and revokes the old one, preventing replay attacks.
- **Role-Based Access Control** — `ROLE_USER`, `ROLE_ADMIN`, `ROLE_MODERATOR` with Spring Security `@PreAuthorize`.
- **BCrypt Password Hashing** — Cost factor 12 by default.
- **Stateless Sessions** — No server-side session state; fully JWT-based.
- **Token Revocation** — Logout revokes the refresh token in the database.
- **JPA + H2 (dev) / PostgreSQL (prod)** — One config change to switch databases.
- **Input Validation** — Bean Validation on all request DTOs.

---

## Quick Start

```bash
git clone https://github.com/rawqubit/spring-jwt-auth
cd spring-jwt-auth
mvn spring-boot:run
```

The service starts on port `8081`.

---

## API Reference

### Register

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "email": "alice@example.com", "password": "securePass123"}'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "a1b2c3d4...",
  "email": "alice@example.com"
}
```

### Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com", "password": "securePass123"}'
```

### Refresh Access Token

```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "550e8400-e29b-41d4-a716-446655440000"}'
```

### Logout (Revoke Refresh Token)

```bash
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "550e8400-e29b-41d4-a716-446655440000"}'
```

### Access a Protected Endpoint

```bash
curl http://localhost:8081/api/v1/protected \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## Security Architecture

```
Client
  │
  ├─ POST /auth/register  ──► UserService.register()
  │                              └─ BCrypt password
  │                              └─ Save User + issue tokens
  │
  ├─ POST /auth/login     ──► AuthenticationManager.authenticate()
  │                              └─ Issue access token (15m) + refresh token (7d)
  │
  ├─ GET  /api/protected  ──► JwtAuthFilter
  │                              └─ Extract Bearer token
  │                              └─ JwtService.isTokenValid()
  │                              └─ Set SecurityContext
  │
  └─ POST /auth/refresh   ──► RefreshTokenService
                               └─ Validate token (not expired, not revoked)
                               └─ Rotate: revoke old, issue new pair
```

---

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `jwt.secret` | (env: `JWT_SECRET`) | Base64-encoded 256-bit HMAC secret |
| `jwt.access-token-expiry-ms` | `900000` (15 min) | Access token lifetime |
| `jwt.refresh-token-expiry-ms` | `604800000` (7 days) | Refresh token lifetime |

### Production Setup

```bash
# Generate a secure secret
openssl rand -base64 32

# Set environment variables
export JWT_SECRET=<generated-secret>
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/jwtauth
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=<db-password>

mvn spring-boot:run
```

---

## Role-Based Access Control

Protect endpoints with `@PreAuthorize`:

```java
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<List<User>> getAllUsers() { ... }

@GetMapping("/profile")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public ResponseEntity<UserProfile> getProfile() { ... }
```

---

## Running Tests

```bash
mvn test
```

---

## License

MIT — see [LICENSE](LICENSE).
