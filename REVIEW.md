# REVIEW.md — AI Output Review Log

Complete **one review entry per slice** before moving to the next.  


---

## Slice 1 — Signup (S1)

### A. Bucket Check
List every class and method the AI produced. Tag each one:  
`Controller` / `Service` / `Repository` / `DTO` / `Security` / `Config` / `Exception`

| File / Method | Tag | In Allowed List? |
|SignupRequest.java — validation annotations on record fields|DTO|Yes|
|UserResponse.java — response record verified|DTO|Yes|
|AuthService.java — signup(SignupRequest request)|Service|Yes|
|AuthService.java — toResponse(User user)|Service|Yes|
|AuthController.java — signup(SignupRequest request)|Service|Yes|

> **Important:** Anything **not** in the allowed list → **reject, re-prompt**. Don't keep it because it works.

### B. Contract Check
Compare against API contract row A1 line by line.

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/signup` | Pass|
| Returns **201** | Pass|
| Response shape: `{ id, email, fullName, role, createdAt }` |Pass |
| No `passwordHash` field in response | Pass|
| Role in response is always `STUDENT` | Pass|

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| `@Valid` on controller parameter | Pass|
| `@Email`, `@NotBlank`, `@Size` on DTO fields |Pass |
| Duplicate email throws `DuplicateResourceException` |Pass |
| No new error/response wrapper class invented | Pass|
| No stack trace or exception message leaked to client |Pass |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Password hashed via injected `PasswordEncoder` (not `new BCryptPasswordEncoder()`) |Pass |
| `SignupRequest` has no `role` field, or it is ignored | Pass|
| Duplicate check uses normalised (lowercased/trimmed) email | Pass|
| AI did **not** touch `SecurityConfig`, add JWT class, or add `UserDetailsService` |Pass |

### E. Understanding Questions
*Answer in your own words (1–2 lines each)*

1. Where exactly is the password hashed, and why is the same password stored as a different string for two users?

   > *Your answer:* The password is hashed inside AuthService.signup() using the injected passwordEncoder.encode(request.password()). Password encoders such as BCrypt use a random salt, so the same password can generate different hashes for different users.

2. What would happen if `SignupRequest` accepted a `role` field and the AI used it?

   > *Your answer:* A user could potentially send a privileged role such as ADMIN while signing up. Therefore, the backend assigns Role.STUDENT directly instead of trusting a role supplied by the client.

3. Which class turns `DuplicateResourceException` into a 409 response?

   > *Your answer:* DuplicateResourceException is annotated with @ResponseStatus(HttpStatus.CONFLICT), and Spring MVC uses this annotation to convert the exception into an HTTP 409 Conflict response.

### F. Decision

| File | Decision | Reason (if modified/rejected) |
|---|---|---|
| SignupRequest.java| Accepted| |
| UserResponse.java| Accepted| |
| AuthService.java| Accepted| |
| AuthController.java| Accepted| |
---

## Slice 2 — Login & JWT (S2)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| LoginRequest.java| DTO|Yes |
|LoginResponse.java|DTO|Yes|
|JwtService.java|Service|Yes|
AuthService.java|Service|Yes|
|AuthController.java|Controller|Yes|

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/login` | Pass|
| Returns **200** | Pass|
| Response: `{ accessToken, tokenType: "Bearer", expiresIn }` | Pass|
| `expiresIn` matches configured value (not hardcoded) |Fail |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Same 401 message for unknown email and wrong password | Pass|
| No `try/catch` swallowing exceptions silently | Pass|

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| JWT secret comes from `@Value`, no string literal secret | Pass|
| Uses jjwt 0.12 API: `.subject()`, `.claim()`, `.id()`, `.issuedAt()`, `.expiration()`, `.signWith(key)` |Pass |
| Rejects deprecated: `setSubject`, `setExpiration`, `signWith(SignatureAlgorithm.HS256, secret)`, `Jwts.parserBuilder()` |Pass |
| `jti` is a random UUID (not the user id) | Pass|
| No password or hash in token claims |Pass |
| Parse/validate method **not** written yet (that is S3) | Pass|

### E. Understanding Questions

1. Why must the error message be identical for "no such email" and "wrong password"?

   > *Your answer:* Using the same message prevents attackers from discovering whether a particular email is registered. Otherwise, different messages could be used for user enumeration.

2. Who can read the claims inside your JWT? Who can change them?

   > *Your answer:* Anyone who has the token can decode and read its claims because JWT payloads are only encoded, not encrypted. However, they cannot modify the claims without invalidating the signature unless they know the signing secret.

3. What does `jti` give us that `sub` doesn't? (Think about S5.)

   > *Your answer:* sub identifies the user, while jti uniquely identifies one specific token. This allows an individual token to be revoked or blacklisted during logout without invalidating every token belonging to that user.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| LoginRequest.java|Accepted | Contains required email and password fields with validation annotations.|
|LoginResponse.java|Acccepted|Returns accessToken, tokenType, and expiresIn, with token type fixed as Bearer.|
|JwtService.java|Modified|JWT generation is correct, but token expiry should come from configuration rather than being hardcoded. Fixed with a follow up prompt |
|AuthService.java|Accepted|Normalises email, verifies password using injected PasswordEncoder, uses identical 401 message, and returns a JWT response.|
|AuthController.java|Accepted|Correctly exposes POST /api/auth/login and returns HTTP 200 OK.|

---

## Slice 3 — JWT Filter & Protected API (S3)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
|JwtService.java | Security |Yes |
|JwtAuthenticationFilter.java|Security|Yes|
|SecurityConfig.java|Config|Yes|
|UserController.java|Controller|Yes|

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `GET /api/courses` works **without** token |Pass |
| `GET /api/users/me` returns **401** with no token | Pass|
| `GET /api/users/me` returns **200** with valid token | Pass|
| 401 body is an `ApiError` JSON object | Pass|

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Filter does not write a response on error — continues chain | Pass|
| Filter clears SecurityContext on bad token | Pass|

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Spring Security 6 style: lambda DSL, `requestMatchers`, `authorizeHttpRequests` | Pass|
| Rejected: `WebSecurityConfigurerAdapter`, `antMatchers`, `authorizeRequests`, `.and()` | Pass|
| `permitAll` covers only the three specific public endpoints | Pass|
| `GET /api/courses` is public but `POST /api/courses` is NOT (watch for broad matcher) | Pass|
| Signature + expiry verified with `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` | Pass|
| Authorities use `ROLE_` prefix | Pass|
| Entry point and access-denied handler wired via `exceptionHandling(...)` | Pass|
| No `UserDetailsService`, `AuthenticationManager`, sessions or CORS added | Pass|
| `GET /api/users/me` returns `UserResponse`, not the entity | Pass|

### E. Understanding Questions

1. Why is CSRF protection disabled here, and when would disabling it be wrong?

   > *Your answer:* CSRF is disabled because this application uses stateless authentication with JWTs sent explicitly in the Authorization header instead of relying on browser-managed login sessions or authentication cookies. Disabling CSRF would be unsafe if authentication depended on cookies that the browser automatically sends with requests.

2. Walk through what happens to a request with an expired token, step by step, until the client sees 401.

   > *Your answer:* The JWT filter extracts the Bearer token and passes it to JwtService.parseToken(). JJWT detects that the token is expired and throws an exception. The filter catches it, clears the SecurityContext, and continues the filter chain. Because the protected endpoint requires authentication and no authenticated user is present, Spring Security invokes JsonAuthenticationEntryPoint, which returns a 401 ApiError response.

3. Why don't 401/403 errors from the filter chain reach `GlobalExceptionHandler`?

   > *Your answer:* These errors occur inside Spring Security's filter chain before the request reaches a controller. GlobalExceptionHandler handles exceptions raised during controller/MVC processing, while Spring Security uses AuthenticationEntryPoint for 401 responses and AccessDeniedHandler for 403 responses.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| JwtService.java| Accepted| Correctly parses and cryptographically verifies JWT signature and expiration using JJWT 0.12 APIs.|
|JwtAuthenticationFilter.java|Accepted|Correctly extracts Bearer tokens, creates Spring Security authentication with ROLE_ authority, clears context on errors, and continues the chain.|
|SecurityConfig.java|Accepted|Uses Spring Security 6 configuration style, exposes only the required public endpoints, enables stateless JWT authentication, and wires the required handlers and filter.|
|UserController.java|Accepted|Correctly exposes protected GET /api/users/me, uses the authenticated email to load the user, and returns UserResponse.|

---

## Slice 4 — Role Restriction (S4)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
|SecurityConfig.java | Config|Yes |
|CourseController.java|Controller|Yes|
|JsonAccessDeniedHandler.java|Security|Yes|

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/courses` with no token → **401** | Pass|
| `POST /api/courses` with STUDENT token → **403** | Pass|
| `POST /api/courses` with ADMIN token → **201** | Pass|
| 403 body is an `ApiError` JSON object | Pass|

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Role naming consistent: `hasRole("ADMIN")` ↔ authority `ROLE_ADMIN` | Pass|
| If `@PreAuthorize` used: `@EnableMethodSecurity` is present | Not used|
| Rule placed **before** `anyRequest().authenticated()` if in SecurityConfig | Pass|
| Diff is small — AI did not rewrite the whole config | Pass|

### E. Understanding Questions

1. A STUDENT calls `POST /api/courses`. Why does the server return 403 and not 401?

   > *Your answer:* The STUDENT has a valid JWT, so the user is successfully authenticated. However, the endpoint requires ROLE_ADMIN, so the authenticated STUDENT lacks permission and receives 403 Forbidden rather than 401 Unauthorized.

2. Where does the role in the JWT come from, and can the client change it?

   > *Your answer:* The role comes from the User entity loaded from the database during login and is added to the JWT by JwtService. A client can alter the visible JWT payload, but the signature would no longer be valid, so the server will reject the modified token.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| SecurityConfig.java|Accepted | Correctly restricts POST /api/courses to ADMIN and places the rule before the general authenticated rule.|
|CourseController.java|Accepted|Existing endpoint already uses DTOs and correctly returns 201 Created when authorization succeeds.|
|JsonAccessDeniedHandler.java|Accepted|Existing handler correctly returns a JSON ApiError with HTTP 403 Forbidden for authenticated users without sufficient authority.|

---

## Slice 5 — Logout (S5) *(stretch)*

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| TokenDenyList.java| Security|Yes |
|JwtAuthenticationFilter.java|Security|Yes|
|AuthController.java|Controller|Yes|

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/auth/logout` returns **204** |Pass |
| Same token returns **401** after logout | Pass|
| Fresh login after logout returns a new working token | Pass|

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Logout adds entry to server-side denylist (not just tells client to delete token) | Pass|
| Denylist keyed by **jti**, not email | Pass|
| Map is `ConcurrentHashMap` (thread-safe) | Pass|
| Logout endpoint is NOT under `permitAll` |Pass |
| Filter checks the denylist and treats denylisted jti as invalid | Pass|
| (Bonus) Expired entries are removed from the map |Pass |

### E. Understanding Questions

1. Why does logout need server-side state if JWT is "stateless"?

   > *Your answer:* A valid JWT normally remains usable until it expires because the server does not keep a session for it. To invalidate a token immediately during logout, the server must remember that specific token's jti in a denylist, which introduces a small amount of server-side state.

2. What happens to the denylist when the server restarts? What if there are two server instances?

   > *Your answer:* Because the denylist is stored only in memory, restarting the server clears it and previously revoked tokens may work again until they expire. With two server instances, each instance has its own denylist, so a token revoked on one instance would not automatically be revoked on the other unless a shared store such as Redis or a database were used.

3. Why store the expiry time with each jti?

   > *Your answer:* Once a JWT has naturally expired, keeping its jti in the denylist is unnecessary because the JWT parser will reject it anyway. Storing the expiry allows old denylist entries to be safely removed and prevents the map from growing indefinitely.

### F. Decision

| File | Decision | Reason |
|---|---|---|
|TokenDenyList.java|Accepted|Uses a thread-safe ConcurrentHashMap, stores revoked token IDs by jti, and removes naturally expired entries.|
|JwtAuthenticationFilter.java|Accepted|Checks the parsed token's jti against the denylist and leaves revoked tokens unauthenticated.|
|AuthController.java|Accepted|Logout parses the current token, stores its jti and expiry in the denylist, and returns HTTP 204 No Content.|
---

## No-AI Change Tasks

|File||What changed|
src/main/resources/application.properties - IntelliJ environment variables	Changed JWT expiry from 3600 seconds to 900 seconds (15 minutes).
src/main/java/com/esd/lab2/service/CourseService.java- Added a method to fetch a course by ID and throw ResourceNotFoundException if it does not exist.
src/main/java/com/esd/lab2/controller/CourseController.java	- Added GET /api/courses/{id} to return one course by ID.
src/main/java/com/esd/lab2/config/SecurityConfig.java	- Made GET /api/courses/{id} public and changed GET /api/courses from public to authenticated.
src/main/java/com/esd/lab2/dto/UpdateProfileRequest.java	 - New DTO containing only fullName, with @NotBlank and @Size(max = 100).
src/main/java/com/esd/lab2/controller/UserController.java -	Added PUT /api/users/me to update only the authenticated user's fullName. Existing GET /me stays unchanged.
src/main/java/com/esd/lab2/dto/SignupRequest.java -	Added password validation requiring at least one digit using @Pattern.

**Explanation (your own words):**
> *Describe what you changed and why it works.*
JWT expiry changed from 3600s to 900s.
- Added public GET /api/courses/{id}; missing IDs return 404.
- create a small request DTO that accepts only fullName, then add an authenticated PUT /api/users/me endpoint that identifies the current user from the JWT and updates only that field. The email, role, password, and ID must remain unchanged
- Added password digit validation during signup.
- GET /api/courses changed from public to authenticated; old anonymous-access tests now expect 401 instead of 200.
