# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`penguin-services` (Maven artifact `social_BE`) is the Spring Boot backend for a social network app. Its frontend lives at `https://penguin-brown-eight.vercel.app`; allowed origins (CORS and WebSocket) are configured via `social-app.cors.allowed-origins`, not hardcoded — see `CorsConfig`/`WebSocketConfig`. Stack: Java 17, Spring Boot 3.1.5, MongoDB, Spring Security + JWT, STOMP WebSocket, Cloudinary (media), Spring Mail + Thymeleaf (transactional email).

## Commands

Use the Maven wrapper (`./mvnw` on Unix, `mvnw.cmd` on Windows).

```bash
./mvnw spring-boot:run          # run locally (needs application.properties, see below)
./mvnw clean package            # build jar into target/
./mvnw clean package -DskipTests
./mvnw test                     # run all tests
./mvnw test -Dtest=JwtTokenUtilTest              # run a single test class
./mvnw test -Dtest=SocialBeApplicationTests#contextLoads   # run a single test method
```

`docker compose up` starts Mongo (and the app, built from the local `Dockerfile`) with working dev defaults already wired — see `docker-compose.yml` / `.env.example` for the env vars it sets. `docker compose --profile tools up` also starts `mongo-express` on port 8081.

`docker build -t social-be .` alone produces a multi-stage image (`mvn package -DskipTests`) that runs on port 8080. There is no Docker `HEALTHCHECK` yet and no `spring-boot-starter-actuator` dependency — `/actuator/health` does not exist in this codebase (see the Refactor Board's REF-21 ticket).

CI: `.github/workflows/main_penguin-apis.yml` builds and deploys to Azure Web App on push to `main`. It does not run tests as a PR gate (REF-23).

## Configuration (required before the app will start)

`src/main/resources/application.properties` is **gitignored** and must be created locally (or set the equivalent env vars — see `docker-compose.yml` / `.env.example`). App-specific config is a single typed, validated `@ConfigurationProperties` bean (`config/SocialAppProperties`) under the `social-app.*` prefix; a missing/blank/too-short required value fails startup with a field-by-field validation error instead of an obscure `@Value` resolution failure:

- Mongo: `spring.data.mongodb.*` (standard Spring Data Mongo connection properties, not part of `SocialAppProperties`). `application.yml` sets `spring.data.mongodb.auto-index-creation: true`, so every `@Indexed`/`@CompoundIndex` on a `model/collection/*` class is created/updated on boot — including a `unique` index on `UserCollection.userName` (and a sparse-unique one on `socialId`), so a collection with pre-existing duplicates will fail to start until they're cleaned up.
- Cloudinary: `social-app.cloudinary.cloud-name`, `social-app.cloudinary.cloud-api-key`, `social-app.cloudinary.cloud-secret-key`
- JWT: `social-app.jwt.secret`, `social-app.jwt.refresh-secret` (each must be **≥ 64 characters** — HS512 needs a 512-bit key, see `JwtTokenUtil`), `social-app.jwt.access-ttl`, `social-app.jwt.refresh-ttl` (seconds)
- CORS: `social-app.cors.allowed-origins` (list; feeds both `CorsConfig` and the WebSocket allowed origins in `WebSocketConfig`)
- Cookies: `social-app.cookie.secure` (default `true`; set `false` for local http-only dev, which also relaxes `SameSite` from `None` to `Lax` — see `CookieService`)
- Mail: `spring.mail.*` for `SendEmailService`

Property keys use kebab-case but bind via Spring's relaxed rules, so the equivalent env var for e.g. `social-app.jwt.secret` is `SOCIAL_APP_JWT_SECRET`. `src/main/resources/application.yml` is committed and carries only non-secret structural defaults (`social-app.cookie.secure`, plus a `dev` Spring profile that relaxes cookies and defaults CORS to `http://localhost:3000`) — it never overrides secrets, which must always come from env vars or the gitignored properties file.

## Architecture

Single package root `com.example.social_be`, layered by type:

- `config/` — `SocialAppProperties` (typed config, see above), `SecurityConfiguration` (stateless, CSRF off, per-path auth rules), `JwtRequestFilter`, `CorsConfig`, `WebSocketConfig`, `CloudinaryConfig`, `JwtAuthenticationEntryPoint`
- `controller/` — REST controllers under `/api/**` plus `WebSocketController` (STOMP message handlers)
- `model/collection/` — MongoDB documents (the persisted entities, e.g. `UserCollection`, `PostCollection`); `model/request/` and `model/response/` are DTOs; `model/custom/CustomUserDetail`
- `repository/` — Spring Data `MongoRepository` interfaces with derived query methods
- `service/` — `UserService` (implements `UserDetailsService`), `CloudinaryServiceImpl`, `SendEmailService`, `CommentService`/`MessageService` (comment/message persistence + post counter updates for the WebSocket flows — see below)
- `security/` — `SecurityUtils`: reads the authenticated caller's id from the JWT principal (`SecurityContextHolder`) and enforces ownership (`requireSelf`). **Always derive identity this way in a new endpoint — never trust a client-supplied id in the request body or path for "who is acting" decisions**; that was a real IDOR vulnerability here before (see `git log --grep=REF-5`).
- `exception/` — `GlobalExceptionHandler` (`@RestControllerAdvice`) is the single place HTTP error responses get built (`ErrorResponse` in `dto/`): `ResourceNotFoundException` → 404, `ForbiddenException`/`AccessDeniedException` → 403, bad-credentials/auth → 401, `MethodArgumentNotValidException` → 400 with per-field messages, anything else → 500 (logged once here; don't add local try/catch that swallows exceptions and returns a generic error — let them propagate so they're logged at this one boundary).
- `util/` — `JwtTokenUtil` (jjwt 0.12.x), `CookieService` (the only code path that sets/clears the `token`/`refreshToken` cookies), `Utilties` (misc helpers incl. `anchoredLiteralPrefix` for safe regex search)
- `resources/templates/` — Thymeleaf email templates (`verification-email.html`, `forgot-password.html`)

### Authentication flow (cookie-based JWT, not bearer headers)

This is the most important thing to understand and easy to get wrong:

- Login (`POST /api/auth/login`) validates credentials, then sets two **HttpOnly cookies** via `CookieService`: `token` (access) and `refreshToken` (refresh), with TTLs sourced from `SocialAppProperties.Jwt` so they can never drift from the JWT's own expiry. Tokens are never returned in the body for the standard flow (the `/refresh-token` response body is the one exception — it returns a `JwtResponse` as well as setting cookies).
- `JwtRequestFilter` (runs before `UsernamePasswordAuthenticationFilter`) reads the JWT from the **`token` cookie**, not the `Authorization` header. It short-circuits (skips auth) for any path containing `/api/auth`. An expired/malformed/unsigned token or unknown user is caught there and just leaves the request unauthenticated (no exception thrown) so `JwtAuthenticationEntryPoint` returns a clean 401 for protected paths.
- `SecurityConfiguration` is `STATELESS`. `/api/auth/**`, `/hi`, `/ws/**` are public; everything else (`/api/user`, `/api/post`, `/api/conversation`, `/api/message`, `/api/comment`) requires authentication.
- Social login (`POST /api/auth/loginWithSocial`) auto-provisions a user from Google profile data and calls `CookieService` the same way.
- Because auth is cookie-based, CORS must allow credentials and expose `Set-Cookie` (see `CorsConfig`, which builds a single `CorsConfigurationSource` bean that `SecurityConfiguration`'s `.cors(Customizer.withDefaults())` picks up automatically).
- Authorization (as opposed to authentication) is enforced per-endpoint via `SecurityUtils`, not by the filter — e.g. `PostController.edit`/`delete` check `SecurityUtils.currentUserId()` against the post's owner and throw `ForbiddenException` otherwise.

### Real-time messaging (STOMP over WebSocket)

- `WebSocketConfig`: SockJS endpoint at `/ws`, simple broker on `/topic/` and `/queue/`, app destination prefix `/app`, allowed origins from `social-app.cors.allowed-origins`.
- `WebSocketController` is a thin `@Controller`: `/app/messages/{id}` → `MessageService`, broadcasts to `/topic/messages/{id}`; `/app/comments/{id}` → `CommentService` (also updates the post's comment counter), broadcasts to `/topic/comments/{id}`. Handlers return plain payload objects (never `ResponseEntity` — that's an HTTP construct with no STOMP meaning). A `@MessageExceptionHandler` routes failures to the sending user's `/queue/errors` instead of swallowing them.

### Media uploads

`PostController` and others accept `multipart/form-data` with a `file` part plus a JSON `formData` part. `CloudinaryServiceImpl.uploadFile` returns a map with `url` and `public_id`; `public_id` is stored as `cloudinaryId` so the asset can later be `destroy`-ed. Posts branch on `fileType` (`"image"` → `thumbnail`, else → `videoSrc`).

## Conventions & gotchas

- Controllers mostly talk to repositories directly; a real service layer only exists where it's landed so far (`CommentService`/`MessageService` for the WebSocket flows, `UserService`/`CloudinaryServiceImpl`/`SendEmailService` for cross-cutting concerns). `PostController`/`UserController`/`ConversationController`/`CommentController`/`MessageController` still hold their own business logic — match the surrounding style in a given controller rather than introducing a service layer there unless asked (a full service-layer migration is tracked as REF-13).
- Don't add `@Async` or `@Transactional` to controller methods, and don't reach for `@Transactional` at all until there's a service layer to scope it in (REF-13). Mongo here is a standalone instance, not a replica set, so `@Transactional` has no real effect anywhere in this codebase (REF-20's decision: not worth standing up a replica set for one call site — see `UserController.interactiveUser` for the pattern instead). `@Async` on a method returning `ResponseEntity` doesn't do what it looks like it does either. Both were removed from every controller for exactly this reason. For a multi-document write where partial failure matters, prefer atomic single-document `MongoTemplate` updates (`Update().addToSet(...)`/`.pull(...)`) over read-modify-save, the way `interactiveUser` does it — it doesn't make the whole operation atomic, but it does eliminate the lost-update race from two concurrent requests overwriting the same document.
- Request DTOs bound via `@RequestBody` should get `jakarta.validation` annotations (`@NotBlank`, `@Email`, `@Size`, etc.) and the handler param needs `@Valid` — `GlobalExceptionHandler` already turns violations into 400 + per-field messages. Also give every request DTO a no-args constructor (Lombok's `@NoArgsConstructor`); one was missing on `ConversationRequest` and Jackson couldn't reliably deserialize it at all.
- Some user-facing error strings are in Vietnamese (e.g. `"Username không tồn tại!!!"`). Keep them consistent with the endpoint you're editing (unifying the language/response envelope across the whole API is tracked as REF-22, not yet done).
- Repositories use Spring Data derived queries (e.g. `findUserCollectionByUserName`, `findAllByUserId`, `deleteAllByPostId`). Follow the existing naming pattern. Anything backing a user-facing search (see `UserRepository.findByLikeEmail`) takes an already-escaped/anchored pattern, not raw user input directly — build it with `Utilties.anchoredLiteralPrefix`, never interpolate user input into a `$regex` query string.
- `model/collection/Token.java` + `repository/TokenRepository.java`, `model/response/UserResponseLogin.java`, and `model/request/MessageRequest.java` are dead code — nothing references them. Don't assume they're wired into the auth flow just because they look related to it.
- Tests live under `src/test/java/com/example/social_be/`, organized by the package they cover (`controller/`, `service/`, `config/`, `util/`, `exception/`). Most are plain Mockito/MockMvc-standalone unit tests that don't need Mongo. The one exception is `SocialBeApplicationTests.contextLoads`, which boots the full Spring context and will fail without a valid `application.properties`/Mongo connection.
