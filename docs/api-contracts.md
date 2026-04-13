# API Contracts

All endpoints return JSON. Timestamps use ISO 8601 in UTC. IDs are UUIDs.

Authenticated endpoints require an `Authorization: Bearer <jwt>` header. Requests without a valid token receive a 401 response.

## Error Response Format

All error responses follow a consistent structure:

```json
{
  "status": 401,
  "error": "Missing or invalid authentication token"
}
```

Validation errors include a `fields` map for per-field detail:

```json
{
  "status": 400,
  "error": "Validation failed",
  "fields": {
    "durationMinutes": "must be a positive number"
  }
}
```

---

## POST /auth/login

Authenticate with credentials and receive a JWT.

**Authorization:** none

**Request body:**

```json
{
  "username": "alice",
  "password": "correct-horse-battery-staple"
}
```

**Success (200):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
}
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Invalid credentials |

---

## POST /grants

Create a time-bound access grant for the authenticated user.

**Authorization:** Bearer token required

**Request body:**

```json
{
  "resourceName": "production-db",
  "durationMinutes": 120
}
```

**Validation rules:**

| Field | Rules |
|-------|-------|
| resourceName | Required, non-blank, max 255 characters |
| durationMinutes | Required, positive integer, max 1440 |

**Success (201 Created):**

```json
{
  "id": "a3f1c9b2-7e4d-4c8a-b6d1-2f9e8a7c3b5d",
  "userId": "e7b2d1f4-3a6c-4e9b-8c5d-1f2a3b4c5d6e",
  "resourceName": "production-db",
  "issuedAt": "2026-03-15T14:30:00Z",
  "expiresAt": "2026-03-15T16:30:00Z",
  "revoked": false
}
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failed (includes `fields` map) |
| 401 | Missing or invalid token |

---

## GET /grants/{id}

Retrieve a specific grant by ID. Users can only access their own grants unless they have an admin role.

**Authorization:** Bearer token required

**Request body:** N/A

**Success (200):**

```json
{
  "id": "a3f1c9b2-7e4d-4c8a-b6d1-2f9e8a7c3b5d",
  "userId": "e7b2d1f4-3a6c-4e9b-8c5d-1f2a3b4c5d6e",
  "resourceName": "production-db",
  "issuedAt": "2026-03-15T14:30:00Z",
  "expiresAt": "2026-03-15T16:30:00Z",
  "revoked": false
}
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid token |
| 403 | Grant belongs to another user |
| 404 | Grant not found |

---

## GET /grants/active

List all active grants for the authenticated user. Active means not revoked and not expired. The user is derived from the JWT — no userId parameter is accepted.

**Authorization:** Bearer token required

**Request body:** N/A

**Success (200):**

```json
{
  "grants": [
    {
      "id": "a3f1c9b2-7e4d-4c8a-b6d1-2f9e8a7c3b5d",
      "userId": "e7b2d1f4-3a6c-4e9b-8c5d-1f2a3b4c5d6e",
      "resourceName": "production-db",
      "issuedAt": "2026-03-15T14:30:00Z",
      "expiresAt": "2026-03-15T16:30:00Z",
      "revoked": false,
      "active": true
    }
  ]
}
```

An empty `"grants": []` array is a valid response when the user has no active grants. This is not an error.

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid token |

---

## DELETE /grants/{id}

Revoke a grant. This is a soft delete — the grant record is preserved with `revoked` set to true for audit purposes. Users can only revoke their own grants unless they have an admin role.

**Authorization:** Bearer token required

**Request body:** N/A

**Success (204 No Content):** Empty response body.

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid token |
| 403 | Grant belongs to another user |
| 404 | Grant not found |

---

## GET /health

Application health check. No authentication required.

**Authorization:** none

**Request body:** N/A

**Success (200):**

```json
{
  "status": "up"
}
```
