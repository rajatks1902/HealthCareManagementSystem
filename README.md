# Healthcare Management System

Healthcare Management System is a Dockerized microservices project for authentication, doctor management, patient management, appointment scheduling, Kafka notifications, Cassandra rollups, and an Angular operations UI.

This file is the main project guide. More detailed backend/API validation notes are also available in:

- `docs/PROJECT_WORKING_AND_CONFIGS.md`
- `docs/USECASE_TEST_REPORT.md`
- `healthcare-management-ui/README.md`

## Tech Stack

| Area | Technology |
| --- | --- |
| Backend services | Java 17, Spring Boot, Spring Web, Spring Validation, Spring Actuator |
| Gateway | Spring Cloud Gateway, Reactor, Resilience4j circuit breakers, JWT validation |
| Auth | Spring Security, BCrypt password hashing, JJWT `0.11.5` |
| Data | Apache Cassandra `4.1`, Spring Data Cassandra |
| Messaging | Apache Kafka `3.9.0`, Spring Kafka |
| Notifications | Spring Mail / JavaMail over SMTP |
| Frontend | Angular 22, standalone components, strict TypeScript, Angular Router, reactive forms, Angular signals |
| UI assets | Lucide icons, native CSS |
| Containers | Docker Compose, service Dockerfiles, Maven container builds, Nginx for UI production image |
| Testing | Maven/Surefire, Angular build, Playwright browser use-case test |

## Services

| Service | Folder | Responsibility |
| --- | --- | --- |
| API Gateway | `api-gateway` | Public entry point on port `8080`, JWT validation, routing, retries, circuit breaker fallbacks |
| Auth Security | `auth-security` | Signup, signin, JWT issuing, user/role persistence |
| Doctor Info | `doctor-info` | Doctor CRUD, availability, rating and specialty data |
| Patient Info | `patient-info` | Patient creation and lookup |
| Appointment Services | `appointment-services` | Appointment create/list/update, Kafka event production, daily/weekly stats rollups |
| Notification Services | `notification-services` | Kafka consumer for appointment events and SMTP email attempts |
| Angular UI | `healthcare-management-ui` | CareOps browser UI for auth, dashboard, doctors, patients, appointments, and stats |
| Cassandra | Docker service | Database for auth, doctor, patient, appointment, and stats data |
| Kafka | Docker service | Event broker for appointment notification events |
| Cassandra Init | Docker service | Runs `cassandra/schema.cql` once and exits with status `0` |

## Runtime Flow

1. Browser or API client calls the API Gateway at `http://127.0.0.1:8080`.
2. `/api/auth/signup` and `/api/auth/signin` are public.
3. Gateway requires `Authorization: Bearer <jwt>` for doctor, patient, and appointment routes.
4. Gateway forwards requests to service containers on the Docker network.
5. Services persist data in Cassandra keyspaces created by `cassandra/schema.cql`.
6. Appointment create/update publishes JSON events to Kafka topic `appointments`.
7. Notification service consumes Kafka events and attempts SMTP delivery.
8. Appointment stats are rebuilt manually or by scheduler into Cassandra rollup rows.

## Prerequisites

Verified locally on 2026-07-12 with:

| Tool | Verified Version |
| --- | --- |
| Java | Temurin `17.0.9` |
| Docker Desktop | `4.39.0` |
| Docker Engine | `28.0.1` |
| Node.js | `v26.0.0` |
| npm | `11.12.1` |

You need Docker Desktop running before backend services or Cassandra-backed tests will work.

## Start The Backend

From the project root:

```bash
docker compose up -d --build
```

Verify containers:

```bash
docker compose ps
```

Verify gateway health:

```bash
curl -i http://127.0.0.1:8080/actuator/health
```

Expected health response:

```json
{"status":"UP"}
```

Backend URLs:

| URL | Purpose |
| --- | --- |
| `http://127.0.0.1:8080` | API Gateway |
| `http://127.0.0.1:8080/actuator/health` | Gateway health |
| `127.0.0.1:9042` | Cassandra |
| `127.0.0.1:9092` | Kafka |

The individual backend services are not exposed as host ports. They run inside the Docker network on container port `8080`.

## Start The UI

The Angular UI is not part of the root `docker-compose.yml`, so run it separately for local development.

```bash
cd healthcare-management-ui
npm ci
npm start
```

Open:

```text
http://localhost:4200
```

The Angular dev server uses `healthcare-management-ui/proxy.conf.json`:

| Browser Path | Proxied To |
| --- | --- |
| `/api/**` | `http://127.0.0.1:8080/api/**` |
| `/actuator/**` | `http://127.0.0.1:8080/actuator/**` |

On this machine, `ng serve` bound to `localhost:4200`; `127.0.0.1:4200` did not respond. Use `http://localhost:4200` for the UI and set `UI_BASE_URL=http://localhost:4200` for the e2e script.

## Stop The Project

Stop containers but keep Cassandra data:

```bash
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to remove persisted Cassandra data.

## Configuration Keys

Docker Compose provides the main runtime configuration. Production deployments should replace development secrets and SMTP placeholders.

| Key | Used By | Purpose |
| --- | --- | --- |
| `GATEWAY_SERVICE_PORT` | API Gateway | Gateway HTTP port, defaults to `8080` |
| `JWT_SECRET` | API Gateway, Auth Security | Shared JWT signing/validation secret; must match in both services |
| `SPRING_CASSANDRA_CONTACT_POINTS` | Data services | Cassandra host, `cassandra` in Docker |
| `SPRING_CASSANDRA_PORT` | Data services | Cassandra port, `9042` |
| `SPRING_CASSANDRA_KEYSPACE_NAME` | Data services | Service keyspace: `security_info`, `doctor_info`, `patient_info`, or `appointment_info` |
| `SPRING_CASSANDRA_LOCAL_DATACENTER` | Data services | Cassandra local datacenter, `datacenter1` |
| `SPRING_CASSANDRA_USERNAME` | Data services | Cassandra username, configured as `cassandra` |
| `SPRING_CASSANDRA_PASSWORD` | Data services | Cassandra password, configured as `cassandra` |
| `SPRING_CASSANDRA_SCHEMA_ACTION` | Data services | Usually `NONE` in Docker because schema is owned by `cassandra/schema.cql` |
| `SERVER_PORT` | Doctor, Patient | Optional local service port override |
| `APPOINTMENT_SERVICE_PORT` | Appointment | Internal appointment service port; Docker sets it to `8080` for gateway routing |
| `APPOINTMENT_STATS_CRON` | Appointment | Stats scheduler cron, default `0 0 1 * * *` |
| `KAFKA_BOOTSTRAP_SERVER` | Appointment, Notification | Kafka broker, `kafka:9092` in Docker |
| `KAFKA_TOPIC` | Appointment, Notification | Appointment event topic, `appointments` |
| `KAFKA_GROUP_ID` | Notification | Kafka consumer group, `appointment-notifications-group` |
| `MAIL_SERVER_HOST` | Notification | SMTP host, default Compose value is `smtp.gmail.com` |
| `MAIL_SERVER_PORT` | Notification | SMTP port, default Compose value is `587` |
| `MAIL_SERVER_USERNAME` | Notification | SMTP username |
| `MAIL_SERVER_PASSWORD` | Notification | SMTP password/app password; Compose currently uses placeholder `password` |
| `UI_BASE_URL` | UI e2e test | Optional Playwright base URL override |
| `CHROME_PATH` | UI e2e test | Optional Chrome executable override |

## Cassandra Schema

The schema lives in `cassandra/schema.cql` and is idempotent. Compose runs it through `cassandra-init` after Cassandra accepts CQL connections.

| Keyspace | Main Tables |
| --- | --- |
| `security_info` | `roles`, `users` |
| `doctor_info` | `doctor_info`, `doctor_details` |
| `patient_info` | `patient_details` |
| `appointment_info` | `appointment_detail`, `doctor_appointments`, `appointment_stats` |

Seed roles:

- `ROLE_PATIENT`
- `ROLE_DOCTOR`
- `ROLE_ADMIN`

Stats rollups use `appointment_info.appointment_stats` with partition key `(period_type, period_start)` and clustering key `doctor_id`.

## Main API Routes

All API calls go through the gateway at `http://127.0.0.1:8080`.

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | Public | Register a user |
| `POST` | `/api/auth/signin` | Public | Sign in and receive JWT |
| `GET` | `/api/v1/doctor/all` | JWT | List doctors |
| `GET` | `/api/v1/doctor/{id}` | JWT | Get doctor by ID |
| `POST` | `/api/v1/doctor` | JWT | Create doctor |
| `PUT` | `/api/v1/doctor/{id}` | JWT | Update doctor |
| `DELETE` | `/api/v1/doctor/{id}` | JWT | Delete doctor |
| `GET` | `/api/v1/patient` | JWT | List patients |
| `GET` | `/api/v1/patient/{id}` | JWT | Get patient by ID |
| `POST` | `/api/v1/patient` | JWT | Create patient |
| `GET` | `/api/v1/appointments/all` | JWT | List appointments |
| `GET` | `/api/v1/appointments/doctor/{doctorId}` | JWT | List appointments for one doctor |
| `POST` | `/api/v1/appointments/create` | JWT | Create appointment and publish Kafka event |
| `PUT` | `/api/v1/appointments` | JWT | Update appointment and publish Kafka event |
| `GET` | `/api/v1/appointments/stats` | JWT | Read daily/weekly stats |
| `POST` | `/api/v1/appointments/stats/rebuild` | JWT | Rebuild stats rollups |

JWT-protected requests must include:

```http
Authorization: Bearer <jwt>
```

## Example Payloads

Signup:

```json
{
  "username": "u040740",
  "email": "u040740@example.com",
  "roles": ["patient"],
  "password": "Test@12345"
}
```

Signin:

```json
{
  "id": "u040740",
  "password": "Test@12345"
}
```

Create doctor:

```json
{
  "email": "dr.asha@example.com",
  "name": "Dr Asha Rao",
  "speciality": "Cardiology",
  "year_of_exp": 12,
  "rating": 4.8,
  "status": "AVAILABLE"
}
```

Doctor statuses:

- `AVAILABLE`
- `NOT_AVAILABLE`
- `DISABLED`

Create patient:

```json
{
  "patient_name": "Ravi Kumar",
  "email": "ravi@example.com",
  "age": "34"
}
```

Create appointment:

```json
{
  "doctorId": "c8f87a2a-b3f4-4c6a-b717-d052ee61d743",
  "patientId": "d678e60a-76fc-45a8-a361-af427ecc1f19",
  "appointmentTime": "2026-07-06T10:30:00",
  "status": "PENDING",
  "notes": "Routine cardiac consultation",
  "doctorComments": "Initial booking"
}
```

Appointment statuses:

- `PENDING`
- `CONFIRMED`
- `REJECTED`
- `COMPLETED`

Stats examples:

```http
GET /api/v1/appointments/stats?period=DAY&from=2026-07-06&to=2026-07-06&refresh=true
GET /api/v1/appointments/stats?period=WEEK&from=2026-07-06&to=2026-07-12&refresh=true
GET /api/v1/appointments/stats?period=DAY&doctorId=<doctorUuid>&refresh=true
```

## Testing And Verification

There is no root Maven parent project. Run backend tests from each service folder.

Backend tests:

```bash
cd api-gateway
./mvnw test
```

```bash
cd auth-security
./mvnw test
```

```bash
cd doctor-info
./mvnw test
```

```bash
cd patient-info
./mvnw test
```

```bash
cd appointment-services
./mvnw test
```

```bash
cd notification-services
./mvnw test
```

The Cassandra-backed Spring context tests require Cassandra on `127.0.0.1:9042`, so start Docker Compose before running `auth-security`, `doctor-info`, or `patient-info` tests.

UI validation:

```bash
cd healthcare-management-ui
npm ci
npm run build
```

For the browser use-case test, keep `npm start` running in one terminal:

```bash
cd healthcare-management-ui
npm start
```

Then run the e2e script in another terminal:

```bash
cd healthcare-management-ui
UI_BASE_URL=http://localhost:4200 npm run test:e2e
```

The e2e script creates disposable test data through the real UI and backend. It writes screenshots and a markdown report under `healthcare-management-ui/test-results/`.

## Verified Locally

The following commands were run successfully on 2026-07-12:

| Command | Result |
| --- | --- |
| `docker compose config --quiet` | Passed |
| `docker compose up -d --build` | Built/recreated services and started containers |
| `docker compose ps -a` | Cassandra, Kafka, gateway, auth, doctor, patient, appointment, and notification running; `cassandra-init` exited `0` |
| `curl -i http://127.0.0.1:8080/actuator/health` | Returned HTTP `200` and `{"status":"UP"}` |
| `./mvnw test` in `api-gateway` | Passed, 1 test |
| `./mvnw test` in `auth-security` | Passed, 1 test after Cassandra was running |
| `./mvnw test` in `doctor-info` | Passed, 1 test after Cassandra was running |
| `./mvnw test` in `patient-info` | Passed, 1 test after Cassandra was running |
| `./mvnw test` in `appointment-services` | Passed, 1 test |
| `./mvnw test` in `notification-services` | Build success, no test sources |
| `npm ci` in `healthcare-management-ui` | Installed dependencies; npm reported 3 low-severity advisories |
| `npm run build` in `healthcare-management-ui` | Passed; output in `dist/careops` |
| `UI_BASE_URL=http://localhost:4200 npm run test:e2e` | Passed 18 UI use cases |

## Known Runtime Notes

- `MAIL_SERVER_PASSWORD=password` is a placeholder. Kafka notification consumption works, but real email delivery requires valid SMTP credentials.
- Spring Boot Admin client warnings are expected because `admin-service` is configured but not included in `docker-compose.yml`.
- Cassandra driver warnings about missing auth challenge can appear because the app supplies Cassandra credentials while the default Cassandra container accepts the connection without an auth challenge.
- The first `docker compose up -d --build` can take time because container-stage Maven builds download dependencies.
- Keep the same `JWT_SECRET` between gateway and auth service. A mismatch will make issued tokens fail gateway validation.
- The Angular app uses relative `/api` calls and the Angular proxy. `healthcare-management-ui/.env.example` is not required for local Angular CLI development.
