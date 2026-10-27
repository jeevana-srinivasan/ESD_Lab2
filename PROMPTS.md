# PROMPTS.md — AI Prompt Log

Record every prompt you send to the AI **starting from Slice 2**.  
Slice 1 is done manually — no AI. See `README.md` for the prompt template structure.

> **Rules:**
> - One prompt (+ follow-ups) per slice
> - Use the structured template from the README — free-form prompts are not allowed
> - Do not back-fill this file after the lab

---

## Example — How a good S1 prompt looks (for reference only, S1 is done manually)

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout:
  com.esd.lab2
  ├── config/       SecurityConfig.java, AdminSeeder.java
  ├── controller/   CourseController.java
  ├── service/      CourseService.java
  ├── dto/          CourseRequest.java, CourseResponse.java
  ├── entity/       User.java, Role.java (enum), Course.java
  ├── repository/   UserRepository.java, CourseRepository.java
  ├── exception/    ApiError.java, GlobalExceptionHandler.java,
  │                 DuplicateResourceException.java, InvalidCredentialsException.java,
  │                 ResourceNotFoundException.java
  └── security/     JsonAuthenticationEntryPoint.java, JsonAccessDeniedHandler.java


TASK
Implement only POST /api/auth/signup.
Request: SignupRequest { email (valid email, required), password (required, 8–64 chars),
         fullName (required, not blank, max 100) }.
Response: 201 with UserResponse { id, email, fullName, role, createdAt }.
The new user's role is always STUDENT, even if the request contains a role field.
Store email trimmed and lowercased. Hash the password with the existing PasswordEncoder bean.
If the email already exists, throw DuplicateResourceException.

ALLOWED TO CREATE OR CHANGE
dto/SignupRequest.java, dto/UserResponse.java,
service/AuthService.java (signup method only),
controller/AuthController.java (signup endpoint only)

RULES
- Do not create or modify any file not listed above.
- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.
- Use DTOs for request and response. Never return an entity from a controller.
- Use jakarta.validation annotations for input validation.
- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.
- If something is unclear, add a TODO comment instead of guessing.

OUTPUT
Each file in full, with its path. Then a list of anything you assumed.
```

---

## Slice 2 — Login & JWT (S2)

**Prompt 1:**

```
CONTEXT\
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.\
Package layout: src/main/java/com/esd/lab2/\
├── Lab2Application.java\
├── config/\
│   ├── SecurityConfig.java       TEMPORARY: permits all. Replace in S3.\
│   └── AdminSeeder.java          Seeds admin@esd.local at startup.\
├── controller/\
│   └── CourseController.java     GET + POST /api/courses (unprotected for now)\
├── service/\
│   └── CourseService.java\
├── dto/\
│   ├── CourseRequest.java\
│   └── CourseResponse.java\
├── entity/\
│   ├── User.java\
│   ├── Role.java                 enum: STUDENT, ADMIN\
│   └── Course.java\
├── repository/\
│   ├── UserRepository.java       findByEmail, existsByEmail\
│   └── CourseRepository.java\
├── exception/\
│   ├── ApiError.java\
│   ├── GlobalExceptionHandler.java\
│   ├── DuplicateResourceException.java    409\
│   ├── InvalidCredentialsException.java   401\
│   └── ResourceNotFoundException.java     404\
└── security/\
&#x20;   ├── JsonAuthenticationEntryPoint.java   writes ApiError for 401 from filter chain\
&#x20;   └── JsonAccessDeniedHandler.java        writes ApiError for 403 from filter chain\
Existing code you must use (do not rewrite it):

AuthService.java

package com.esd.lab2.service;\
\
import com.esd.lab2.dto.SignupRequest;\
import com.esd.lab2.dto.UserResponse;\
import com.esd.lab2.entity.Role;\
import com.esd.lab2.entity.User;\
import com.esd.lab2.exception.DuplicateResourceException;\
import com.esd.lab2.repository.UserRepository;\
import org.springframework.security.crypto.password.PasswordEncoder;\
import org.springframework.stereotype.Service;\
\
import java.util.Locale;\
\
/\*\*\
&#x20;\* S1 — FILL IN MANUALLY (no AI for this slice).\
&#x20;\*\
&#x20;\* Steps to implement signup():\
&#x20;\*   1. Normalise the email: trim it and convert to lowercase.\
&#x20;\*   2. Check if the email already exists using userRepository.existsByEmail(...).\
&#x20;\*      If it does, throw DuplicateResourceException with a helpful message.\
&#x20;\*   3. Hash the password using passwordEncoder.encode(...).\
&#x20;\*      Never store plaintext. Never use new BCryptPasswordEncoder() — use the injected bean.\
&#x20;\*   4. Create a new User entity. Role must always be STUDENT — do not read role from the request.\
&#x20;\*   5. Save the user with userRepository.save(...).\
&#x20;\*   6. Return a UserResponse built from the saved entity.\
&#x20;\*\
&#x20;\* S2 onwards: login() and other methods will be added here by AI.\
&#x20;\*/\
@Service\
public class AuthService {\
\
&#x20;   private final UserRepository userRepository;\
&#x20;   private final PasswordEncoder passwordEncoder;\
\
&#x20;   public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {\
&#x20;       this.userRepository  = userRepository;\
&#x20;       this.passwordEncoder = passwordEncoder;\
&#x20;   }\
\
&#x20;   /\*\*\
&#x20;    \* Registers a new user. Role is always STUDENT regardless of what the request contains.\
&#x20;    \*/\
&#x20;   public UserResponse signup(SignupRequest request) {\
&#x20;       // TODO 1: normalise the email (trim + lowercase)\
&#x20;       String email = request.email().trim().toLowerCase(Locale.ROOT);\
\
&#x20;       // TODO 2: check for duplicate email and throw DuplicateResourceException if found\
&#x20;       if (userRepository.existsByEmail(email)){\
&#x20;           throw new DuplicateResourceException("User with email" + email + "already exists");\
&#x20;       }\
\
&#x20;       // TODO 3: hash the password using the injected passwordEncoder bean\
&#x20;       String hashedPassword = passwordEncoder.encode(request.password());\
\
&#x20;       // TODO 4: build and save the User entity (role = Role.STUDENT always)\
&#x20;       User user = new User(email,hashedPassword, request.fullName(), Role.STUDENT);\
&#x20;       User savedUser = userRepository.save(user);\
\
&#x20;       // TODO 5: return a UserResponse — never return the User entity directly\
&#x20;       return toResponse(savedUser);\
&#x20;   }\
\
&#x20;   // ── helper ────────────────────────────────────────────────────────────────\
\
&#x20;   private UserResponse toResponse(User user) {\
&#x20;       // TODO: map User entity fields to UserResponse record\
&#x20;       return new UserResponse(user.getId(),\
&#x20;               user.getEmail(),\
&#x20;               user.getFullName(),\
&#x20;               user.getRole(),\
&#x20;               user.getCreatedAt());\
&#x20;   }\
}\


AuthController.java\
package com.esd.lab2.controller;\
\
import com.esd.lab2.dto.SignupRequest;\
import com.esd.lab2.dto.UserResponse;\
import com.esd.lab2.service.AuthService;\
import jakarta.validation.Valid;\
import org.springframework.http.HttpStatus;\
import org.springframework.http.ResponseEntity;\
import org.springframework.web.bind.annotation.\*;\
\
/\*\*\
&#x20;\* S1 — FILL IN MANUALLY (no AI for this slice).\
&#x20;\*\
&#x20;\* This controller handles all /api/auth endpoints.\
&#x20;\* In S1 you add the signup endpoint.\
&#x20;\* In S2 (AI) you will add login and later logout.\
&#x20;\*\
&#x20;\*/\
@RestController\
@RequestMapping("/api/auth")\
public class AuthController {\
\
&#x20;   private final AuthService authService;\
\
&#x20;   public AuthController(AuthService authService) {\
&#x20;       this.authService = authService;\
&#x20;   }\
\
&#x20;   /\*\*\
&#x20;    \* A1 — POST /api/auth/signup\
&#x20;    \* Public endpoint. Returns 201 with UserResponse on success.\
&#x20;    \*/\
&#x20;   @PostMapping("/signup")\
&#x20;   public ResponseEntity\<UserResponse> signup(@Valid @RequestBody SignupRequest request) {\
&#x20;       // TODO: call authService.signup(request) and return 201 with the result\
&#x20;       UserResponse response = authService.signup(request);\
\
&#x20;       return ResponseEntity\
&#x20;               .status(HttpStatus.CREATED)\
&#x20;               .body(response);\
&#x20;   }\
\
&#x20;   // S2: login endpoint will be added here by AI\
&#x20;   // S5: logout endpoint will be added here by AI\
}\
\
TASK\
Implement only: Create record with `@NotBlank` and `@Email` on `email`, and `@NotBlank` on `password`.  &#x20;

Create record with fields `accessToken` (String), `tokenType` (String default `"Bearer"`), and `expiresIn` (long seconds).  &#x20;

- Implement `generateToken(User user)` using JJWT 0.12 API. Read `jwt.secret` from `@Value`, configure claims (`sub` = email, `role`, `jti` = random UUID, `iat`, `exp`), and sign with HMAC-SHA256.

Implement `login(LoginRequest)`: look up user by lowercase/trimmed email, verify password with `passwordEncoder.matches()`, throw `InvalidCredentialsException` with identical message `"Invalid email or password"` on any failure, generate token, and return `LoginResponse`.  &#x20;

Add `POST /api/auth/login` accepting `@Valid @RequestBody LoginRequest`, calling `authService.login()`, and returning HTTP `200 OK`   \
Contract: 
LoginRequest    { email: required, password: required }

LoginResponse   { accessToken, tokenType: "Bearer", expiresIn: \<seconds> }
**Rules that are part of the contract:**

- Signup always creates a `STUDENT`. Ignore any `role` field in the request.
- Store email in lowercase and trimmed.
- Login failure returns the **same** 401 message whether the email is unknown or the password is wrong: `"Invalid email or password"`.
- JWT contains: `sub` = email, `role` claim, `jti` (random UUID), `iat`, `exp`. Signed with HS256.
- After logout, the same token gets 401 on every protected endpoint.\
  \
  ALLOWED TO CREATE OR CHANGE\
 src/main/java/com/esd/lab2/dto/LoginRequest.java   create

src/main/java/com/esd/lab2/dto/LoginResponse.java  create

src/main/java/com/esd/lab2/security/JwtService.java   create

src/main/java/com/esd/lab2/service/AuthService.java

src/main/java/com/esd/lab2/controller/AuthController.java
\
RULES\
\- Do not create or modify any file not listed above.\
\- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.\
\- Use DTOs for request and response. Never return an entity from a controller.\
\- Use jakarta.validation annotations for input validation.\
\- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.\
\- If something is unclear, add a TODO comment instead of guessing.\
\
OUTPUT\
Each file in full, with its path. Then a list of anything you assumed.
```

**Follow-up (if any):**

```
Do not harcode the expiration and give the changes to be made to make the expiration come from the configuration values
```

---

## Slice 3 — JWT Filter & Protected API (S3)

**Prompt 1:**

```
CONTEXT\
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.\
Package layout: same as above with the extra files we have created\
Existing code you must use (do not rewrite it):\
JwtService.java&#x20;

SecurityConfig.java

package com.esd.lab2.config;\
\
import org.springframework.context.annotation.Bean;\
import org.springframework.context.annotation.Configuration;\
import org.springframework.security.config.annotation.web.builders.HttpSecurity;\
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;\
import org.springframework.security.config.http.SessionCreationPolicy;\
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;\
import org.springframework.security.crypto.password.PasswordEncoder;\
import org.springframework.security.web.SecurityFilterChain;\
\
/\*\*\
&#x20;\* TEMPORARY SecurityConfig — permits all requests so Slices 1 and 2 work\
&#x20;\* without authentication in place.\
&#x20;\*\
&#x20;\* NOTE: Replace the filterChain bean in Slice 3.\
&#x20;\*       Do NOT touch this file before Slice 3.\
&#x20;\*/\
@Configuration\
@EnableWebSecurity\
public class SecurityConfig {\
\
&#x20;   @Bean\
&#x20;   public PasswordEncoder passwordEncoder() {\
&#x20;       return new BCryptPasswordEncoder();\
&#x20;   }\
\
&#x20;   // TEMPORARY: replaced in Slice 3\
&#x20;   @Bean\
&#x20;   public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {\
&#x20;       http\
&#x20;           .csrf(csrf -> csrf.disable())\
&#x20;           .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))\
&#x20;           .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());\
&#x20;       return http.build();\
&#x20;   }\
}\
\
TASK\
Implement only:   &#x20;

Implement token parsing and verifictaion using `wts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()` to cryptographically verify signature, check expiration, and extract `Claims`.  &#x20;

Create a filter extending `OncePerRequestFilter`. Intercept incoming requests, extract the `Authorization: Bearer <token>` header, parse claims, and populate `SecurityContextHolder` with `UsernamePasswordAuthenticationToken` using authority `"ROLE_" + role`. Clear context on error and continue chain.  &#x20;

Configure `SecurityFilterChain`: disable CSRF, set session policy to `STATELESS`, permit public endpoints (`POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/courses`), require authentication for `anyRequest()`, wire `jsonAuthenticationEntryPoint` and `jsonAccessDeniedHandler`, and register `jwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.  &#x20;

Add `GET /api/users/me` requiring authentication. Retrieve user details from database by `authentication.getName()`, and return `UserResponse` with HTTP `200 OK`.   \
Contract:&#x20;

SignupRequest   { email: valid email, required
                  password: required, 8-64 chars
                  fullName: required, not blank, max 100 }

UserResponse    { id, email, fullName, role, createdAt }   // no password hash!

LoginRequest    { email: required, password: required }

LoginResponse   { accessToken, tokenType: "Bearer", expiresIn: <seconds> }

```
```
ALLOWED TO CREATE OR CHANGE<exact list of files/methods>
JwtService.java
/security/JwtAuthenticationFilter =>create
/config/SecurityConfig.java
/controller/UserController.java =>create

RULES- Do not create or modify any file not listed above.- 
Use the existing exceptions in com.esd.lab2.exception. 
Do not create new error classes.- Use DTOs for request and response. 
Never return an entity from a controller.- 
Use jakarta.validation annotations for input validation.- 
Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.- 
If something is unclear, add a TODO comment instead of guessing.
OUTPUTEach file in full, with its path. 
Then a list of anything you assumed.
```
```

**Follow-up (if any):**

```
(paste here)
```

---

## Slice 4 — Role Restriction (S4)

**Prompt 1:**

```
CONTEXT\
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.\
Package layout: same as before with the extra files created\
Existing code you must use (do not rewrite it):\
config/SecurityConfig.java
\
\
TASK\
Implement only: **Enforce ADMIN Role in Security Configuration**   Add `.requestMatchers(HttpMethod.POST, "/api/courses").hasRole("ADMIN")` immediately before `.anyRequest().authenticated()`  &#x20;
**Verify Endpoints and Rejection Handling**  &#x20;
/controller/CourseController.java : Endpoint `POST /api/courses` creates courses (HTTP `201 Created`) when caller has `ADMIN` role.  &#x20;
/security/JsonAccessDeniedHandler.java Intercepts `AccessDeniedException` when a non-admin (e.g. `STUDENT`) attempts to post, returning standard HTTP `403 Forbidden` JSON.   \
Contract: same as before\
\
ALLOWED TO CREATE OR CHANGE\
/controller/CourseController.java
/security/JsonAccessDeniedHandler.java
SecurityConfig.java\
\
RULES\
\- Do not create or modify any file not listed above.\
\- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.\
\- Use DTOs for request and response. Never return an entity from a controller.\
\- Use jakarta.validation annotations for input validation.\
\- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.\
\- If something is unclear, add a TODO comment instead of guessing.\
\
OUTPUT\
Each file in full, with its path. Then a list of anything you assumed.
```

**Follow-up (if any):**

```
(paste here)
```

---

## Slice 5 — Logout *(stretch, S5)*

**Prompt 1:**

```
CONTEXT\
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.\
Package layout: same as above and the files created\
Existing code you must use (do not rewrite it):\
JwtAuthenticationFilter.java
\
\
TASK\
Implement only: C**reate In-Memory Token Denylist**   Create thread-safe `@Component` using `ConcurrentHashMap<String, Instant>`. Implement `revoke(jti, expiry)` to store revoked token IDs, `isRevoked(jti)` to check revocation, and `cleanExpired()` to clean up naturally expired tokens.  &#x20;
**Expose the Logout Endpoint**  &#x20;
Add `POST /api/auth/logout`. Extract Bearer token from `Authorization` header, parse claims to retrieve `jti` and expiration date, call `tokenDenylist.revoke(jti, expiry)`, and return HTTP `204 No Content`.  &#x20;
**Enforce Denylist Check in JWT Filter**  &#x20;
Add `POST /api/auth/logout`. Extract Bearer token from `Authorization` header, parse claims to retrieve `jti` and expiration date, call `tokenDenylist.revoke(jti, expiry)`, and return HTTP `204 No Content`.   \
Contract: \<paste the rows and DTOs from the contract>\
\
ALLOWED TO CREATE OR CHANGE\
AuthController.java
JwtAutheticationFilter.java
/security/TokenDenyList.java\
\
RULES\
\- Do not create or modify any file not listed above.\
\- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.\
\- Use DTOs for request and response. Never return an entity from a controller.\
\- Use jakarta.validation annotations for input validation.\
\- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.\
\- If something is unclear, add a TODO comment instead of guessing.\
\
OUTPUT\
Each file in full, with its path. Then a list of anything you assumed.
```

**Follow-up (if any):**

```
(paste here)
```
