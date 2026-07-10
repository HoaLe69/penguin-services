# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`penguin-services` (Maven artifact `social_BE`) is the Spring Boot backend for a social network app. Its frontend lives at `https://penguin-brown-eight.vercel.app` — that origin is hardcoded in several places (CORS, WebSocket allowed origins). Stack: Java 17, Spring Boot 3.1.5, MongoDB, Spring Security + JWT, STOMP WebSocket, Cloudinary (media), Spring Mail + Thymeleaf (transactional email).

## Commands

Use the Maven wrapper (`./mvnw` on Unix, `mvnw.cmd` on Windows).

```bash
./mvnw spring-boot:run          # run locally (needs application.properties, see below)
./mvnw clean package            # build jar into target/
./mvnw clean package -DskipTests
./mvnw test                     # run all tests
./mvnw test -Dtest=SocialBeApplicationTests#contextLoads   # run a single test
```

Docker: `docker build -t social-be .` produces a multi-stage image (`mvn package -DskipTests`), runs on port 8080, healthcheck hits `/actuator/health`.

## Configuration (required before the app will start)

`src/main/resources/application.properties` is **gitignored** and must be created locally. The code injects these property keys via `@Value` — they have no defaults, so a missing/misspelled key fails startup:

- Mongo: `spring.data.mongodb.*` (standard Spring Data Mongo connection properties)
- Cloudinary: `social_app.cloudName`, `social_app.cloudApiKey`, `social_app.cloudSecretKey`
- JWT: `social_app.secret`, `social_app.secretRefresh`, `social_app.expireTime`, and **`sociall_app.expireTimeRefresh`** (note the intentional typo `sociall_` — it must match exactly in the properties file; see `JwtTokenUtil`)
- CORS: `app.cors.allowed-origin`
- Mail: `spring.mail.*` for `SendEmailService`

## Architecture

Single package root `com.example.social_be`, layered by type:

- `config/` — `SecurityConfiguration` (stateless, CSRF off, per-path auth rules), `JwtRequestFilter`, `CorsConfig`, `WebSocketConfig`, `CloudinaryConfig`, `JwtAuthenticationEntryPoint`
- `controller/` — REST controllers under `/api/**` plus `WebSocketController` (STOMP message handlers)
- `model/collection/` — MongoDB documents (the persisted entities, e.g. `UserCollection`, `PostCollection`); `model/request/` and `model/response/` are DTOs; `model/custom/CustomUserDetail`
- `repository/` — Spring Data `MongoRepository` interfaces with derived query methods
- `service/` — `UserService` (implements `UserDetailsService`), `CloudinaryServiceImpl`, `SendEmailService`
- `util/` — `JwtTokenUtil`, `AuthUtil` (cookie attach helper), `Utilties`
- `resources/templates/` — Thymeleaf email templates (`verification-email.html`, `forgot-password.html`)

### Authentication flow (cookie-based JWT, not bearer headers)

This is the most important thing to understand and easy to get wrong:

- Login (`POST /api/auth/login`) validates credentials, then sets two **HttpOnly cookies**: `token` (access, 1 day) and `refreshToken` (7 days). Tokens are never returned in the body for the standard flow.
- `JwtRequestFilter` (runs before `UsernamePasswordAuthenticationFilter`) reads the JWT from the **`token` cookie**, not the `Authorization` header. It short-circuits (skips auth) for any path containing `/api/auth`.
- `SecurityConfiguration` is `STATELESS`. `/api/auth/**`, `/hi`, `/ws/**` are public; everything else (`/api/user`, `/api/post`, `/api/conversation`, `/api/message`, `/api/comment`) requires authentication.
- Social login (`POST /api/auth/loginWithSocial`) auto-provisions a user from Google profile and uses `AuthUtil.attachTokenInCookieResponse`.
- Because auth is cookie-based, CORS must allow credentials and expose `Set-Cookie` (see `CorsConfig`).

### Real-time messaging (STOMP over WebSocket)

- `WebSocketConfig`: SockJS endpoint at `/ws`, simple broker on `/topic/` and `/queue/`, app destination prefix `/app`. Allowed origin is the hardcoded Vercel URL.
- `WebSocketController` handles `/app/messages/{id}` → broadcasts to `/topic/messages/{id}`, and `/app/comments/{id}` → `/topic/comments/{id}`. Comment/message create+delete happen inside these socket handlers (they persist via repositories directly), so real-time and persistence are coupled here rather than in a service.

### Media uploads

`PostController` and others accept `multipart/form-data` with a `file` part plus a JSON `formData` part. `CloudinaryServiceImpl.uploadFile` returns a map with `url` and `public_id`; `public_id` is stored as `cloudinaryId` so the asset can later be `destroy`-ed. Posts branch on `fileType` (`"image"` → `thumbnail`, else → `videoSrc`).

## Conventions & gotchas

- Controllers talk to repositories directly; the `service/` layer is thin and only used for cross-cutting concerns (auth/user details, cloudinary, email). Match the surrounding style rather than introducing a service layer unless asked.
- Some user-facing error strings are in Vietnamese (e.g. `"Username không tồn tại!!!"`). Keep them consistent with the endpoint you're editing.
- Repositories use Spring Data derived queries (e.g. `findUserCollectionByUserName`, `findAllByUserId`, `deleteAllByPostId`). Follow the existing naming pattern.
- Tests: only `SocialBeApplicationTests.contextLoads` exists; it will fail without a valid `application.properties`/Mongo connection since it boots the full context.
