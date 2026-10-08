# Backend

Spring Boot 3.3.4 / Java 17 REST API for TheTechWire. The backend owns business rules, authentication and authorization, persistence, headline refresh, and media upload handling.

## Architecture

```text
HTTP request
  → SecurityConfig / JWT authentication / RateLimitFilter
  → controller (route + request validation)
  → service (business rules and transaction boundary)
  → Spring Data repository
  → JPA entity / Hibernate
  → PostgreSQL
```

- `config/`: Spring Security, JWT, static upload resources, and rate limiting.
- `controller/`: REST endpoints grouped by feature.
- `dto/`: API request and response types; request bodies use Jakarta validation where applicable.
- `service/`: use cases, authorization checks, sanitization, and external integration logic.
- `repository/`: Spring Data JPA queries.
- `model/`: JPA entities and enums.
- `exception/`: API exception and error response handling.
- `src/main/resources/application.yml`: default and `prod` profile configuration.
- `src/main/resources/schema.sql`: SQL initialization/upgrade statements used outside the `prod` profile.

## API routes

Unless noted otherwise, routes require a valid bearer token. Public/admin access is also enforced by `SecurityConfig`; the table is a quick index, not a substitute for endpoint authorization checks.

| Feature | Routes |
|---|---|
| Authentication | `POST /api/auth/register`, `/login`, `/verify-email?token=...`, `/password-reset/request`, `/password-reset/confirm` |
| Current user | `GET /api/users/me`, `PATCH /api/users/me` |
| Posts | `GET /api/posts` (page, `tag`, or `q`), `GET /api/posts/mine`, `GET /api/posts/{slug}`, `GET /api/posts/{id}/edit`, `POST /api/posts`, `PUT /api/posts/{id}`, `POST /api/posts/{id}/publish`, `POST /api/posts/{id}/archive`, `DELETE /api/posts/{id}` |
| Story Trails | `GET /api/trails`, `GET /api/trails/{slug}` |
| Groups | `GET /api/groups`, `GET /api/groups/{id}` (public discovery and public-group reads; private groups return a content-free preview to non-members), `POST /api/groups` |
| Group access | `POST /api/groups/{id}/follow` (public groups), `POST /api/groups/{id}/join-requests`, `PATCH /api/groups/{id}/join-requests/{requestId}`, `POST /api/groups/{id}/members` (admin invite), `PATCH /api/groups/{id}` (admin settings) |
| Group discussion | `POST /api/groups/{id}/posts`, `POST /api/groups/{id}/posts/{postId}/comments`, corresponding delete routes |
| Comments | `GET /api/comments/post/{postId}`, `POST /api/comments/post/{postId}`, `POST /api/comments/{commentId}/flag`, `DELETE /api/comments/{commentId}` |
| Sharing | `POST /api/posts/{postId}/share` |
| Reader Pulse | `GET /api/pulses/posts/{postId}`, `POST /api/pulses/posts/{postId}` (currently public) |
| News | `GET /api/news/headlines`, optionally `?category=technology` (public) |
| Notifications | `GET /api/notifications`, `GET /api/notifications/unread-count`, `PATCH /api/notifications/read-all` |
| Uploads | `POST /api/upload` with multipart field `file`; returns a local `/uploads/...` URL |
| Sponsorships | `GET /api/sponsorships/active?placement=...` and `POST /api/sponsorships/{id}/click` are public |
| Admin | `/api/admin/sponsorships`, `/comments/flagged`, `/comments/{id}/flag`, `/comments/{id}`, `/posts`, `/posts/{id}/publish`, `/posts/{id}/archive`, `/posts/{id}`, `/users`, `/users/{id}/enabled`, `/users/{id}/role`, `/users/{id}`; admin routes require `ROLE_ADMIN` |

Posts, news, comment reads, Story Trails, public group discovery/detail, public Reader Pulse routes, static uploads, active sponsorships, and sponsorship clicks are public according to the current security configuration. Private group members and posts are returned only to members or group managers. Mutations—including following, requesting access, and all group discussions—require authentication; membership and management rules are enforced by the controller. Other routes require authentication unless covered by the admin rules. The backend must remain the final authorization boundary.

## Local setup

Requirements: Java 17, Maven, and PostgreSQL.

1. Create a database named `wireblog`.
2. The default profile currently connects to `jdbc:postgresql://localhost:5432/wireblog` with username `postgres` and password `root`. Change `spring.datasource.*` in `src/main/resources/application.yml`, or override with Spring Boot's standard `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` environment variables.
3. Optionally set `NEWS_API_KEY` for live headlines. Without it, the app serves cached headlines (if any) and the fallback response.
4. Run from `backend/`:

   ```powershell
   mvn spring-boot:run
   ```

The API listens on port `8080`.

## Configuration

| Setting | Default/local behavior | `prod` profile |
|---|---|---|
| Datasource | Local PostgreSQL URL and credentials described above | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` are required |
| `JWT_SECRET` | Has an insecure development fallback in `application.yml`; override it for local use | Required; use a high-entropy key of at least 256 bits |
| `NEWS_API_KEY` | Optional; live news is skipped when empty | Required by the profile |
| `FRONTEND_URL` | Defaults to `http://localhost:4200` for account links | Set to the deployed frontend origin |
| `APP_UPLOAD_DIR` / `APP_UPLOAD_BASE_URL` | Local `uploads/` directory and localhost URL | Set to durable storage/public URL if deploying; storage code is still local-disk based |

In the `prod` profile, Hibernate uses `ddl-auto: validate` and Flyway migrations are enabled. In the default profile, Hibernate uses `ddl-auto: update` and Spring runs `schema.sql`. Although the H2 runtime dependency remains, the active default datasource is PostgreSQL; H2 is not the configured default database.

## Security and operational behavior

- JWT bearer authentication is stateless; password hashes use BCrypt.
- The current frontend stores the JWT in browser `localStorage`; see the frontend guide for its implications.
- `RateLimitFilter` applies a fixed in-memory limit to selected hotspots. It is per application instance and trusts the first `X-Forwarded-For` value when present; configure a trusted proxy and replace this approach before scaling.
- Uploads are limited to 5 MB and JPEG/PNG/GIF/WebP signatures are checked, but files are stored on local disk.
- `EmailService` is development-only: it logs verification and reset URLs, including tokens. It does not send email and must be replaced before production use. Verification tokens currently have no expiry, and login does not require `emailVerified` to be true.
- Flyway migrations are versioned under `src/main/resources/db/migration` for production. The hand-maintained `schema.sql` remains the default-profile initialization path and must be kept aligned.

## Build and tests

From `backend/`:

```powershell
mvn test
mvn package
```

`mvn test` runs `WireBlogApplicationFlowTest`, an API-level workflow suite using Spring Boot, MockMvc, and an isolated in-memory H2 database. It exercises story publishing/editing/search/deletion, comments and notifications, uploads, Reader Pulse and sharing, public/private group discovery and membership moderation, friend requests, and administrator moderation/user/sponsorship operations. Run only this suite with:

```powershell
mvn -Dtest=WireBlogApplicationFlowTest test
```

The H2 suite verifies application/API behavior but does not replace PostgreSQL migration/upgrade tests or browser-level frontend tests. No frontend test or lint runner is currently configured.

## Recommended backend improvements

1. Replace log-based account email links with a provider, stop logging raw tokens, add verification-token expiry, and enforce the intended unverified-account policy.
2. Add Flyway/Liquibase migrations; remove local schema drift and verify upgrades against existing databases.
3. Add automated security, service, repository, and API tests; cap page sizes and consider pagination for admin user listing.
4. Move credentials out of tracked configuration, enforce environment-specific CORS, and use shared rate limiting behind trusted proxy configuration.
5. Move uploads to object storage with retention/cleanup and production URL configuration.
