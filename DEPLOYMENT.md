# Production deployment

This application consists of two independent deployments:

```text
Browser -> Angular static site -> Spring Boot API -> PostgreSQL
                              -> NewsAPI
```

Use a static host for `frontend/` (Vercel, Netlify, Cloudflare Pages, or similar) and a Java host with a persistent disk for `backend/` (Render, Railway, Fly.io, or similar). Attach a managed PostgreSQL database to the backend host.

## 1. Deploy the backend

Create a web service with these settings:

| Setting | Value |
| --- | --- |
| Root directory | `backend` |
| Runtime | Java 17 |
| Build command | `mvn -DskipTests package` |
| Start command | `java -jar target/wireblog-backend-1.0.0.jar` |
| Health-check path | `/actuator/health` |

Add each variable listed in [`backend/.env.example`](backend/.env.example) to the host's encrypted environment-variable dashboard. Do **not** upload or commit a file containing production values.

Important values:

- Set `SPRING_PROFILES_ACTIVE=prod`.
- Use the database provider's PostgreSQL connection details with the `jdbc:postgresql://` prefix for `DB_URL`. Prefer an internal/private URL when both services run on the same provider.
- Generate `JWT_SECRET` with at least 32 random bytes, for example: `openssl rand -base64 48`. Never rotate it casually, because rotation logs everyone out.
- Set `FRONTEND_URL` and `CORS_ALLOWED_ORIGIN_PATTERNS` to the exact public frontend origins. Include both apex and `www` only if both are actually used.
- The current upload implementation writes to local disk. Mount a persistent volume and point `UPLOAD_DIR` at it. For multiple API replicas, migrate uploads to object storage before scaling.

After deployment, note the API URL, such as `https://api.example.com`. Verify `https://api.example.com/actuator/health` returns HTTP 200.

## 2. Point the frontend at the deployed API

Before building the frontend, change `frontend/src/environments/environment.prod.ts` so `apiUrl` is your API origin followed by `/api`, for example:

```ts
apiUrl: 'https://api.example.com/api'
```

This value is compiled into the browser bundle. It is public configuration, not a secret, and changing it requires a new frontend build/deployment.

Deploy the frontend with:

| Setting | Value |
| --- | --- |
| Root directory | `frontend` |
| Build command | `npm ci && npm run build` |
| Publish directory | `dist/frontend/browser` |

Configure an SPA rewrite/fallback so all non-file routes serve `index.html`; otherwise refreshing a post URL will return 404.

## 3. Configure the final API variables

Once the frontend host assigns its real URL, set these backend variables and redeploy the backend:

```dotenv
FRONTEND_URL=https://www.example.com
CORS_ALLOWED_ORIGIN_PATTERNS=https://www.example.com,https://example.com
UPLOAD_BASE_URL=https://api.example.com/uploads
```

If the API is not served from `api.example.com`, replace it with the actual API service origin. `UPLOAD_BASE_URL` must be publicly reachable and must not contain `/api`.

## 4. Launch checks

- Confirm `/actuator/health` returns 200.
- Register an account and confirm it signs in immediately.
- Log in from the deployed frontend; browser developer tools should show no CORS errors.
- Upload an image, redeploy the backend, and confirm the image still exists. If it does not, the disk is ephemeral and a persistent volume/object storage is required.
- Enable automated database backups and perform one restore drill before accepting production data.
