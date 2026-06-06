# RescueScan — Emergency QR Code System

Create a unique QR code with your emergency contacts. First responders scan it to call your loved ones instantly.

## Tech Stack

Java 17, Spring Boot 3.2.0, Spring MVC, Spring Data JPA, Spring Security, Thymeleaf, ZXing, SQL Server / H2

## Quick Start (H2 — no database setup required)

1. **Replace** `src/main/resources/application.properties` with:
```properties
spring.application.name=RescueScan
spring.datasource.url=jdbc:h2:file:./data/rescuescan
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.thymeleaf.cache=false
server.port=8080
```

2. **Run**:
```bash
mvnw spring-boot:run     # Windows
./mvnw spring-boot:run   # Mac / Linux
```

3. Open **http://localhost:8080**

## SQL Server Setup

1. Install SQL Server (LocalDB, Developer Edition, or Docker)
2. Create database `RescueScanDB` and user `rescuescant` / `rescuescanadmin123`
3. Run the app with the existing `application.properties` (already configured for SQL Server)

## How to Use

1. **Register** an account
2. **Add emergency contacts** — names, phone numbers, and relationships
3. **Set an emergency number** (911, 112, 999, etc.)
4. **Print your QR code** — the page shows two QR codes:
   - **Online QR**: points to a URL with live contact info
   - **Offline QR**: embeds contact text directly (works without internet)
5. **Keep it with you** — wallet, phone case, or ID badge

## Making the Online QR Code Accessible

The online QR encodes `http://localhost:8080/qr/public/{CODE}`. For it to work outside your machine:
- **ngrok**: `ngrok http 8080` gives a public URL you can share
- **Port forwarding**: forward port 8080 on your router to your machine
- **Deploy**: host on a VPS or cloud platform

## Project Structure

```
src/
  main/
    java/com/rescuescan/
      RescueScanApplication.java    — Entry point
      config/SecurityConfig.java    — Spring Security
      controller/                   — AuthController, DashboardController, PublicQrController
      model/                        — User.java, EmergencyContact.java (JPA entities)
      repository/                   — UserRepository, EmergencyContactRepository
      service/                      — UserService, EmergencyContactService, QrCodeService, CustomUserDetailsService
    resources/
      application.properties        — DB and app config
      templates/                    — 8 Thymeleaf HTML templates
      static/css/style.css          — All styles
      static/js/main.js             — Form handling
pom.xml                             — Maven build
```

## Security Warning

The default `application.properties` contains hardcoded database credentials (`rescuescant` / `rescuescanadmin123`). **Do not make this repo public without removing them first.** Use environment variables or a separate config file instead.
