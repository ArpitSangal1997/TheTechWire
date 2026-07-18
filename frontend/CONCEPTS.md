# Frontend Concepts for Wireblog

1. Angular Standalone App Architecture
   - This frontend uses modern Angular with standalone components instead of a single `NgModule`.
   - Each component declares its own imports and providers, which makes the app easier to test and reason about.
   - `AppComponent` is the root component that bootstraps the application and renders the page layout.
   - `app.config.ts` provides global app services such as router support, HTTP support, and browser animations.
   - In simple terms, standalone components mean each component is more self-contained and the app has less centralized boilerplate.

2. Angular CLI and Project Configuration
   - `package.json` lists dependencies, dev dependencies, and scripts used to build, serve, and test the app.
   - `npm start` typically runs the development server, while `npm build` produces optimized files for deployment.
   - `angular.json` is the Angular CLI configuration file that controls build and project settings.
   - It describes the application entry point, styles, assets, and output directory.
   - This file also configures environment file replacements for development and production builds.

3. Routing and Lazy Loading
   - `app.routes.ts` defines the URL routes that map to components in the app.
   - Example routes include home (`/`), post detail (`/post/:slug`), login (`/login`), and admin dashboard (`/admin`).
   - `loadComponent()` loads a component only when the user navigates to its route.
   - This is lazy loading, which reduces the initial bundle size and speeds up first load.
   - Guards like `authGuard` and `adminGuard` protect pages that require login or admin access.

4. Dependency Injection and Services
   - Angular's dependency injection system gives the app shared services that are created once and reused.
   - `@Injectable({ providedIn: 'root' })` makes a service available globally in the app.
   - `AuthService`, `PostService`, `UploadService`, and other services contain data access and business logic.
   - Services keep the UI components thin by moving network calls and state management out of the template.
   - Example: `PostService` handles all `/posts` calls so components do not need to know endpoint URLs.

5. Reactive State with Signals
   - Signals are a reactive state primitive in Angular that automatically update views when the data changes.
   - `signal()` creates a value container that components can read and react to.
   - `computed()` derives values from signals without manual subscriptions.
   - `AuthService` uses signals like `currentUserSignal` and `isLoggedIn` to keep auth state synchronized across the app.
   - Example: when a user logs in, the navbar updates automatically because the signals changed.

6. HTTP Client and Interceptors
   - `provideHttpClient(withInterceptors([authInterceptor]))` configures Angular's HTTP client and adds request interception.
   - `HttpClient` is the service used for sending GET, POST, PUT, and DELETE requests to the backend.
   - Interceptors can modify requests or responses globally.
   - `authInterceptor` adds the JWT token to the `Authorization` header for authenticated requests.
   - Example: the app does not need to add the token manually in each service method.

7. Authentication Flow
   - The app authenticates users by sending login or register requests to the backend.
   - `AuthService` receives a JWT token and user information from the backend.
   - The token is stored in `localStorage` so the user stays signed in after refresh.
   - `AuthService` exposes methods like `isLoggedIn()` and `isAdmin()` to the rest of the app.
   - Example: after successful login, the app redirects the user to the home page and updates the navbar.

8. Route Guards
   - Guards are functions that run before a route activates.
   - `authGuard` checks if the user is logged in and blocks access to protected routes.
   - `adminGuard` checks if the current user has the `ADMIN` role and blocks non-admin users.
   - If the guard returns `false`, the router redirects the user to another page.
   - Example: `/admin` is blocked for non-admin users and sends them back to home or login.

9. Component-Based UI Structure
   - The app is organized into feature components and shared components.
   - Feature components live under `src/app/features` and each handle one page or user task.
   - Shared components live under `src/app/shared/components` and provide UI pieces used across pages.
   - Each component has its own template, styles, and logic.
   - Example: `NavbarComponent` handles the top navigation, while `PostEditorComponent` handles creating and editing posts.

10. Forms and Template Binding
    - `FormsModule` enables template-driven forms in Angular.
    - Two-way binding with `[(ngModel)]` keeps the form input value synchronized with component properties.
    - Form validation and submission happen in the component class.
    - Example: email and password fields in the login form update component variables automatically.
    - When the user submits, the component creates a request object and sends it via a service.

11. File Upload Support
    - `UploadService` sends file data to the backend using `FormData`.
    - The file input element allows the user to select an image from their device.
    - The frontend posts the image to the `/upload` endpoint and receives an upload URL.
    - The post editor stores that returned URL as the post image location.
    - This separates file upload concerns from regular post data submission.

12. Rich Text Editing with Quill
    - The post editor uses Quill, a rich text editor library, for composing formatted content.
    - `@ViewChild('editor')` gets a reference to the DOM element where Quill will mount.
    - `ngAfterViewInit()` initializes the Quill editor after the component view is ready.
    - The editor emits content changes that update the component's `content` property.
    - Example: bold text, lists, links, and images are entered visually and saved as HTML content.

13. Route Parameters and Navigation
    - `ActivatedRoute` reads dynamic values from the URL.
    - Route paths like `/post/:slug` expose a `slug` parameter to the component.
    - The component uses that slug to load the correct blog post from the backend.
    - `Router` can also navigate programmatically after actions such as login or form submit.
    - Example: after saving a new post, the app redirects to that post's detail page.

14. Backend API Integration
    - The frontend communicates with the backend using REST APIs.
    - `environment.ts` defines the base API URL used during development.
    - Services construct endpoints like `${environment.apiUrl}/posts` and `${environment.apiUrl}/auth/login`.
    - The app sends JSON requests and receives JSON responses.
    - Example: `PostService.list()` fetches a list of posts from the backend and returns typed results.

15. Typed Models and Interfaces
    - TypeScript interfaces describe the shape of data objects used in the app.
    - The `core/models` folder defines interfaces like `PostSummary`, `PostDetail`, `PostRequest`, and `Page<T>`.
    - These types help catch errors at compile time and make code easier to understand.
    - Example: the editor uses `PostRequest` when sending a new post to the backend.
    - If the backend response does not match the expected model, TypeScript alerts the developer.

16. UI Loading and Error States
    - Components use state variables such as `loading`, `saving`, `uploading`, and `error`.
    - This gives the user feedback during asynchronous operations.
    - When data is loading, the UI can show a spinner or disable interactive elements.
    - When an error occurs, the UI shows a friendly message instead of failing silently.
    - Example: the post list component can display `Loading posts...` while it waits for the backend.

17. Standalone and Reusable UI Components
    - Shared components can be used across multiple pages for consistent appearance.
    - `NavbarComponent` and `TickerComponent` are examples of reusable UI pieces.
    - Standalone components include their own imports, so each one is self-contained.
    - This reduces coupling and makes it easier to reuse a component in another feature.
    - Example: the same `NavbarComponent` works on both home and admin pages.

18. Environment Configuration
    - The app uses `environment.ts` for development settings and `environment.prod.ts` for production settings.
    - Angular replaces the environment file during the build based on the selected configuration.
    - This means the same code can use a different backend API URL in production.
    - Example: `environment.apiUrl` points to `http://localhost:8080/api` in development.
    - In production, it may point to the deployed server address.

19. Modern Angular Patterns Used Here
    - `inject()` is used in guards and interceptors to get services without classes or constructors.
    - `standalone: true` components are the modern Angular style and remove the need for feature modules.
    - `loadComponent()` is the route-based lazy loading mechanism that improves startup performance.
    - Signals and computed state are the reactive state model used in this app.
    - These modern patterns keep the code concise and aligned with current Angular best practices.

20. Error Handling and User Feedback
    - Components handle HTTP errors and convert them into user-friendly messages.
    - Services or components catch backend errors and set an `error` message for the UI.
    - Friendly errors prevent confusing blank screens and help users understand what went wrong.
    - Example: if image upload fails, the editor shows a clear `Image upload failed.` message.
    - Good error handling makes the app easier to use and debug.

## HLD, LLD, and System Design for the Frontend

### What is HLD (High-Level Design)?
- HLD shows the main pieces of the frontend and how they fit together.
- It describes the page flow, routing, and backend integration without code-level detail.
- For this app, HLD explains the UI, navigation, authentication, and service boundaries.

### Frontend HLD for this project
- Browser UI: Angular standalone components rendered in the browser.
- Router: maps URLs to components and protects routes with guards.
- Services: shared logic for HTTP calls, auth state, uploads, posts, and notifications.
- Backend API: REST endpoints called through `HttpClient`.
- State: signals keep UI reactive and reflect login status, loading, and data.

### HLD diagram
```
Browser / Angular app
        |
        | User navigation and HTTP calls
        v
  App routes and components
    +-------------------------------+
    | 1. AppComponent              |
    | 2. Feature components        |
    |    home, login, post editor  |
    | 3. Shared components         |
    |    navbar, notifications     |
    | 4. Guards & interceptor      |
    +-------------------------------+
        |          |
        |          +--> Auth state via signals
        v
  HTTP Client -> Backend API (/api/...)
```

### What is LLD (Low-Level Design)?
- LLD describes the actual Angular files, components, services, and their responsibilities.
- It explains what each class or function does and how data flows through the app.
- In this app, LLD covers routing, guards, interceptors, services, and component interactions.

### Frontend LLD for this project
- `src/app/app.routes.ts`
    - Defines app routes and lazy loads feature components.
    - Uses `authGuard` and `adminGuard` to protect private pages.
- `src/app/app.config.ts`
    - Configures global providers like HTTP client, router, and animations.
    - Ensures services are available throughout the app.
- `src/app/core/services/auth.service.ts`
    - Manages login, logout, registration, and token storage.
    - Stores current user state in signals and exposes helpers like `isLoggedIn()`.
- `src/app/core/interceptors/auth.interceptor.ts`
    - Adds JWT tokens to outgoing requests automatically.
    - Ensures only authenticated requests include `Authorization`.
- `src/app/core/guards/auth.guard.ts` and `admin.guard.ts`
    - Decide whether navigation can proceed based on auth state and user role.
    - Redirect unauthorized users to login or home.
- `src/app/features/*.component.ts`
    - Each feature component handles a specific page or UI section.
    - They use services to load data, submit forms, upload images, and navigate.
- `src/app/core/services/post.service.ts`
    - Encapsulates API calls for posts, comments, and content loading.
    - Converts typed responses into models used by components.
- `src/app/core/services/upload.service.ts`
    - Sends image files to the backend upload endpoint.
    - Returns the uploaded image URL for post editor use.

### What is System Design for this frontend?
- System design explains how the frontend serves the whole product and supports the user experience.
- It includes navigation flow, API dependency, auth lifecycle, and deployment behavior.

### System design considerations for this frontend
- User interface and navigation
    - Public pages: home, news feed, post detail, login, register.
    - Protected pages: my stories, post editor, admin dashboard.
    - The router controls which component renders and when guards run.
- Authentication lifecycle
    - Logs in by sending credentials to the backend.
    - Stores token in `localStorage` and maintains auth status with signals.
    - Automatically includes auth token on every backend API request.
- Data loading and caching
    - Services load post lists, single post data, and user-specific content.
    - Components show loading state until HTTP responses arrive.
    - Signals and component-level state keep the data reactive.
- Error handling and user feedback
    - API failures are caught in services or components.
    - Components display error messages and prevent broken UI.
    - This keeps users informed and the app more robust.
- Deployment and environments
    - Uses `environment.ts` for development API URLs.
    - Uses `environment.prod.ts` for production build settings.
    - The app can be deployed as static files to any web host.

### Rebuild confidence
- If you understand the frontend HLD, you know how the app pages and services are organized.
- If you understand the frontend LLD, you know which Angular files and classes to build.
- If you understand the frontend system design, you know how the UI behaves in real use.
- This file now contains the conceptual map needed to recreate the Angular frontend step by step.
