# RescueScan — Complete Architecture & Code Reference

> Project: Emergency QR Code System
> Tech: Java 17, Spring Boot 3.2.0, Spring MVC, Spring Data JPA, Spring Security, Thymeleaf, ZXing, SQL Server/H2
> Build: Maven (wrapper)
> Structure: Strict layered MVC — Controller -> Service -> Repository -> Entity/DB

## 1. High-Level Architecture

```
                     Browser (User / Scanner)
                            |
                    +-------+-------+
                    |  HTTP Request  |
                    +-------+-------+
                            |
              +-------------+-------------+
              |   Spring Security Filter   |
              |     Chain (SecurityConfig) |
              |  /login, /register,        |
              |  /qr/public/** -> permitAll |
              |  /dashboard/** -> auth only |
              +-------------+-------------+
                            |
              +-------------+-------------+
              |     Controllers (MVC)      |
              |  AuthController            |
              |  DashboardController       |
              |  PublicQrController        |
              +-------------+-------------+
                            |
              +-------------+-------------+
              |     Services (Business)    |
              |  UserService               |
              |  EmergencyContactService   |
              |  QrCodeService             |
              |  CustomUserDetailsService  |
              +-------------+-------------+
                            |
              +-------------+-------------+
              |    Repositories (Data)     |
              |  UserRepository            |
              |  EmergencyContactRepo      |
              +-------------+-------------+
                            |
              +-------------+-------------+
              |   JPA Entities + DB        |
              |  User -> users table        |
              |  EmergencyContact ->        |
              |    emergency_contacts      |
              +----------------------------+
```

## 2. Build & Configuration Files

### 2.1 pom.xml (89 lines)

Parent: spring-boot-starter-parent:3.2.0. Coordinates: com.rescuescan:rescuescan:1.0.0. Java 17.
Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-thymeleaf, thymeleaf-extras-springsecurity6, mssql-jdbc (runtime), com.google.zxing:core:3.5.2 + javase:3.5.2, spring-boot-starter-test + spring-security-test (test). Build plugin: spring-boot-maven-plugin.

### 2.2 application.properties (14 lines)

| Line | What it does |
|------|-------------|
| 1 | App name: RescueScan |
| 3 | Datasource: jdbc:sqlserver://localhost:1433;databaseName=RescueScanDB;encrypt=false |
| 4-5 | Username: rescuescant, Password: rescuescanadmin123 |
| 6 | Driver: com.microsoft.sqlserver.jdbc.SQLServerDriver |
| 8 | Dialect: org.hibernate.dialect.SQLServerDialect |
| 9 | DDL auto: update (Hibernate creates/updates tables at startup) |
| 10 | Show SQL: false |
| 12 | Thymeleaf cache: false (dev mode, templates reload on refresh) |
| 14 | Server port: 8080 |

### 2.3 .gitignore (48 lines)

Excludes: data/ (H2 DB), *.mv.db, target/, IDE files (.idea/, .vscode/, *.iml), OS files, logs, temp-extract/, temp-lib/, run.ps1, maven-wrapper.jar.

### 2.4 .gitattributes

`* text=auto` — enforces LF line endings on commit.

### 2.5 mvnw.cmd (31 lines)

| Lines | What it does |
|-------|-------------|
| 2-5 | Maven 3.9.9, local cache path at %USERPROFILE%\.m2\wrapper\dists |
| 7-13 | If no JAVA_HOME, searches PATH for java.exe; error if missing |
| 15-28 | If Maven binary missing locally, calls download-maven.ps1 to download and extract; exits if fails |
| 30 | Delegates to mvn.cmd with all args |

### 2.6 .mvn/wrapper/download-maven.ps1 (39 lines)

| Lines | What it does |
|-------|-------------|
| 1-6 | Params: $MavenVersion, $MavenHome; ErrorActionPreference=Stop |
| 8-9 | Download URL and temp ZIP path |
| 11 | Enables TLS 1.2 |
| 15 | Downloads ZIP via Invoke-WebRequest |
| 17-19 | Creates destination dir if needed |
| 22 | Extracts ZIP to MavenHome via Expand-Archive |
| 24 | Deletes ZIP |
| 26-29 | Verifies mvn.cmd exists at expected path |
| 32,35-38 | Success/error exit messages |

### 2.7 .mvn/wrapper/maven-wrapper.properties (2 lines)

distributionUrl -> apache-maven-3.9.9-bin.zip, wrapperUrl -> maven-wrapper-3.3.2.jar

### 2.8 .vscode/settings.json (4 lines)

java.compile.nullAnalysis.mode: "automatic", java.configuration.updateBuildConfiguration: "interactive"

## 3. Entry Point

### 3.1 RescueScanApplication.java (11 lines)

| Lines | What it does |
|-------|-------------|
| 1 | Package com.rescuescan |
| 3-4 | Imports: SpringApplication, SpringBootApplication |
| 6 | @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan("com.rescuescan") |
| 7 | Class declaration |
| 8-10 | SpringApplication.run() — boots Tomcat, loads properties, connects DB, runs DDL, starts on port 8080 |

## 4. Model Layer (Entities)

### 4.1 User.java (79 lines) — Table: users

| Lines | What it does |
|-------|-------------|
| 1 | Package com.rescuescan.model |
| 3-5 | Imports: jakarta.persistence.*, ArrayList, List |
| 7 | @Entity |
| 8 | @Table(name = "users") |
| 10-13 | Long id: @Id @GeneratedValue(IDENTITY) — auto-increment PK |
| 15-16 | String username: @Column(nullable=false, unique=true) |
| 18-19 | String password: @Column(nullable=false) — BCrypt hash stored here |
| 21-22 | String qrCode: @Column(nullable=false, unique=true) — 12-char uppercase UUID |
| 24-25 | boolean profileCompleted: @Column(name="profile_completed"), default=false |
| 27-28 | List<EmergencyContact> contacts: @OneToMany(mappedBy="user", cascade=ALL, orphanRemoval=true) — initialized as empty ArrayList |
| 30 | No-arg constructor (JPA requirement) |
| 32-78 | Getters/setters for all 5 fields |

**Key**: qrCode is random 12-char UUID. profileCompleted gates dashboard access. Cascade ALL + orphanRemoval allows delete-and-reinsert pattern in contact service.

### 4.2 EmergencyContact.java (67 lines) — Table: emergency_contacts

| Lines | What it does |
|-------|-------------|
| 1 | Package com.rescuescan.model |
| 3 | import jakarta.persistence.* |
| 5-6 | @Entity @Table(name = "emergency_contacts") |
| 9-11 | Long id: auto-increment PK |
| 13-14 | String contactName: @Column(name="contact_name", nullable=false) |
| 16-17 | String contactPhone: @Column(name="contact_phone", nullable=false) — sentinel "EMERGENCY" used for emergency services |
| 19-20 | String relationship: @Column(name="relationship"), nullable |
| 22-24 | User user: @ManyToOne(fetch=LAZY) @JoinColumn(name="user_id", nullable=false) — owning side of relationship |
| 26 | No-arg constructor |
| 28-66 | Getters/setters |

**Key**: phone="EMERGENCY" sentinel — public-info.html filters contacts by this value to separate close contacts from emergency services.

## 5. Repository Layer (Data Access)

### 5.1 UserRepository.java (11 lines)

Interface extends JpaRepository<User, Long>. Methods:
- Optional<User> findByUsername(String) — derived: SELECT * FROM users WHERE username = ?
- Optional<User> findByQrCode(String) — derived: SELECT * FROM users WHERE qr_code = ?
- boolean existsByUsername(String) — derived: SELECT COUNT(*) > 0 FROM users WHERE username = ?

### 5.2 EmergencyContactRepository.java (11 lines)

Interface extends JpaRepository<EmergencyContact, Long>. Methods:
- List<EmergencyContact> findByUser(User) — derived: SELECT * FROM emergency_contacts WHERE user_id = ?
- void deleteByUser(User) — derived: DELETE FROM emergency_contacts WHERE user_id = ?

## 6. Service Layer (Business Logic)

### 6.1 UserService.java (48 lines)

| Lines | What it does |
|-------|-------------|
| 1-8 | Imports: UUID, PasswordEncoder |
| 9 | @Service |
| 12-13 | Private final fields: UserRepository, PasswordEncoder |
| 15-18 | Constructor injection (Spring auto-wires both) |
| 20-32 | registerUser(username, password): checks existsByUsername -> throws if taken. Creates User: sets username, BCrypt-encodes password, generates UUID.toString().substring(0,12).toUpperCase() for qrCode, profileCompleted=false, saves via repository, returns saved User |
| 34-37 | findByUsername: delegates to repository, throws IllegalArgumentException("User not found") if absent |
| 39-42 | findByQrCode: delegates to repository, throws IllegalArgumentException("Invalid QR code") if absent |
| 44-47 | completeProfile: sets profileCompleted=true, saves, returns |

### 6.2 EmergencyContactService.java (38 lines)

| Lines | What it does |
|-------|-------------|
| 1-8 | Imports: @Transactional, List |
| 10 | @Service |
| 13 | Private final EmergencyContactRepository contactRepository |
| 15-17 | Constructor injection |
| 19-33 | @Transactional saveContacts(user, names, phones, relationships): deletes all existing contacts for user via deleteByUser. Loops parallel lists (equal size), for each non-empty name creates EmergencyContact, sets trimmed values, saves. Empty names skipped |
| 35-37 | getContactsByUser: returns contactRepository.findByUser(user) |

**Key**: Takes parallel lists (not objects) because HTML forms with repeated name="contactName" submit as List<String>.

### 6.3 QrCodeService.java (39 lines)

| Lines | What it does |
|-------|-------------|
| 1-13 | Imports: ZXing classes, ByteArrayOutputStream |
| 15 | @Service |
| 18-19 | Constants: WIDTH=300, HEIGHT=300 |
| 21-34 | generateQrCode(content): instantiates QRCodeWriter. Sets hints: UTF-8 charset, error correction H (highest, ~30% redundancy). Encodes to BitMatrix (QR_CODE, 300x300). Writes to ByteArrayOutputStream as PNG via MatrixToImageWriter. Returns byte[] |
| 36-38 | getQrCodeUrl(qrCode): returns "/qr/" + qrCode |

### 6.4 CustomUserDetailsService.java (33 lines)

| Lines | What it does |
|-------|-------------|
| 1-11 | Imports: UserDetailsService, SimpleGrantedAuthority, UsernameNotFoundException |
| 13 | @Service |
| 14 | Implements UserDetailsService |
| 16-20 | Private final UserRepository, constructor injection |
| 22-32 | loadUserByUsername(username): fetches User from repository, throws UsernameNotFoundException if missing. Returns Spring Security User with username, BCrypt password, Single granted authority "ROLE_USER". DaoAuthenticationProvider uses this to verify credentials |

## 7. Security Configuration

### 7.1 SecurityConfig.java (71 lines)

| Lines | What it does |
|-------|-------------|
| 1-14 | Imports: SecurityFilterChain, HttpSecurity, DaoAuthenticationProvider, ProviderManager, BCryptPasswordEncoder |
| 16 | @Configuration |
| 17 | @EnableWebSecurity |
| 20-24 | Private final CustomUserDetailsService, constructor injection |
| 26-29 | @Bean PasswordEncoder: returns new BCryptPasswordEncoder() — 10 rounds default |
| 31-34 | @Bean UserDetailsService: returns CustomUserDetailsService instance |
| 36-42 | @Bean AuthenticationManager: creates DaoAuthenticationProvider, sets userDetailsService + passwordEncoder, wraps in ProviderManager |
| 44-70 | @Bean SecurityFilterChain(http): |
| 47-51 | Authorization: permitAll for "/", "/register", "/login", "/qr/public/**", "/css/**", "/js/**", "/h2-console/**". All other requests require authentication |
| 52-56 | FormLogin: loginPage="/login", defaultSuccessUrl="/dashboard" (always=true), permitAll |
| 57-61 | Logout: logoutUrl="/logout", logoutSuccessUrl="/login?logout", permitAll |
| 62-64 | CSRF: ignoringRequestMatchers("/h2-console/**") — H2 console uses frames that conflict with CSRF |
| 65-67 | Headers: frameOptions.sameOrigin() — allows H2 console frames |
| 69 | http.build() |

**Summary**: Public endpoints (login, register, qr/public) are unauthenticated. All /dashboard/* routes require login. Static files always accessible.

## 8. Controller Layer

### 8.1 AuthController.java (60 lines)

Base: none (root mappings). Purpose: unauthenticated routes.

| Lines | What it does |
|-------|-------------|
| 1-10 | Imports: @Controller, @GetMapping, @PostMapping, @RequestParam, RedirectAttributes |
| 12 | @Controller |
| 15-19 | Private final UserService, constructor injection |
| 21-24 | GET / -> redirect:/login |
| 26-29 | GET /login -> "login" (login.html). POST /login handled by Spring Security |
| 31-34 | GET /register -> "register" (register.html) |
| 36-59 | POST /register(username, password, confirmPassword, RedirectAttributes): |
| 41-44 | If passwords don't match -> flash error "Passwords do not match", redirect:/register |
| 46-49 | If password length < 4 -> flash error, redirect:/register |
| 51-58 | Try userService.registerUser(). Success -> flash success, redirect:/login. IllegalArgumentException caught -> flash error (e.g. "Username already exists"), redirect:/register |

### 8.2 DashboardController.java (182 lines)

Base: /dashboard. Purpose: all authenticated user functionality.

| Lines | What it does |
|-------|-------------|
| 1-19 | Imports: WriterException, Authentication, @RequestMapping, @ResponseBody, MediaType, ArrayList |
| 21-22 | @Controller @RequestMapping("/dashboard") |
| 25-28 | Private final: UserService, EmergencyContactService, QrCodeService, UserRepository |
| 30-38 | Constructor injection |
| 40-52 | GET /dashboard: gets auth username -> userService.findByUsername(). If !profileCompleted -> redirect:/dashboard/setup. Adds user, contacts, qrCodeUrl to model. Returns "dashboard" |
| 54-60 | GET /setup: loads user, adds to model. Returns "setup" |
| 62-98 | POST /setup/preview(contactName[], contactPhone[], relationship[]): validates at least one non-empty name. Builds temporary EmergencyContact list (not saved). Adds user + preview contacts to model. Returns "confirm-setup" |
| 100-113 | POST /setup/confirm(contactName[], contactPhone[], relationship[]): contactService.saveContacts() persists to DB. userService.completeProfile() sets profileCompleted=true. Flash success. Redirect:/dashboard/qr |
| 115-125 | GET /qr: loads user + contacts. Adds qrCodeUrl, offlineQrCodeUrl="/dashboard/qr/offline-image", offlineText (from buildEmergencyInfoText). Returns "qr-code" |
| 127-133 | GET /qr/image @ResponseBody: builds URL "http://localhost:8080/qr/public/{qrCode}". Calls qrCodeService.generateQrCode(url). Returns byte[] PNG |
| 135-142 | GET /qr/offline-image @ResponseBody: builds offline text via buildEmergencyInfoText(). Calls qrCodeService.generateQrCode(text). Returns byte[] PNG |
| 144-161 | private buildEmergencyInfoText(user, contacts): builds "EMERGENCY: username\n---\n1. Name | Phone | Relationship\n2. ..." multiline string |
| 163-169 | GET /contacts: loads user + contacts. Returns "contacts" |
| 171-181 | POST /contacts/update(contactName[], contactPhone[], relationship[]): contactService.saveContacts(). Flash success. Redirect:/dashboard/contacts |

**Key**: Online QR encodes URL -> scanner opens public-info.html. Offline QR encodes raw text -> scanner displays text directly (no internet). buildEmergencyInfoText used for both offline QR image and page display.

### 8.3 PublicQrController.java (36 lines)

Base: /qr/public. Purpose: public emergency info endpoint (no auth).

| Lines | What it does |
|-------|-------------|
| 15 | @Controller |
| 16 | @RequestMapping("/qr/public") |
| 19-25 | Private final UserService + EmergencyContactService, constructor injection |
| 27-35 | GET /{qrCode}: userService.findByQrCode(qrCode) looks up user. contactService.getContactsByUser(user) loads contacts. Adds to model. Returns "public-info" |

**This is the first responder endpoint** — displays contacts with click-to-call tel: links, no login required.

## 9. View Layer (Thymeleaf Templates)

### 9.1 login.html (48 lines)

Rendered by: AuthController.login(). HTML5 + Thymeleaf namespace. Links style.css. Body class="auth-page". Centered card with:
- Header: "RescueScan" / "Emergency QR Code System"
- param.error -> "Invalid username or password" (Spring Security failed login)
- param.logout -> "You have been logged out"
- Flash ${success} and ${error} from register redirect
- Form POST to /login (Spring Security), fields: username + password
- Footer link to /register

### 9.2 register.html (45 lines)

Rendered by: AuthController.registerForm(). Body class="auth-page". Flash messages ${error} ${success}. Form POST to /register, fields: username (minlength=3), password (minlength=4), confirmPassword. Footer link to /login.

### 9.3 dashboard.html (94 lines)

Rendered by: DashboardController.dashboard().
- Navbar: brand link -> /dashboard, ${user.username}, logout POST form
- Success flash message
- 2-column grid: QR Card (image from /dashboard/qr/image, qrCode string, View & Print -> /dashboard/qr, Edit Contacts -> /dashboard/contacts) + Contacts Card (th:each contacts, shows name/relationship/phone, empty state with Add Contacts link)
- "How It Works" info card: 3 steps in 3-column grid
- Script: /js/main.js

### 9.4 setup.html (114 lines)

Rendered by: DashboardController.setupForm().
- Navbar, welcome header, note about preview step
- Form POST to /dashboard/setup/preview
- 3 contact entry blocks (Contact 1 required, 2+3 optional), each with name="contactName[]", name="contactPhone[]", name="relationship[]"
- Emergency number section: input name="contactName" (the number), hidden name="contactPhone" value="EMERGENCY", hidden name="relationship" value="Emergency Services" — reuses contact structure with sentinel
- Submit: "Preview & Confirm"

### 9.5 confirm-setup.html (69 lines)

Rendered by: DashboardController.setupPreview().
- Header: "Confirm Your Information", warning about changes requiring reprint
- Contact review: numbered badges (gradient circles), name, phone, relationship
- Warning box (amber): changing contacts requires reprinting QR
- Confirm form (POST to /dashboard/setup/confirm): hidden inputs th:each for each contact's name/phone/relationship — recreates parallel list structure
- "Go Back & Edit" button (GET to /dashboard/setup)

### 9.6 qr-code.html (56 lines)

Rendered by: DashboardController.viewQrCode().
- Dual QR display: OFFLINE QR (image from /dashboard/qr/offline-image, "Works without internet") + ONLINE QR (image from /dashboard/qr/image, "Full details online")
- Print button calls window.print()
- @media print CSS hides navbar, buttons, .no-print

### 9.7 contacts.html (81 lines)

Rendered by: DashboardController.manageContacts().
- Header: "Manage Emergency Contacts" + Back button
- Form POST to /dashboard/contacts/update
- Existing contacts: pre-populated fields via th:each with iterStat
- Empty slots: ${#numbers.sequence(#lists.size(contacts), 2)} — fills up to 3 contact slots
- "Save Changes" button

### 9.8 public-info.html (55 lines)

Rendered by: PublicQrController.viewEmergencyInfo(). NO AUTH REQUIRED.
- Body class="public-page" (different styling, no navbar)
- "Emergency Contact" badge (gradient pill)
- Close Contacts: th:each contacts, filtered by phone != "EMERGENCY", shows avatar (first letter), name, relationship, clickable tel: link, green Call button
- Emergency Services: filtered by phone == "EMERGENCY", shows number as clickable tel: link, red gradient card
- Footer: "Powered by RescueScan", disclaimer

## 10. Static Assets

### 10.1 style.css (1274 lines)

| Section | Lines | What it does |
|---------|-------|-------------|
| Font import | 1 | Google Fonts Inter (300-800) |
| CSS variables | 3-33 | Design system: red primary (#e11d48), semantic colors, shadows, radii, transitions |
| Reset | 35-52 | box-sizing, font, background, antialiasing |
| Container | 54-58 | max-width 1120px |
| Navbar | 61-112 | Glassmorphism (backdrop-filter blur), brand with gradient ::before logo, user badge, logout button |
| Auth pages | 115-209 | Full-viewport centered, decorative gradient blobs ::before/::after, card with shadow |
| Forms | 212-261 | Inputs with focus ring (red box-shadow), .form-row grid |
| Buttons | 264-358 | .btn-primary (red gradient), .btn-secondary (light gray), .btn-ghost, .btn-logout (red on hover), .btn-call (green gradient), sizes (.full, .large, .sm) |
| Alerts | 361-398 | .alert-success (green) and .alert-error (red) with ::before dot |
| Dashboard | 401-496 | 2-column grid, cards with hover shadow, QR display centered |
| Contacts list | 498-545 | Flex items, hover highlight, empty state |
| Info card | 548-597 | How It Works 3-column grid with numbered circles |
| Setup page | 600-684 | Centered card form, contact entries |
| Confirmation | 687-769 | Review items with numbered badges, warning box |
| QR print | 772-901 | Large print card, dual QR, offline text |
| Public info | 904-1103 | Red gradient background, avatar circles, emergency services, disclaimer |
| Contacts mgmt | 1106-1143 | Edit form with pre-populated fields |
| @media print | 1146-1171 | Hides navbar, buttons, .no-print; black border on QR card |
| Responsive | 1174-1238 | 900px: single column; 640px: smaller padding, hidden nav-user, full-width buttons |
| Animations | 1241-1274 | fadeIn keyframe on all cards; pulse on disabled buttons |

### 10.2 main.js (20 lines)

| Lines | What it does |
|-------|-------------|
| 1 | DOMContentLoaded listener |
| 2 | Selects all <form> elements |
| 4-11 | On each form submit: disables all submit buttons, changes text to "Processing..." — prevents double-submit |
| 14 | Selects all <input type="tel"> |
| 15-19 | On input: strips non-digit/+/-(/)/space chars via regex /[^\d+\-() ]/g — sanitizes phone input |

## 11. Auxiliary Files

### 11.1 run.ps1 (gitignored) — Custom PowerShell run script, not in version control.

### 11.2 data/rescuescan.mv.db (gitignored) — H2 database file for local development.

## 12. Entity Relationship

```
User (1) ----< (N) EmergencyContact
  |                       |
  |-- id (PK)             |-- id (PK)
  |-- username (unique)    |-- contactName
  |-- password (BCrypt)    |-- contactPhone
  |-- qrCode (unique)      |-- relationship
  |-- profileCompleted     |-- user_id (FK -> User.id)
```

## 13. Request Flow Walkthroughs

### 13.1 Registration

GET /register -> register.html. User fills form. POST /register (username, password, confirmPassword). AuthController validates passwords match and length >= 4. UserService.registerUser() checks existsByUsername (throws if taken), creates User with BCrypt password + 12-char UUID qrCode + profileCompleted=false, saves. Redirect to /login with success flash.

### 13.2 Login

POST /login. Spring Security filter intercepts. Delegates to AuthenticationManager -> DaoAuthenticationProvider -> CustomUserDetailsService.loadUserByUsername() fetches User from DB, returns Spring Security User with BCrypt hash + ROLE_USER. BCryptPasswordEncoder.matches() verifies password. On success, creates SecurityContext, redirects to /dashboard.

### 13.3 First Login Setup

GET /dashboard -> profileCompleted=false -> redirect to /dashboard/setup. User fills contacts + emergency number. POST /dashboard/setup/preview -> validates at least one contact, builds preview list (not saved), shows confirm-setup.html. POST /dashboard/setup/confirm -> contactService.saveContacts() deletes old contacts (if any), inserts new ones. userService.completeProfile() sets profileCompleted=true. Redirect to /dashboard/qr.

### 13.4 QR Generation

GET /dashboard/qr -> loads user + contacts, displays dual QR: online (encodes URL http://host/qr/public/{code}) and offline (encodes raw text). Images served by /dashboard/qr/image and /dashboard/qr/offline-image, both @ResponseBody byte[] PNG.

### 13.5 Public QR Scan (Emergency)

Scanner opens URL from online QR: GET /qr/public/{qrCode}. PublicQrController looks up user by qrCode, loads contacts, renders public-info.html. Close contacts shown with tel: call links. Emergency services (phone="EMERGENCY") shown separately with big call button. Offline QR: phone decodes text directly, no internet needed.

### 13.6 Contact Editing

GET /dashboard/contacts -> shows pre-populated form + empty slots. POST /dashboard/contacts/update -> saveContacts() deletes all, re-inserts. Redirect with success flash.

### 13.7 Logout

POST /logout. Spring Security invalidates session, clears SecurityContext, redirects to /login?logout.

## 14. Complete Dependency Graph

```
RescueScanApplication.java
  -> application.properties
  -> Component Scan ->
       SecurityConfig -> CustomUserDetailsService -> UserRepository -> User (entity)
       AuthController -> UserService -> UserRepository + PasswordEncoder -> User
       DashboardController -> UserService + EmergencyContactService + QrCodeService + UserRepository
                              -> EmergencyContactRepository -> EmergencyContact (entity)
       PublicQrController -> UserService + EmergencyContactService
  -> Templates: login, register, dashboard, setup, confirm-setup, qr-code, contacts, public-info
  -> Static: style.css, main.js
```

## 15. Key Patterns Summary

| Pattern | Where | Description |
|---------|-------|-------------|
| Constructor Injection | All services, controllers, config | Spring auto-wires via constructor params (no @Autowired) |
| Parallel Lists | setup.html, contacts.html, confirm-setup.html | Repeated name="contactName" inputs bind to List<String> in controller |
| Sentinel Value | phone="EMERGENCY" | Country emergency number stored as contact with special phone, filtered in public-info.html |
| Gateway | profileCompleted flag | Blocks dashboard access until profile setup is done |
| Delete-and-Reinsert | EmergencyContactService.saveContacts() | Deletes all old contacts, re-inserts in @Transactional block |
| Dual QR Mode | /dashboard/qr/image vs /dashboard/qr/offline-image | Same encoder, different content (URL vs text) |
| Flash Attributes | All controllers | RedirectAttributes passes messages across one redirect |
| @ResponseBody byte[] | DashboardController.getQrImage() | Returns raw PNG, Spring Boot auto-sets image/png Content-Type |
