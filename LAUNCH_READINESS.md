# TheTechWire launch-readiness review

**Review date:** 2026-09-28  
**Scope:** Local Angular frontend and Spring Boot API; browser flows and API operations using two disposable QA accounts and temporary records. Those accounts and all QA content were removed and cleanup counts verified.  
**Decision:** **Not ready for an unrestricted public launch yet.** Core author, reader, and admin flows now have manual end-to-end coverage, but there are still no automated tests, and several account, operational, and abuse-resistance gaps should be addressed before real users entrust the service with accounts or content.

## Regression check results

| Area | Result | Evidence / limitation |
|---|---|---|
| Frontend production build | **Pass with warning** | `npm run build` succeeded. Angular warned that `quill` is CommonJS, which can limit optimization. |
| Backend Maven test lifecycle | **Build pass; tests absent** | `mvn clean test` compiled all 80 backend sources and succeeded, but explicitly reported “No tests to run.” This is not evidence that behavior is regression-tested. |
| Frontend dev server | **Pass** | Angular dev server started at `http://localhost:4200`. |
| Home/feed | **Renders; data empty** | The page showed the designed “No stories yet” empty state. `GET /api/posts?page=0&size=3` returned 200 with zero posts. |
| Story Trails | **Renders; data empty** | `/trails` showed the empty state. `GET /api/trails?page=0&size=3` returned 200 with zero trails. |
| GK/news | **Pass for public read/filter** | `/gk` rendered headlines. Clicking Technology replaced the results with technology headlines; the public news endpoint returned 200. |
| Authentication | **Manual signup/login pass; policy gap remains** | Registered two disposable users in the browser, received AUTHOR sessions, and logged in successfully. The user was still `emailVerified=false`; login is currently allowed. Temporary accounts were removed. Email delivery is not implemented. |
| Authenticated routes | **Anonymous guard pass** | Direct anonymous visits to `/write`, `/my-stories`, `/account`, and `/admin` redirected to `/login`. |
| Public read APIs | **Pass for representative resources** | Posts, Story Trails, News, and active sponsorship reads returned 200. |
| Protected APIs | **Anonymous rejection pass** | `/api/users/me`, notifications, admin users, comment creation, and upload returned 403 without a token. |
| Author publishing | **Pass** | Through UI: create/publish a tagged article with a Story Trail, open the public detail page, find it in My Stories, edit/publish it, archive it (public detail became 404), then republish it (public detail returned 200). |
| Comments/replies | **Pass after fix** | Authenticated comment and reply create/read passed; comment flag/unflag/list passed after transaction-boundary fix. Notifications for replies were created. |
| Sharing/notifications | **Pass** | Copy-link and in-app share returned 200; share notification appeared for recipient and reply notification for author. |
| Reader Pulse | **Pass, basic vote path** | A vote for a published QA post returned 200 and total votes became 1. Duplicate-vote resistance is not enforced server-side. |
| Admin users and authorization | **Pass after fix** | Admin dashboard rendered users; admin list/posts endpoints returned 200. Self-suspend/self-role-change returned 400. Promotion/demotion reflected immediately on an existing token after the JWT filter fix. Suspension invalidated the old token and new login returned 403. |
| Admin comment moderation | **Pass after fix** | Before fix, flag and flagged-list endpoints returned 500 from detached lazy JPA data. Added transactions; flag/list/unflag then returned 200. |
| Sponsorship management/click tracking | **Pass** | Created an active sidebar placement in admin UI; it appeared publicly, anonymous click tracking returned 200 and incremented clicks, and the deal was deleted through the UI. |
| Admin post moderation UI | **Gap found** | Backend admin post listing/actions exist and listing returned 200, but `admin-dashboard.component.html` does not render the `posts` model or the publish/archive/delete handlers. Admins cannot use those post moderation actions from the current dashboard. |
| Article detail/Story Trail detail | **Pass for a real QA story** | Published story and generated Trail loaded. Direct non-existent detail routes now load the Angular shell after the base-href fix; missing post API returns 404 as expected. |
| Profile/password reset | **Not fully tested** | Profile update and successful reset were not exercised. Reset emails are only logged by the development service. |
| Upload success path | **Not tested** | Anonymous upload was correctly rejected with 403. A valid upload would create a file, so the positive path was not run. |
| Actuator health | **Blocked for unauthenticated probe** | `GET /actuator/health` returned 403. Configure a narrowly scoped health/readiness probe rather than exposing all management endpoints. |
| Deep links / refresh | **Bug found and fixed** | Before the fix, directly opening nested paths resolved JS/CSS under `/post/` or `/trails/` and rendered blank. Added `<base href="/">` to `frontend/src/index.html`; production output contains the base and the restarted dev server reports the root base on nested routes. |

Manual flows used unique `example.test` email addresses and temporary records. QA users, post, comments, replies, share, Pulse vote, sponsorship, Story Trail, notifications, and browser token were cleaned up; database counts for QA users/posts/trails/sponsorships/comments/Pulses/shares/notifications were zero afterward. These manual checks do not replace repeatable automated tests. Successful upload, profile update, password reset, admin post actions, and concurrency/error paths still need coverage.

## Fixes made during this review

- Added `<base href="/">` to `frontend/src/index.html` after direct nested-route loads were found to resolve assets under the route and render blank pages.
- Added transactions to admin flagged-comment listing and flag updates after both returned HTTP 500 for lazy-loaded entity data.
- Changed generic API 500 handling to log exceptions server-side and return `An unexpected error occurred.` instead of exposing Hibernate messages.
- Changed `JwtAuthFilter` to load current role/enabled state from the database, and hardened `CurrentUserResolver`. Before this, an existing JWT remained usable after suspension and carried stale role claims; after the fix, promotion/demotion and suspension affected already-issued tokens immediately.

## Launch blockers (prioritize before opening registration broadly)

### P0 — Account and security trust

1. **Replace development email logging.** `backend/src/main/java/com/wireblog/service/EmailService.java` writes verification and password-reset URLs with raw tokens into logs; it does not send email. Implement a real email provider, prevent token disclosure in logs, and test delivery/failure handling. Add expiry for verification tokens (password-reset tokens currently have a 30-minute expiry).
2. **Set and enforce production secrets.** Never deploy the checked-in development JWT fallback or the local database credential. Require strong per-environment secrets, rotate any credential that has been exposed, and confirm the `prod` profile is used. Do not commit real secrets.
3. **Review authentication policy and session lifecycle.** `AuthService.login` does not require `emailVerified`, and JWTs are stored in browser `localStorage`. Decide and implement the verification policy; define invalid/expired token UX and evaluate HttpOnly secure cookies or another XSS-aware session approach.
4. **Prevent vote manipulation.** Reader Pulse votes are anonymous and each POST is inserted as a new vote; client-side `sessionStorage` is not an enforcement boundary. Add per-account or privacy-preserving vote deduplication and rate limits, or clearly treat totals as informal feedback.
5. **Configure trusted proxy/rate limiting.** `RateLimitFilter` is in-memory (per instance) and takes the first `X-Forwarded-For` value without a trusted-proxy boundary. Configure proxy handling and use shared rate limits before multiple instances or public abuse exposure.

### P1 — Data durability, regression safety, and operations

1. **Introduce versioned DB migrations.** The default profile combines Hibernate `ddl-auto: update` with `schema.sql`; production validates but has no versioned migration runner. Add Flyway/Liquibase and test upgrades from a copy of existing data.
2. **Build a repeatable test suite.** Add backend unit/service/security/API tests and PostgreSQL integration/migration tests; add Angular service/component tests and Playwright/Cypress end-to-end flows. Run them in CI on each change.
3. **Make health/readiness usable safely.** The current health URL returns 403 anonymously. Expose only necessary liveness/readiness probes on a protected/private management interface or a narrowly permitted endpoint.
4. **Use durable media storage.** `UploadController` writes to local disk. Local ephemeral storage is not durable across deployments or shared across replicas. Move media to object storage; add quotas, cleanup, backup/retention, and CDN/public URL support.
5. **Harden deployment configuration.** Move CORS origins and frontend/upload URLs to environment-specific settings; keep TLS, database backups, restore drills, monitoring, alerting, and deployment rollback procedures documented and tested.
6. **Validate limits and persistence rules.** Cap requested page size, test concurrent handle/email registration constraints, and make sure all destructive admin operations have auditable behavior.
7. **Complete the moderation UI and API input handling.** Add the missing admin post moderation table/actions, and return a clear 400 for invalid/negative page parameters instead of the observed generic 500 response.

## Product improvements with user value

### Before launch / first beta

- **Editorial trust and safety:** publish clear community rules, privacy policy, terms, copyright/contact process, and visible report/moderation status. Signup makes users authors, so user-generated content needs moderation/abuse processes from day one.
- **Reliable onboarding:** email confirmation, resend verification, useful password-reset confirmation, friendly error/retry states, and clear author first-post onboarding.
- **Share-ready articles:** server-rendered or otherwise crawlable article metadata (title, summary, canonical URL, Open Graph image) for SEO and social previews. The current Angular setup is an SPA and does not configure SSR.
- **Accessible core journeys:** keyboard-only navigation, visible focus states, semantic labels, contrast, reduced-motion behavior, and screen-reader checks for feed, editor, dialogs, and error states.

### High-value roadmap after a safe beta

1. **Follow Story Trails and author topics** with notification/email digests; this gives readers a reason to return as a story evolves.
2. **Bookmarks / reading list** and “continue reading” for repeat reader retention.
3. **Author analytics** for views, reading engagement, shares, and trail referrals, with clear privacy controls.
4. **Newsletter subscriptions and digest preferences** with double opt-in and unsubscribe management.
5. **Better discovery**: search beyond title (currently title matching), filters/sorting, related stories, and editorially curated collections.
6. **Autosave and revision history** in the rich-text editor to protect creator work and make publishing safer.
7. **Transparent sponsorship reporting**: labeled sponsored placements, reliable impression/click definitions, fraud controls, and admin reporting.
8. **Reader feedback/reporting tools** with moderation queues and status notifications.

## Suggested launch gates

- [ ] Production secrets and database credentials are externalized and rotated.
- [ ] Real email delivery works; raw tokens never appear in logs; verification/reset expiry and account policy are tested.
- [ ] Migrations have been tested on a restored copy of production-like data; backup restore is proven.
- [ ] Automated tests cover public reading and all critical authenticated/admin workflows.
- [ ] Security review covers authorization ownership, XSS/content sanitization, token storage, CORS, proxy/rate limiting, uploads, and error responses.
- [ ] Monitoring, narrowly scoped health/readiness, incident response, and rollback are in place.
- [ ] Legal/community policies, reporting/moderation staffing, contact details, and accessibility checks are ready.
- [ ] Seed/demo content and first-run empty states are reviewed; analytics and user feedback channels are set up.

A **small invite-only beta** can be considered after the P0 items are closed and critical workflows are manually verified with disposable test data. Do not treat green builds or anonymous API smoke checks as launch approval.
