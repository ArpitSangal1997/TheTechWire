# Backend Concepts for Wireblog

1. Spring Boot Application Setup
   - Spring Boot is a Java framework that makes it faster to build web applications.
   - The main class is `WireBlogApplication`. It has a `main` method that calls `SpringApplication.run(...)`.
   - `@SpringBootApplication` means this class is the starting point for Spring. It combines three things:
     - `@Configuration`: this class can define beans and configuration.
     - `@EnableAutoConfiguration`: Spring will automatically configure common parts like web servers and databases.
     - `@ComponentScan`: Spring will look for other components in the same package.
   - `@EnableScheduling` allows the app to run scheduled tasks, like refreshing news every 15 minutes.
   - `@EnableCaching` turns on caching support, which can store results and make repeated operations faster.
   - Example: When the app starts, Spring creates all controllers, services, and other beans automatically.

2. Maven Build and Dependencies
   - `pom.xml` is the configuration file for Maven, the build tool.
   - It lists dependencies, which are external libraries the app needs.
   - Important dependencies here:
     - `spring-boot-starter-web`: lets the app handle HTTP requests and create REST APIs.
     - `spring-boot-starter-data-jpa`: provides database access through JPA.
     - `spring-boot-starter-security`: adds security features like login and role-based access.
     - `spring-boot-starter-validation`: validates user input automatically.
     - `spring-boot-starter-cache`: enables caching support.
     - `spring-boot-starter-actuator`: exposes monitoring and health endpoints.
     - `jjwt`: generates and validates JWT tokens.
     - `h2` and `postgresql`: database drivers for local development and production.
   - `java.version` is set to 17, so the project uses Java 17 features.
   - Lombok is used to reduce boilerplate code. For example, `@Data` adds getters and setters automatically.

3. Configuration and Profiles
   - `application.yml` stores app settings in a human-readable format.
   - It contains database settings, server port, JWT settings, upload settings, and news API settings.
   - Example: `spring.datasource.url` tells the app where the database is.
   - Profiles let the app use different settings for development and production.
   - In the `prod` section, sensitive values are read from environment variables like `DB_URL` and `JWT_SECRET`.
   - `@Value("${app.jwt.secret}")` in Java code injects the configured JWT secret.
   - Think of profiles as different toolboxes for different environments.

4. Controllers and REST Endpoints
   - Controllers handle incoming HTTP requests and return responses.
   - `@RestController` marks a class as a REST API controller.
   - `@RequestMapping("/api/auth")` sets the base URL for that controller.
   - `@GetMapping`, `@PostMapping`, `@PutMapping`, and `@DeleteMapping` define specific endpoints.
   - Example: `@PostMapping("/login")` handles `POST /api/auth/login`.
   - `@RequestBody` converts JSON request bodies into Java objects.
   - `@Valid` checks that incoming data follows validation rules.
   - `@PathVariable` and `@RequestParam` read values from the URL and query string.
   - Example: `@GetMapping("/posts/{slug}")` uses `@PathVariable String slug` to get the post slug from the URL.

5. Data Transfer Objects (DTOs)
   - DTOs are simple objects used for API input and output.
   - They separate the data the client sends and receives from internal database objects.
   - The `dto` package contains classes like `RegisterRequest`, `LoginRequest`, `PostRequest`, `PostDetailResponse`, and `AuthResponse`.
   - Example: `RegisterRequest` contains `email`, `password`, and `displayName`, while `AuthResponse` contains the JWT token and user info.
   - This makes the API easier to change later without changing the database model.

6. JPA Entities and Database Mapping
   - Entities are Java classes that map to database tables.
   - `@Entity` means this class is stored in the database.
   - `@Table(name = "users")` specifies the table name.
   - `@Id` identifies the primary key.
   - `@GeneratedValue(strategy = GenerationType.IDENTITY)` uses the database to generate unique IDs.
   - `@Column(nullable = false, unique = true)` defines rules for a database column.
   - `@Enumerated(EnumType.STRING)` stores enum values as text instead of numbers.
   - `@PrePersist` runs code before the entity is saved for the first time.
   - Example: the `User` entity sets `createdAt` automatically before insertion.

7. Repositories and Data Access
   - Repositories are interfaces that handle database queries.
   - Each repository extends `JpaRepository<Entity, IdType>`.
   - This gives common operations like `save()`, `findById()`, and `deleteById()` for free.
   - Example: `PostRepository extends JpaRepository<Post, Long>`.
   - You can define custom methods by naming them carefully, such as `findTop50ByOrderByPublishedAtDesc()`.
   - Think of repositories as helpers that hide low-level SQL details.

8. Service Layer and Business Logic
   - Services contain the core logic of the application.
   - `@Service` tells Spring to manage the class as a bean.
   - Controllers call services to perform work, keeping controllers simple.
   - `@Transactional` means a group of database operations are treated as one unit.
   - If one operation fails, the whole transaction is rolled back.
   - `@Transactional(readOnly = true)` is used for methods that only read data.
   - Example: `PostService` handles creating, updating, publishing, and deleting posts.

9. Spring Security Basics
   - Spring Security protects routes and checks permissions.
   - `SecurityConfig` defines which URLs are public and which need login.
   - `SessionCreationPolicy.STATELESS` means the server does not store login sessions.
   - Each request must carry a valid JWT token.
   - Example: `/api/admin/**` is only accessible to users with the `ADMIN` role.
   - Public endpoints like `/api/auth/**` and some read-only post APIs are left open.

10. JWT Token Handling
    - JWT stands for JSON Web Token. It is a compact token that carries user identity.
    - `JwtUtil` creates tokens using a secret key.
    - Tokens include claims such as `sub` (email), `uid` (user ID), and `role`.
    - Example: after login, the backend returns a token that the frontend stores.
    - `JwtAuthFilter` reads this token on each request and validates it.
    - If the token is valid, the user is authenticated for that request.

11. Filters, Security Chain, and CORS
    - Filters run before controllers and can modify or reject requests.
    - `JwtAuthFilter` is a filter that checks the token and adds user details to the security context.
    - `SecurityFilterChain` is the sequence of filters used for each request.
    - CORS allows the frontend app to call the backend from another origin.
    - `CorsConfigurationSource` defines allowed origins, methods, and headers.
    - Example: `http://localhost:4200` is allowed because the frontend runs there in development.

12. Exception Handling
    - `@RestControllerAdvice` catches exceptions thrown by controllers.
    - `@ExceptionHandler(ApiException.class)` handles custom business errors.
    - `MethodArgumentNotValidException` is thrown when validation fails.
    - The handler returns a consistent JSON error structure.
    - Example: if a request omits a required field, the response contains a helpful message explaining the issue.

13. File Upload and Static File Serving
    - `UploadController` accepts `MultipartFile` uploads from clients.
    - This is the standard way to upload files in Spring.
    - `StaticResourceConfig` maps URL paths like `/uploads/**` to a folder on disk.
    - Example: after uploading an image, the client can access it at `http://localhost:8080/uploads/image.jpg`.
    - The upload path is configurable through `app.upload.dir`.

14. Scheduled Tasks and External API Integration
    - `@Scheduled` allows methods to run periodically.
    - `NewsService` refreshes headlines every 15 minutes.
    - It uses `RestTemplate` to fetch JSON from an external news API.
    - `ObjectMapper` parses JSON into usable data.
    - `@EventListener(ApplicationReadyEvent.class)` refreshes headlines once when the app starts.
    - Example: the app caches news so that it can still show content even if the external provider is disrupted.

15. Validation with Jakarta Bean Validation
    - Validation annotations ensure incoming data is correct.
    - Common annotations include `@NotBlank`, `@Email`, and `@Size`.
    - `@Valid` makes Spring check these rules automatically.
    - Example: `@NotBlank private String email;` means the email cannot be empty.
    - If validation fails, the application returns a `400 Bad Request` error with a clear message.

16. Profiles, Environment Variables, and Production Safety
    - Profiles allow different settings for development and production.
    - In production, secrets like database credentials and JWT keys come from environment variables.
    - This keeps sensitive data out of the source code.
    - Example: `JWT_SECRET` is not hard-coded; it is loaded from the environment.
    - If the value changes in production, old tokens become invalid.

17. H2 Support and Database Initialization
    - H2 is an in-memory database useful for local development.
    - When using H2, the app can start without installing PostgreSQL.
    - `spring.jpa.hibernate.ddl-auto: update` makes Hibernate update the schema automatically during development.
    - In production, `ddl-auto: validate` checks the schema without changing it.
    - `h2.console.enabled: true` opens a browser-based database console for debugging.

18. Monitoring and Actuator
    - `spring-boot-starter-actuator` adds endpoints that report app health and metrics.
    - These endpoints are useful for operations and debugging.
    - Example endpoints include `/actuator/health` and `/actuator/info`.
    - This gives insight into whether the app is running correctly.

19. Important Annotations and What They Mean
   - `@SpringBootApplication`: starts the Spring Boot app.
   - `@RestController`: marks a class as a REST API controller.
   - `@Service`: marks a class as a service layer component.
   - `@Repository`: marks a data access component.
   - `@Entity`: maps a class to a database table.
   - `@Configuration`: marks a class that defines beans.
   - `@Bean`: creates a bean from a method.
   - `@Value`: injects configuration values from `application.yml` or environment variables.
   - `@Component`: marks a generic Spring component.
   - `@Transactional`: manages database transaction boundaries.

20. Overall Backend Request Flow
   - A client sends an HTTP request to the backend.
   - Security filters run first and check the JWT token.
   - If the request is allowed, the controller method executes.
   - The controller delegates business work to a service.
   - Services use repositories to read and write the database.
   - The controller returns JSON data to the client.
   - If anything fails, exception handlers turn the error into a clear response.
## Practical Spring Boot Backend Guide

This section explains how the backend is built from the ground up. It is written as if you are learning Spring Boot for the first time.

### 1. What is Spring Boot?
- Spring Boot is a framework that makes it easy to build Java web applications.
- It provides a ready-made application setup and hides much of the low-level wiring.
- Think of it as a toolkit: instead of writing boilerplate code for servers, configuration, and dependency loading, Spring Boot does it for you.
- In this backend, Spring Boot is responsible for running the web server, loading the controllers, and connecting the database.

### 2. Project structure and packages
- The backend follows a common Spring structure under `src/main/java/com/wireblog`.
- Package meanings:
  - `controller`: handles incoming HTTP requests.
  - `service`: contains business logic and orchestrates work.
  - `repository`: talks to the database.
  - `model`: defines database entities.
  - `dto`: defines request and response formats.
  - `config`: contains configuration classes such as security and static file handling.
  - `exception`: contains error handling logic.
- This structure helps keep responsibilities separated.

### 3. Building the project with Maven
- `pom.xml` is the Maven project file.
- Key sections:
  - `<parent>` imports Spring Boot default settings.
  - `<dependencies>` lists the libraries used by the application.
  - `<build>` defines how Maven packages the app.
- To create the app, you start with a Maven project and add the required starters such as `spring-boot-starter-web`.

### 4. Main application class
- `WireBlogApplication.java` is the entry point.
- It contains `public static void main(String[] args)`.
- It uses `SpringApplication.run(WireBlogApplication.class, args)` to start the app.
- Annotations in this class:
  - `@SpringBootApplication`: starts Spring Boot auto-configuration.
  - `@EnableScheduling`: enables scheduled tasks.
  - `@EnableCaching`: enables caching support.
- How to use it: run this class from your IDE or with `mvn spring-boot:run`.

### 5. Configuration with `application.yml`
- This YAML file stores settings such as database location, JWT secrets, and upload paths.
- Example:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wireblog
    username: postgres
    password: root
app:
  jwt:
    secret: "REPLACE_WITH_A_LONG_RANDOM_SECRET"
    expiration-ms: 604800000
```
- Why use it:
  - Keeps secrets and environment settings outside code.
  - Makes the app easier to configure for development and production.
- How to use it: Spring automatically reads this file at startup.

### 6. Profiles and environment variables
- `application.yml` contains default values.
- A `prod` profile section overrides those values when the app runs in production.
- Example: `app.jwt.secret` is loaded from `JWT_SECRET` in production.
- Why use profiles:
  - development can be easy and local
  - production can be secure and environment-specific
- How to use it: activate the profile with `-Dspring.profiles.active=prod` or an environment variable.

### 7. REST controllers
- Controllers define API endpoints.
- Example annotations:
  - `@RestController`: marks a class as a JSON API controller.
  - `@RequestMapping("/api/auth")`: sets the URL prefix.
  - `@PostMapping("/register")`: handles POST requests.
- Why use controllers:
  - They separate HTTP handling from business logic.
  - They keep request and response mapping clear.
- How to use them:
  - Write a controller method for each API endpoint.
  - Use `@RequestBody` to read JSON input.
  - Use `@PathVariable` and `@RequestParam` for URL data.

### 8. DTOs for requests and responses
- DTO stands for Data Transfer Object.
- These objects describe exactly what the API accepts and returns.
- Why use DTOs:
  - Keeps API data separate from internal database entities.
  - Prevents the client from seeing sensitive fields such as password hashes.
  - Makes API versions easier to manage.
- Example:
  - `LoginRequest` contains `email` and `password`.
  - `AuthResponse` contains `token` and user details.

### 9. Validation with Bean Validation
- Validation annotations enforce rules on incoming data.
- Common annotations:
  - `@NotBlank`: field must not be empty.
  - `@Email`: field must be a valid email.
  - `@Size`: field length constraints.
- How to use:
  - Add annotations to DTO fields.
  - Use `@Valid` in controller method parameters.
- Why use validation:
  - Protects the backend from bad or incomplete requests.
  - Provides friendly error messages to the client.

### 10. Entities and JPA mapping
- Entities are Java classes that map to database tables.
- Important annotations:
  - `@Entity`: tells JPA this class is a database table.
  - `@Table(name = "...")`: optional table name mapping.
  - `@Id`: marks the primary key.
  - `@GeneratedValue`: auto-generates IDs.
  - `@Column`: configures a table column.
  - `@Enumerated(EnumType.STRING)`: stores enums as readable text.
- Example: `User` entity stores user account data.
- Why use JPA:
  - It makes database access object-oriented.
  - It reduces manual SQL code.

### 11. Repositories and data access
- Repositories are interfaces that handle database operations.
- Example: `UserRepository extends JpaRepository<User, Long>`.
- `JpaRepository` provides methods like `save`, `findById`, and `deleteById`.
- Why use repositories:
  - They abstract database access.
  - They let you focus on business logic.
- How to use them:
  - Inject a repository into a service.
  - Call methods such as `save()` or custom queries.

### 12. Custom query methods
- Spring Data can create queries from method names.
- Example: `findTop50ByOrderByPublishedAtDesc()`.
- Why use custom methods:
  - You can fetch data without writing SQL.
  - The method name describes the query.
- How to use:
  - Declare the method in the repository interface.
  - Spring creates the implementation automatically.

### 13. Service layer and `@Service`
- Services contain business logic.
- `@Service` marks a class as a Spring component.
- Why use services:
  - Keeps controllers simple and thin.
  - Holds the rules for how data is created, updated, and deleted.
- Example: `PostService` handles creating, publishing, and deleting posts.

### 14. Transactions with `@Transactional`
- Transactions group several database operations together.
- If one operation fails, all changes are rolled back.
- Example: creating a post and saving tags should either fully succeed or fully fail.
- Why use transactions:
  - Prevents partial updates that leave the database inconsistent.
- How to use:
  - Add `@Transactional` to service methods.
  - Use `@Transactional(readOnly = true)` for methods that only read data.

### 15. Spring Security basics
- Spring Security protects backend routes.
- Key concepts:
  - authentication: checking who the user is
  - authorization: checking what the user can do
- `SecurityConfig` defines which URLs are public and which require login.
- Example: `/api/auth/**` is public, `/api/admin/**` requires admin role.
- Why use Spring Security:
  - It provides a strong, extensible way to protect a web API.

### 16. Stateless JWT authentication
- JWT is a token that proves a user is logged in.
- The backend issues the token after successful login.
- The token is sent with every request in the `Authorization` header.
- `JwtUtil` creates and validates the token.
- Why use JWT:
  - The server does not need to remember sessions.
  - It works well for APIs and single-page applications.

### 17. How JWT is used in this app
- Login and registration controllers return an `AuthResponse` with a JWT.
- The frontend stores that JWT and sends it on future requests.
- `JwtAuthFilter` reads the token from the request header.
- If the token is valid, Spring Security knows the user is authenticated.
- Example: the filter checks `Authorization: Bearer <token>`.

### 18. Security filters and `SecurityFilterChain`
- A filter is code that runs before controller methods.
- Security filters check CORS, authentication, and authorization.
- `SecurityFilterChain` sets the order of these filters.
- Example: `JwtAuthFilter` runs before the username/password filter.

### 19. CORS and frontend communication
- CORS allows the frontend app at a different domain to call the backend.
- Example: `http://localhost:4200` can call `http://localhost:8080`.
- `CorsConfigurationSource` defines allowed origins, methods, and headers.
- Without CORS, the browser would block requests coming from another origin.

### 20. Exception handling
- `GlobalExceptionHandler` catches exceptions from controllers.
- `@RestControllerAdvice` makes the class a global error handler.
- `@ExceptionHandler` maps specific exceptions to responses.
- Why use it:
  - It provides consistent error messages to the client.
  - It keeps controller code cleaner.
- Example: validation errors return a JSON payload with the message and status code.

### 21. File upload and serving static files
- `UploadController` receives a file from the frontend.
- The uploaded file is represented by `MultipartFile`.
- `StaticResourceConfig` maps the `/uploads/**` URL to files stored on disk.
- Example: upload an image, then view it from `http://localhost:8080/uploads/my-image.jpg`.
- Why use this:
  - Allows the app to store and serve images, files, and attachments.

### 22. Scheduling and external APIs
- `NewsService` fetches headlines from a third-party news provider.
- `@Scheduled` runs the fetch every 15 minutes.
- `RestTemplate` makes HTTP calls to the external service.
- `ObjectMapper` parses the JSON response.
- Why use scheduling:
  - Keeps the news content fresh without manual refresh.
  - Caches results in the database for faster reads.

### 23. Application startup initialization
- `@EventListener(ApplicationReadyEvent.class)` runs code after the app starts.
- Example: refresh the news cache immediately after startup.
- Why use it:
  - Ensures required background data is available before users begin using the app.

### 24. Monitoring with Spring Boot Actuator
- Actuator adds endpoints for health, metrics, and application info.
- Example: `/actuator/health` shows whether the app is running.
- Why use actuator:
  - Provides a quick way to monitor the backend's health.

### 25. How to run the backend locally
1. Open the backend directory in your IDE.
2. Make sure the database is available or use H2 for local testing.
3. Run `mvn spring-boot:run` or launch `WireBlogApplication` from the IDE.
4. The backend starts on port `8080` by default.
5. Test API endpoints such as `/api/auth/login` using Postman or a browser.

### 26. How the complete backend works together
- The frontend sends requests to the backend API.
- Spring Boot starts the web server and loads all beans.
- Security filters verify each request.
- Controllers receive requests and call services.
- Services use repositories to read/write the database.
- Exception handlers return clean error responses when needed.
- Scheduled tasks and file uploads work in the background.

### 27. Why each layer exists
- `controller`: communicates with the outside world.
- `service`: contains business rules.
- `repository`: talks to the database.
- `model`: defines the data structure.
- `dto`: defines the API surface.
- `config`: configures security, CORS, and static files.
- `exception`: handles errors consistently.

### 28. Practical advice for a beginner
- Start with the main application class and understand how Spring Boot starts.
- Read `application.yml` to see how configuration is loaded.
- Look at a controller and follow the flow into the service.
- See how the service uses a repository to access the database.
- Learn the security configuration and how JWT is validated.
- Use Postman or curl to call endpoints and inspect JSON responses.
- Make small changes and restart the app to experiment.

### 29. Common beginner concepts
- Dependency injection means objects are provided to each other automatically.
- Beans are Spring-managed objects.
- Annotations tell Spring what role a class or method plays.
- A REST API is a set of URLs that exchange JSON data.
- A database entity is a Java class that stores persistent data.

### 30. Next steps after learning
- Explore the frontend to see how it calls the backend.
- Add a new controller or service to practice.
- Change a DTO and see how validation works.
- Add a new repository query method.
- Try running the app in production mode using a real PostgreSQL database.
# Detailed Spring Boot Concepts

1. `@SpringBootApplication`
   - Marks the main class as the Spring Boot application entry point.
   - Combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`.
   - Example: Spring automatically discovers your controllers and services just because they are in the same package.

2. `@EnableScheduling`
   - Enables support for scheduled tasks.
   - Methods annotated with `@Scheduled` will run on a timer.
   - Example: `NewsService.refresh()` runs every 15 minutes to update headlines.

3. `@EnableCaching`
   - Turns on caching support in Spring.
   - Caching stores data temporarily to answer repeated requests faster.
   - Example: if the app needed to cache user profile data, it could avoid repeated database reads.

4. `pom.xml`
   - The project file for Maven, the Java build tool.
   - Defines dependencies, project metadata, and build settings.
   - When you run `mvn clean package`, Maven reads this file to build the app.

5. `spring-boot-starter-web`
   - Provides the web layer for Spring Boot.
   - Includes Spring MVC, JSON support, and a built-in web server.
   - Without it, the app would not accept web requests.

6. `spring-boot-starter-data-jpa`
   - Adds JPA-based database access.
   - Includes Hibernate, a library that maps Java objects to database tables.
   - It allows using repositories instead of writing SQL directly.

7. `spring-boot-starter-security`
   - Adds Spring Security to the app.
   - Provides authentication and authorization infrastructure.
   - Example: protect admin-only endpoints and validate JWT tokens.

8. `spring-boot-starter-validation`
   - Enables validation of incoming request data.
   - Works with annotations like `@NotBlank` and `@Email`.
   - Example: invalid registration input is rejected before the service runs.

9. `spring-boot-starter-cache`
   - Enables Spring’s caching support.
   - Useful for speeding repeated operations.
   - Example: caching external news results or expensive database calculations.

10. `spring-boot-starter-actuator`
    - Adds health and monitoring endpoints.
    - Useful for checking application status in production.
    - Example endpoints include `/actuator/health`.

11. `@Value`
    - Reads configuration from `application.yml` or environment variables.
    - Example: `@Value("${app.jwt.secret}")` injects the JWT secret into a bean.
    - This makes configuration flexible and environment-specific.

12. `@RestController`
    - Marks a class as a REST API controller.
    - Methods return JSON directly instead of rendering HTML.
    - Example: `AuthController` exposes login and registration endpoints.

13. `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, and `@DeleteMapping`
    - Define HTTP endpoints and the HTTP methods they accept.
    - Example: `@PostMapping("/api/auth/login")` means “handle POST requests to `/login`”.
    - These annotations bind URLs to Java methods.

14. `@RequestBody`
    - Converts JSON sent by the client into a Java object.
    - Example: the login JSON object is converted into a `LoginRequest`.
    - This is how controllers receive structured request data.

15. `@Valid`
    - Validates request objects based on annotations in the DTO.
    - Example: `@NotBlank private String email;` requires the email field to be non-empty.
    - If validation fails, Spring returns a `400 Bad Request` response.

16. `@PathVariable` and `@RequestParam`
    - `@PathVariable` extracts values from the URL path.
    - Example: `/api/posts/{id}` extracts `id` from the path.
    - `@RequestParam` reads query parameters like `?page=1`.

17. DTOs (Data Transfer Objects)
    - DTOs are used for communication between client and server.
    - They keep API input/output separate from database entities.
    - Example: `PostRequest` contains only fields needed to create or update a post.

18. `@Entity`
    - Marks a class as a database-backed entity.
    - Each entity instance maps to a database row.
    - Example: `User`, `Post`, and `Comment` are database entities.

19. `@Table`
    - Specifies the database table name for an entity.
    - Example: `@Table(name = "users")` maps the `User` entity to the `users` table.

20. `@Id` and `@GeneratedValue`
    - `@Id` marks the primary key of the entity.
    - `@GeneratedValue` means the database generates the key automatically.
    - This is how new records get unique IDs.

21. `@Column`
    - Configures column properties such as `nullable` and `unique`.
    - Example: `@Column(nullable = false, unique = true)` enforces these constraints in the database.

22. `@Enumerated(EnumType.STRING)`
    - Stores enum values in the database as strings.
    - Example: `Role.ADMIN` is stored as `ADMIN` instead of an integer.
    - This makes the database easier to read.

23. `@PrePersist`
    - Executes a method before the entity is saved for the first time.
    - Example: set `createdAt` automatically before inserting a new row.

24. `JpaRepository`
    - Provides many standard data access methods automatically.
    - Example: `save()`, `findById()`, `findAll()`, `deleteById()`.
    - It removes the need for manual SQL for most operations.

25. Custom derived query methods
    - Spring Data JPA derives queries from method names.
    - Example: `findTop50ByOrderByPublishedAtDesc()` returns the newest 50 headlines.
    - This lets you write database queries in plain Java method names.

26. `@Service`
    - Marks a class as a service layer component.
    - Services contain business logic and coordinate repositories.
    - Example: `AuthService` handles login, registration, and token creation.

27. `@Transactional`
    - Ensures a method runs inside a database transaction.
    - If something fails, all database changes inside the method are rolled back.
    - This keeps data consistent.

28. `@Transactional(readOnly = true)`
    - Marks a method as read-only.
    - It can be more efficient because the database knows no updates will happen.
    - Use it for methods that only fetch data.

29. `SecurityFilterChain`
    - Defines the security rules for incoming requests.
    - It specifies which URLs are public and which need authentication.
    - Example: `/api/auth/**` is public, but `/api/admin/**` requires admin role.

30. `SessionCreationPolicy.STATELESS`
    - Means the backend does not keep session state.
    - Every request must send its own authentication token.
    - This is common for modern APIs.

31. `CorsConfigurationSource`
    - Configures cross-origin request rules.
    - Example: allows the frontend at `http://localhost:4200` to make requests here.
    - Without CORS, browser calls from another domain would be blocked.

32. JWT (JSON Web Token)
    - A signed token that represents user identity.
    - Contains claims like subject, user ID, and role.
    - Example: the token is sent in the `Authorization` header as `Bearer <token>`.

33. `OncePerRequestFilter`
    - A base class for filters that run once per request.
    - `JwtAuthFilter` extends this class to validate JWTs.

## Build This Backend from Scratch

This section is the practical roadmap. If you read this carefully, you should be able to rebuild the backend.

### Step 1: Create the Maven project
- Start from an empty folder and create a Maven project.
- Use `spring-boot-starter-parent` with version `3.3.4`.
- Set `java.version` to `17` in `pom.xml`.
- Add these dependencies:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-cache`
  - `spring-boot-starter-actuator`
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
  - `h2` and `postgresql` runtime drivers
  - `lombok` (optional but useful for reducing boilerplate)

### Step 2: Set up the main class
- Create `WireBlogApplication` in package `com.wireblog`.
- Add `@SpringBootApplication`, `@EnableScheduling`, and `@EnableCaching`.
- Add `public static void main(String[] args) { SpringApplication.run(WireBlogApplication.class, args); }`.
- This is the starting point Spring Boot uses to launch the app.

### Step 3: Add configuration
- Create `src/main/resources/application.yml`.
- Configure:
  - `spring.datasource` for database connection
  - `spring.jpa` for Hibernate behavior
  - `server.port` to `8080`
  - `app.jwt.secret` and `app.jwt.expiration-ms`
  - upload directory and base URL
  - news API base URL, key, and country
- Add a `prod` profile section that reads sensitive values from environment variables with `${...}` notation.
- This makes development easy and production secure.

### Step 4: Define the package structure
- Create packages under `com.wireblog`:
  - `config`
  - `controller`
  - `dto`
  - `exception`
  - `model`
  - `repository`
  - `service`
- This structure separates concerns and keeps the project readable.

### Step 5: Build your entities
- Create entity classes in `model`.
- Example entities:
  - `User` with fields such as `id`, `displayName`, `email`, `passwordHash`, `role`, `enabled`, `createdAt`
  - `Post` with fields `id`, `title`, `content`, `slug`, `status`, `author`, `publishedAt`, `createdAt`
  - `Comment`, `Share`, `Notification`, `Sponsorship`, `NewsHeadline`
- Use `@Entity` and `@Table`.
- Use `@Id`, `@GeneratedValue`, and `@Column` to declare columns.
- Use `@Enumerated(EnumType.STRING)` for enum fields.
- Use `@PrePersist` for default values like setting `createdAt`.

### Step 6: Create repository interfaces
- For each entity, create a repository interface extending `JpaRepository`.
- Example: `UserRepository extends JpaRepository<User, Long>`.
- Add custom query methods where needed, such as `findTop50ByOrderByPublishedAtDesc()`.
- These interfaces give you database queries without SQL.

### Step 7: Write DTOs for API models
- Create request DTOs in `dto`, such as `RegisterRequest`, `LoginRequest`, `PostRequest`, `CommentRequest`, `PasswordResetRequest`.
- Create response DTOs such as `AuthResponse`, `PostDetailResponse`, `PostSummaryResponse`, and `ApiError`.
- Use DTOs to avoid exposing entity internals to API clients.

### Step 8: Build service classes
- Create service classes in `service` and annotate them with `@Service`.
- Put business logic here, not in controllers.
- Example services:
  - `AuthService` for login, registration, and JWT generation
  - `PostService` for creating, updating, publishing, and deleting posts
  - `CommentService`, `ShareService`, `NotificationService`, `SponsorshipService`
  - `NewsService` for scheduled news refresh
- Inject repositories and utility classes into each service using constructor injection.

### Step 9: Add security configuration
- Create `SecurityConfig` in `config`.
- Define a `SecurityFilterChain` bean to configure security rules.
- Disable CSRF and set session policy to stateless.
- Allow public access to `api/auth/**`, read-only post URLs, and uploads.
- Require authentication for protected endpoints.
- Require `ADMIN` role for admin routes.
- Create a `PasswordEncoder` bean with `BCryptPasswordEncoder`.

### Step 10: Implement JWT utilities and filter
- Create `JwtUtil` to generate and validate tokens.
- Use `@Value` to inject `app.jwt.secret` and `app.jwt.expiration-ms`.
- Include claims like user ID and role in the token.
- Create `JwtAuthFilter` extending `OncePerRequestFilter`.
- In the filter, read the `Authorization` header and validate the token.
- If valid, set authentication into `SecurityContextHolder`.

### Step 11: Create controllers
- Build controllers in `controller` with `@RestController`.
- Define endpoints using mapping annotations.
- Example controllers:
  - `AuthController` for login, register, verify email, password reset
  - `PostController` for listing posts, fetching one post, creating and updating posts
  - `CommentController` for adding and deleting comments
  - `NotificationController`, `SponsorshipController`, `ShareController`, `UploadController`
- Use `@Valid`, `@RequestBody`, `@PathVariable`, and `@RequestParam` for request handling.

### Step 12: Add exception handling
- Create `GlobalExceptionHandler` with `@RestControllerAdvice`.
- Add `@ExceptionHandler` methods for:
  - `ApiException`
  - `MethodArgumentNotValidException`
  - `Exception`
- Build a common `ApiError` response structure.
- This makes API errors consistent and easy to debug.

### Step 13: Configure file uploads and static serving
- Add `UploadController` to receive multipart file uploads.
- Use `MultipartFile` for the uploaded file.
- Save files to the directory configured by `app.upload.dir`.
- Create `StaticResourceConfig` implementing `WebMvcConfigurer`.
- Map `/uploads/**` to the upload directory using `addResourceHandlers()`.

### Step 14: Add scheduled news refresh
- Build `NewsService` to pull data from an external API.
- Annotate a refresh method with `@Scheduled(fixedRate = 15 * 60 * 1000)`.
- Use `RestTemplate` to call the external endpoint.
- Parse JSON with `ObjectMapper`.
- Save headlines to the database.
- Use `@EventListener(ApplicationReadyEvent.class)` to load headlines at startup.

### Step 15: Run and test the application
- Run the app with `mvn spring-boot:run` or from your IDE.
- Verify it starts on port `8080`.
- Use Postman or curl to test endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /api/posts`
  - `POST /api/upload`
- If anything fails, read the Spring Boot startup logs for configuration or bean errors.

### Step 16: Verify login and JWT flow
- Register a user.
- Login with the new user and get the JWT token.
- Send the JWT in `Authorization: Bearer <token>` when calling protected endpoints.
- Confirm admin-only endpoints fail without the proper role.

### Step 17: Deploy with production settings
- Use the `prod` profile and environment variables in deployment.
- Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `NEWS_API_KEY`.
- Ensure `spring.jpa.hibernate.ddl-auto=validate` in production.
- This prevents accidental schema changes on a live database.

### Step 18: Beginner checklist
- Do you understand the purpose of each package?
- Can you explain what a controller, service, repository, and entity do?
- Can you describe how a JWT token is created and validated?
- Can you identify which endpoints are public and which require authentication?
- Can you start the app and verify it responds on port 8080?

### Step 19: If you're not sure
- First, run the existing project and explore the source code.
- Follow the flow from controller -> service -> repository.
- Use the REST API list to experiment with requests.
- Ask yourself: "What happens when I call this URL?"
- This file now contains a complete map of the backend design.

### Step 20: Final confidence statement
- Yes, this file is now written as a beginner-friendly rebuild guide.
- If you read it carefully and follow the steps, you should be able to recreate this backend without additional help.
- It explains what each component is, why it is used, how it is wired, and how to verify it works.
    - It ensures token validation happens only once.

34. `SecurityContextHolder`
    - Holds the authentication information for the current request.
    - After JWT validation, the authenticated user is stored here.
    - Controllers can later read this information if needed.

35. `@RestControllerAdvice`
    - Catches exceptions from REST controllers globally.
    - It allows centralized handling of errors and consistent responses.
    - Example: returns structured JSON for validation failures.

36. `@ExceptionHandler`
    - Marks methods that handle specific exceptions.
    - Example: `@ExceptionHandler(MethodArgumentNotValidException.class)` handles validation errors.
    - It turns exceptions into user-friendly error responses.

37. `@Scheduled`
    - Runs a method automatically on a schedule.
    - Example: `@Scheduled(fixedRate = 900000)` runs every 15 minutes.
    - This is useful for periodic background tasks.

38. `ApplicationReadyEvent`
    - Fired when the Spring Boot app has fully started.
    - Listening to it allows one-time initialization tasks.
    - Example: refresh cached news headlines on startup.

39. `RestTemplate`
    - A Spring utility for making HTTP requests to other services.
    - Used here to fetch news from an external API.
    - Example: calling the news provider to load headlines.

40. `ObjectMapper`
    - Converts JSON text into Java objects.
    - Used to parse the response from the news API.
    - Example: reading the `articles` array from the API response.

41. `MultipartFile`
    - Represents an uploaded file in a web request.
    - Used in `UploadController` to receive image uploads.
    - Example: the frontend sends a file and the backend stores it on disk.

42. `WebMvcConfigurer`
    - Allows customization of Spring MVC behavior.
    - `addResourceHandlers()` maps URL paths to static file locations.
    - Example: serve files from the `uploads` directory at `/uploads/**`.

43. Profiles and environment variables
    - Profiles enable different settings for different environments.
    - The `prod` profile uses environment variables for secrets.
    - This makes deployments safer by avoiding hard-coded secrets.

44. H2 vs PostgreSQL
    - H2 is a lightweight, in-memory database useful for local testing.
    - PostgreSQL is a production-grade database used in real deployments.
    - The app can switch between them based on configuration.

45. Actuator health checks
    - Actuator provides endpoints to inspect app health.
    - Useful for monitoring and automated systems.
    - Example: `/actuator/health` tells whether the backend is running correctly.

46. Request processing flow summary
    - Request arrives at the backend.
    - Security filters validate the token and set the user context.
    - Controllers handle the request and delegate work to services.
    - Services perform database operations using repositories.
    - If an error happens, exception handlers turn it into a friendly response.
    - The backend returns JSON to the frontend.

## HLD, LLD, and System Design for the Backend

### What is HLD (High-Level Design)?
- HLD describes the system as a set of major components and how they interact.
- It shows the overall architecture without implementation details.
- For this backend, HLD explains the key layers, external systems, and the runtime flow.

### Backend HLD for this project
- Client: Angular frontend running in the browser.
- API Server: Spring Boot application that exposes REST endpoints.
- Security layer: JWT authentication and role-based authorization.
- Service layer: business logic for posts, comments, users, notifications, uploads, and news refresh.
- Persistence layer: JPA repositories talking to PostgreSQL or H2.
- External systems: external news API for headlines, file storage on local disk via uploads directory.

### HLD diagram
```
Browser / Angular frontend
        |
        | HTTP REST calls
        v
Spring Boot API Server
    +---------------------------+
    | 1. JwtAuthFilter          |  <--- validates Authorization header
    | 2. Controllers            |  <--- handles endpoints
    | 3. Services               |  <--- business rules
    | 4. Repositories           |  <--- database access
    +---------------------------+
        |           |            \
        |           |             \
        |           |              > external news API
        |           |             /
        |           v            /
        |     Database (PostgreSQL/H2)
        v
   Static files under /uploads/**
```

### What is LLD (Low-Level Design)?
- LLD describes classes, methods, data models, and the detailed responsibilities of each component.
- It explains how each package and class works and how data flows through the backend.
- In this project, LLD is the implementation inside `config`, `controller`, `service`, `repository`, `model`, `dto`, and `exception`.

### Backend LLD for this project
- `com.wireblog.WireBlogApplication`
    - Bootstraps Spring Boot, enables scheduling and caching.
- `config` package
    - `SecurityConfig`: configures HTTP security, filter chain, stateless sessions, public and protected endpoints.
    - `JwtUtil`: builds and validates JWT tokens with secret, expiry, and claims.
    - `JwtAuthFilter`: reads the token, validates it, and populates security context.
    - `StaticResourceConfig`: maps `/uploads/**` to the file system.
- `controller` package
    - `AuthController`: registration, login, password reset, user verification.
    - `PostController`: CRUD and list operations for posts, including admin-only actions.
    - `CommentController`: create and delete comments.
    - `UploadController`: receives multipart uploads and returns file URLs.
    - `NewsController`, `NotificationController`, `SponsorshipController`: expose related endpoints.
- `service` package
    - Contains the application business rules and transaction logic.
    - Validates input, enforces permissions, transforms DTOs to entities, and manages data persistence.
    - Example: `PostService` decides how to save drafts, publish posts, and query recent content.
- `repository` package
    - Defines JPA interfaces for each entity.
    - Provides methods such as `findByEmail`, `findTop50ByOrderByPublishedAtDesc`, and `findBySlug`.
    - Abstracts SQL away behind Spring Data JPA.
- `model` package
    - Defines entity structure with `@Entity`, field mappings, and relationships.
    - Includes `User`, `Post`, `Comment`, `Notification`, `NewsHeadline`, `Sponsorship`, and `Share`.
- `dto` package
    - Encapsulates requests and responses.
    - Avoids leaking entity internals to callers.
    - Example DTOs: `LoginRequest`, `AuthResponse`, `PostRequest`, `ApiError`.
- `exception` package
    - `ApiException`: custom runtime exception for controlled failures.
    - `GlobalExceptionHandler`: returns structured error JSON for validation, API, and unexpected errors.

### What is System Design for this backend?
- System design describes how the application meets functional and non-functional requirements.
- It includes API behavior, storage design, security, reliability, and deployment considerations.
- It helps answer: how does the backend support the whole blog system in production?

### System design considerations for this backend
- API contract
    - Public endpoints for browsing posts, reading content, logging in, registering, and uploading files.
    - Protected endpoints for creating content, admin actions, and user-specific updates.
- Authentication and authorization
    - JWT tokens are issued at login and required for protected routes.
    - `ADMIN` role gates administrative endpoints.
    - Security is stateless, so each request is independently verified.
- Data storage
    - Relational database stores users, posts, comments, notifications, and cached news.
    - H2 is used for local development and simple testing.
    - PostgreSQL is used in production for reliability and performance.
- File uploads
    - Uploaded images are saved to a local directory and served through Spring MVC static resource mapping.
    - The upload endpoint returns a public URL that the frontend can use.
- External integration
    - The backend polls a news provider on a schedule and stores headlines in local storage.
    - This is handled by a scheduled service method and provides fresh news content.
- Reliability and operations
    - Spring Boot Actuator exposes health checks and readiness endpoints.
    - Application properties separate environment-specific configuration.
    - Production profile reads secrets from environment variables.
- Maintainability
    - Clear package structure separates concerns and keeps the codebase readable.
    - DTOs and exception handling standardize API contracts.
    - Services centralize business logic so controllers stay thin.

### Rebuild confidence
- If you understand the HLD, you know how the pieces fit together.
- If you understand the LLD, you know which classes to write and what each one should do.
- If you understand the system design, you know how the backend behaves under real use.
- This file now includes the full conceptual map needed to rebuild the backend step by step.
