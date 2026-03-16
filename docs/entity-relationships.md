# Entity Relationships

## Entities

### User

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | Primary key, generated |
| username | String | Required, unique, max 50 characters |
| password | String | Required, stored as bcrypt hash |
| roleId | UUID | Foreign key to Role, required |

### Role

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | Primary key, generated |
| name | String | Required, unique (e.g. USER, ADMIN) |

### AccessGrant

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | Primary key, generated |
| userId | UUID | Foreign key to User, required |
| resourceName | String | Required, non-blank, max 255 characters |
| issuedAt | Timestamp | Required, set at creation (UTC) |
| expiresAt | Timestamp | Required, computed as issuedAt + durationMinutes (UTC) |
| revoked | Boolean | Required, defaults to false |

## Relationships

### User to Role — many-to-one

Each user has exactly one role. Multiple users can share the same role. A `roleId` foreign key on the User table references Role. This is sufficient for the current scope where only USER and ADMIN roles exist. Migration to many-to-many (via a join table) is planned for the PAM iteration when group-based policies and RBAC require users to hold multiple roles.

### User to AccessGrant — one-to-many

A user can have many access grants. Each grant belongs to exactly one user. The `userId` field on AccessGrant is stored as a plain UUID foreign key rather than a JPA entity reference. This keeps grant queries simple and avoids unintended eager loading of User objects.

### Role to AccessGrant — no direct relationship

Grants do not reference roles. A grant records which user requested access to which resource and when that access expires. Role determines whether a user is authorized to perform certain actions (e.g. admins can view and revoke any grant), but that authorization is enforced in the service and security layers, not in the data model.

## Indexes

| Table | Column(s) | Rationale |
|-------|-----------|-----------|
| User | username | Queried on every login |
| AccessGrant | userId | Queried on nearly every grant operation |
| AccessGrant | expiresAt, revoked | Used by the scheduled expiration job to find grants that are past due but not yet marked revoked |

## Constraints

**User.username** has a unique constraint. Duplicate usernames would break authentication.

**Role.name** has a unique constraint. Role names are identifiers used in security configuration and must not be ambiguous.

**AccessGrant** does not enforce uniqueness on (userId, resourceName). A user may hold multiple grants for the same resource — for example, one that is expired or revoked and a new one that is active. Restricting this at the database level would require filtering by status, which adds complexity without meaningful benefit at this scale. The service layer can enforce single-active-grant-per-resource logic if needed.

## Design Notes

**No Resource entity.** The `resourceName` field on AccessGrant is a freeform string. This project tracks the lifecycle of access grants (creation, expiration, revocation) but does not protect or enforce access on real resources. Resource-level access control, including a Resource entity, policy evaluation, and approval workflows, is deferred to the PAM iteration.

**Plain UUID foreign keys over JPA entity references.** AccessGrant stores `userId` as a UUID rather than mapping a `@ManyToOne` relationship to User. This avoids lazy loading concerns, keeps the entity simple, and aligns with the API contract where `userId` is returned as a UUID string.

**Soft deletes for revocation.** The `revoked` boolean flag preserves grant records for audit purposes. No rows are deleted from the AccessGrant table during normal operation.
