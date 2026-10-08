# Frontend

TheTechWire's browser application is built with Angular 17, TypeScript, RxJS, and standalone components. Angular CLI serves the development app and creates the production bundle.

## Architecture

```text
Browser URL
  → lazy route in app.routes.ts
  → feature standalone component
  → core service (HttpClient)
  → auth interceptor adds Bearer JWT when available
  → Spring Boot API (backend/)
```

- `src/main.ts`, `src/app/app.config.ts`: application bootstrap, router, HTTP client/interceptor, and animations.
- `src/app/app.routes.ts`: lazy-loaded feature routes, authentication guard, and admin guard.
- `src/app/features/`: page-level features and their templates/styles.
- `src/app/core/services/`: API access and authentication/session state.
- `src/app/core/models/`: frontend API models.
- `src/app/core/interceptors/`: adds the bearer token to outgoing HTTP requests.
- `src/app/core/guards/`: UI navigation guards; server-side authorization is still required.
- `src/app/shared/`: reusable UI components.
- `src/environments/`: development and production API base URLs.

## Routes

| URL | Feature | Access |
|---|---|---|
| `/` | Public home/feed | Public |
| `/trails`, `/trails/:slug` | Story Trails list/detail | Public |
| `/post/:slug` | Article detail and discussion | Public |
| `/groups`, `/groups/:id` | Group discovery, public discussion, and private group previews | Public |
| `/gk` | News headlines | Public |
| `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify-email` | Account flows | Public |
| `/write`, `/write/:id`, `/my-stories`, `/account` | Editor, author stories, account | Authenticated |
| `/admin` | Admin dashboard | Authenticated admin |

Routes use `loadComponent`, so feature pages are loaded lazily by the Angular router.

## Local setup

Requirements: Node.js/npm compatible with Angular 17 and a running backend. From `frontend/`:

```powershell
npm ci
npm start
```

The dev server runs at `http://localhost:4200`. The development environment calls `http://localhost:8080/api`; start and configure the backend as described in [../backend/README.md](../backend/README.md).

## Configuration

- `src/environments/environment.ts` sets `apiUrl` to `http://localhost:8080/api`.
- `src/environments/environment.prod.ts` currently sets `apiUrl` to `https://api.thewire.app/api`.

Update the production environment before building for a different deployment. These values are bundled into client code and must not contain secrets.

## Main feature areas

- Public post feed, search/filtering, post detail, comments, sharing, and sponsorship placements.
- Author post creation/editing, draft/publish/archive actions, and personal story list.
- Story Trail list and detail pages, plus groups with public following and private invite/request access.
- Authentication, email verification, password reset, and profile management.
- Reader Pulse, GK news, notifications, and admin/moderation workflows.

The client persists the authenticated user/JWT under `wireblog.auth` in `localStorage`; `AuthService` restores it on startup and the interceptor adds it to API requests. This is convenient for a SPA but exposes tokens to JavaScript executing in the origin, so assess an HttpOnly-cookie/session design and XSS defenses before production.

## Build and verification

```powershell
npm run build
npm run watch
```

The package currently has no test, lint, or type-check-only script. Add Angular unit/component tests and browser-level end-to-end coverage for authentication, publishing, permissions, and the main reader journeys.

## Recommended frontend improvements

1. Add route/component and service tests plus end-to-end smoke tests in CI.
2. Improve resilient session handling (expired/invalid JWT recovery) and evaluate safer token storage.
3. Add accessible keyboard/screen-reader checks and consistent loading, empty, offline, and API-error states.
4. Make deployment API URLs environment-injected at build/deploy time and validate them in CI.
5. Track the initial bundle budget and editor dependencies; load heavy editor code only on author routes if it is not already tree-shaken adequately.
