# Production deployment

This application consists of two independent deployments:

```text
Browser -> Angular static site -> Spring Boot API -> PostgreSQL
                              -> NewsAPI
```

For a free hobby deployment, host both services on Render and use an external PostgreSQL provider such as Neon. Render's free web service sleeps when idle and has ephemeral storage; its free PostgreSQL databases expire after 30 days. This setup is appropriate for a demo, not production.

## 1. Create a PostgreSQL database

Create a PostgreSQL project with your database provider and keep its connection details private. Render and Neon are separate services, so use the provider's public connection host and require SSL. Construct `DB_URL` in JDBC format:

```text
jdbc:postgresql://<host>:5432/<database>?sslmode=require
```

Start with a new, empty database. The production profile runs Flyway migrations on startup.

## 2. Deploy the backend on Render

In Render, create a **Web Service** from the GitHub repository. Use:

| Setting | Value |
| --- | --- |
| Root Directory | `backend` |
| Runtime | Docker |
| Dockerfile Path | `Dockerfile` |
| Docker Build Context Directory | `.` |
| Instance Type | Free (demo use only) |
| Health Check Path | `/actuator/health` |

The Dockerfile builds the Java 17 Spring Boot application. Add the environment variables listed in [`backend/.env.example`](backend/.env.example) in Render's Environment tab. For the initial deployment, set `FRONTEND_URL` and `CORS_ALLOWED_ORIGIN_PATTERNS` to temporary values; update them after the static site is created.

Use the exact external database connection values for `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Generate a unique `JWT_SECRET` with at least 32 random bytes. Enter the NewsAPI key in Render's private environment settings; never commit it. Leave uploads disabled for real user content until object storage is configured: Render's free filesystem is ephemeral, so uploaded images may disappear after a restart, sleep, or deploy.

Deploy and note the service URL, for example `https://wireblog-api.onrender.com`. Check that `<service-url>/actuator/health` returns HTTP 200. The first request after an idle period can take a while while the free service wakes.

## 3. Point the frontend at the deployed API

Before deploying the frontend, change `frontend/src/environments/environment.prod.ts` so `apiUrl` is the Render service URL followed by `/api`, for example:

```ts
apiUrl: 'https://wireblog-api.onrender.com/api'
```

This value is compiled into the browser bundle. It is public configuration, not a secret, and changing it requires a new frontend build/deployment.

Create a Render **Static Site** from the same repository:

| Setting | Value |
| --- | --- |
| Root Directory | `frontend` |
| Build Command | `npm ci && npm run build` |
| Publish Directory | `dist/wireblog-frontend/browser` |

Add an SPA rewrite so all non-file routes serve `/index.html`; otherwise refreshing a story or group link may return 404.

## 4. Configure the final API variables

Once Render assigns the static site URL, update these backend variables and redeploy:

```dotenv
FRONTEND_URL=https://wireblog.onrender.com
CORS_ALLOWED_ORIGIN_PATTERNS=https://wireblog.onrender.com
UPLOAD_BASE_URL=https://wireblog-api.onrender.com/uploads
```

Replace the example hosts with the actual Render URLs. `UPLOAD_BASE_URL` must be publicly reachable and must not contain `/api`.

## 5. Launch checks

- Confirm `/actuator/health` returns 200.
- Register an account and confirm it signs in immediately.
- Log in from the deployed frontend; browser developer tools should show no CORS errors.
- Do not upload important images while using Render's free ephemeral filesystem. Move uploads to object storage before relying on them.
- Enable database backups and perform a restore test before storing important data.
