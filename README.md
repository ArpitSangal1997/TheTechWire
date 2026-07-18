# TheTechWire

TheTechWire is a full-stack publishing platform for writers, readers, and admins. It includes a public blog feed, author workspace, threaded comments, sharing, notifications, sponsorship placements, and a GK news experience powered by the News API.

## Project structure

```text
blogapp/
├── backend/   Spring Boot 3.3, Java 17, Spring Security, Spring Data JPA, PostgreSQL
└── frontend/  Angular 17 standalone app
```

## Local setup

### Backend

Create a PostgreSQL database named `wireblog` and configure the local connection in [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml).

```powershell
cd backend
$env:NEWS_API_KEY="your_key_here"
mvn spring-boot:run
```

The backend runs at http://localhost:8080.

### Frontend

```bash
cd frontend
npm install
npm start
```

The frontend runs at http://localhost:4200 and calls the backend through `/api`.

## Core capabilities

- Authentication and account flows: register, login, email verification, password reset.
- Posts: create, edit, publish, archive, delete, search, tag-filter, pagination, and author-specific lists.
- Comments: add threaded comments and delete your own comments.
- Sharing: share posts in-app, on social channels, by email, or with a copy link.
- News: fetch and cache GK headlines by category.
- Notifications: unread counts, read-all action, and share/reply notifications.
- Admin: sponsorship management and moderation-oriented admin endpoints.

## API flow overview

### Auth
- POST `/api/auth/register`
- POST `/api/auth/login`
- POST `/api/auth/verify-email?token=...`
- POST `/api/auth/password-reset/request`
- POST `/api/auth/password-reset/confirm`

### Posts
- GET `/api/posts`
- GET `/api/posts/mine`
- GET `/api/posts/{slug}`
- POST `/api/posts`
- PUT `/api/posts/{id}`
- POST `/api/posts/{id}/publish`
- POST `/api/posts/{id}/archive`
- DELETE `/api/posts/{id}`

### Comments
- GET `/api/comments/post/{postId}`
- POST `/api/comments/post/{postId}`
- DELETE `/api/comments/{commentId}`

### Shares
- POST `/api/posts/{postId}/share`

### News
- GET `/api/news/headlines`
- GET `/api/news/headlines?category=technology`

### Notifications
- GET `/api/notifications`
- GET `/api/notifications/unread-count`
- PATCH `/api/notifications/read-all`

### Uploads
- POST `/api/upload`

### Admin
- GET `/api/admin/sponsorships`
- POST `/api/admin/sponsorships`
- PUT `/api/admin/sponsorships/{id}`
- DELETE `/api/admin/sponsorships/{id}`
- GET `/api/sponsorships/active?placement=...`
- POST `/api/sponsorships/{id}/click`

## Configuration

| Variable | Purpose |
|---|---|
| `NEWS_API_KEY` | News API key for GK headlines. |
| `JWT_SECRET` | Production JWT signing secret. |
| `FRONTEND_URL` | Base URL for verification and reset links. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL settings for the `prod` profile. |

## Admin setup

Register through the UI, then promote the user in PostgreSQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```

Log out and back in to access the admin dashboard.

## Verification

- `mvn test`: passes.
- `npm run build`: passes with the existing Quill-related warning.

## Next step for deployment

The application is ready for feature work and local development. Deployment-specific items such as Docker, reverse proxy, HTTPS, CI, and secrets management can be added later without changing the existing app flow.
