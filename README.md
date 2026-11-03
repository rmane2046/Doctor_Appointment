# DocBook - Doctor Appointment Booking System

A full-stack web application where patients search doctors and book appointment slots, doctors manage their
appointments, and an admin manages the whole system.

**Backend:** Java 17, Spring Boot 3.3, Spring Security + JWT, Spring Data JPA (Hibernate), H2 / MySQL
**Frontend:** HTML, CSS, vanilla JavaScript (fetch API) - served by the same Spring Boot app
**Build tool:** Maven

---

## Features

| Role    | What they can do |
|---------|------------------|
| Visitor | Browse doctors, filter by specialization, search by name, see free time slots |
| Patient | Register / login, book a slot, view own appointments, cancel an appointment |
| Doctor  | Login, see own appointments with patient details, confirm / complete / cancel |
| Admin   | Dashboard statistics, view all appointments and users, add new doctors, cancel any appointment |

Business rules enforced in the backend:
- A slot can only be booked once (cancelled slots become free again)
- No booking in the past, and only on valid slot boundaries inside the doctor's working hours
- Only the owner (patient / doctor) or an admin can cancel; only the doctor or admin can confirm / complete
- Completed or cancelled appointments cannot be changed again
- Passwords stored as BCrypt hashes; JWT tokens expire after 24 hours

---

## How to run

### Requirements
- JDK 17 or newer (JDK 21 works)
- Maven 3.8+ (or open the folder in IntelliJ IDEA / Eclipse / STS and let the IDE import the Maven project)

### Option 1 - H2 in-memory database (no setup, recommended for first run)
```bash
mvn spring-boot:run
```
Open **http://localhost:8080** in the browser.
Data is re-created with demo records every time the app starts.

### Option 2 - MySQL (data is kept between restarts)
1. Install MySQL and make sure it is running.
2. Edit username / password in `src/main/resources/application-mysql.properties`.
3. Run:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
The `docapp` database and all tables are created automatically.

### Option 3 - Oracle Database
1. Create a user (run as SYS / SYSTEM in SQL Developer; for XE 18c+ first run `ALTER SESSION SET CONTAINER = XEPDB1;`):
```sql
CREATE USER docapp IDENTIFIED BY docapp123;
GRANT CONNECT, RESOURCE TO docapp;
GRANT UNLIMITED TABLESPACE TO docapp;
```
2. Check the URL / user / password in `src/main/resources/application-oracle.properties`.
3. Run with the profile: `mvn spring-boot:run -Dspring-boot.run.profiles=oracle`
   (in Eclipse: add `--spring.profiles.active=oracle` as a program argument).

### Build a runnable jar
```bash
mvn clean package
java -jar target/doctor-appointment-1.0.0.jar
```
(add `-DskipTests` to `mvn package` to skip the start-up test)

---

## Demo accounts (created automatically)

| Role    | Email                       | Password    |
|---------|-----------------------------|-------------|
| Admin   | admin@docapp.com            | Admin@123   |
| Patient | patient@docapp.com          | Patient@123 |
| Doctor  | anjali.sharma@docapp.com    | Doctor@123  |
| Doctor  | rahul.verma@docapp.com      | Doctor@123  |
| Doctor  | priya.nair@docapp.com       | Doctor@123  |
| Doctor  | amit.deshmukh@docapp.com    | Doctor@123  |
| Doctor  | sneha.kulkarni@docapp.com   | Doctor@123  |
| Doctor  | vikram.patil@docapp.com     | Doctor@123  |

---

## Project structure

```
src/main/java/com/docapp
|-- DoctorAppointmentApplication.java   entry point (main method)
|-- config/DataSeeder.java              inserts demo data at start-up
|-- model/                              JPA entities: User, Doctor, Appointment + enums Role, AppointmentStatus
|-- repository/                         Spring Data JPA interfaces (database access)
|-- dto/                                request / response objects (what goes over the API)
|-- service/                            business logic: Auth, Doctor (slots), Appointment, Admin
|-- controller/                         REST endpoints: Auth, Doctor, Appointment, Admin
|-- security/                           JwtService, JwtAuthFilter, SecurityConfig
`-- exception/                          ApiException + GlobalExceptionHandler (JSON error responses)

src/main/resources
|-- application.properties              default config (H2)
|-- application-mysql.properties        MySQL profile
`-- static/                             frontend: index, login, register, my-appointments, doctor, admin (.html) + css/ + js/
```

Layered architecture: **Controller -> Service -> Repository -> Database**.
Controllers only handle HTTP, services hold the rules, repositories talk to the database.

---

## REST API

| Method | URL | Who | Description |
|--------|-----|-----|-------------|
| POST | `/api/auth/register` | public | Register a patient, returns JWT + user |
| POST | `/api/auth/login` | public | Login, returns JWT + user |
| GET | `/api/doctors?specialization=` | public | List doctors (optional filter) |
| GET | `/api/doctors/specializations` | public | Distinct specializations |
| GET | `/api/doctors/{id}` | public | One doctor |
| GET | `/api/doctors/{id}/slots?date=yyyy-MM-dd` | public | Slots of that day with availability |
| POST | `/api/appointments` | PATIENT | Book `{doctorId, date, time, reason}` |
| GET | `/api/appointments/my` | PATIENT | My appointments |
| GET | `/api/appointments/doctor` | DOCTOR | Appointments of the logged-in doctor |
| PATCH | `/api/appointments/{id}/cancel` | PATIENT / DOCTOR / ADMIN | Cancel (ownership checked) |
| PATCH | `/api/appointments/{id}/status` | DOCTOR / ADMIN | Body `{status: "CONFIRMED" \| "COMPLETED" \| "CANCELLED"}` |
| POST | `/api/admin/doctors` | ADMIN | Create doctor account + profile |
| GET | `/api/admin/appointments` | ADMIN | All appointments |
| GET | `/api/admin/users` | ADMIN | All users |
| GET | `/api/admin/stats` | ADMIN | Counts for the dashboard |

Protected endpoints need the header `Authorization: Bearer <token>`.
Errors always look like `{"error": "message"}`.

### Try it with curl
```bash
# login
curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
     -d '{"email":"patient@docapp.com","password":"Patient@123"}'

# slots of doctor 1 on a date
curl -s "localhost:8080/api/doctors/1/slots?date=2026-11-10"

# book (replace TOKEN)
curl -s -X POST localhost:8080/api/appointments -H "Content-Type: application/json" \
     -H "Authorization: Bearer TOKEN" \
     -d '{"doctorId":1,"date":"2026-11-10","time":"11:00","reason":"Check-up"}'
```

---

## Known limitations / possible improvements
- Two people booking the exact same slot at the same millisecond could both pass the "is it taken" check.
  Fix: a unique constraint on (doctor, date, time) or optimistic locking.
- Doctors work the same hours every day (no weekly schedule or leave days yet).
- No email / SMS notifications, no payment, no pagination.
- JWT is stored in localStorage (simple); production apps often use httpOnly cookies + refresh tokens.
- Only a context-load test exists; service-layer unit tests (Mockito) and controller tests (MockMvc) can be added.
