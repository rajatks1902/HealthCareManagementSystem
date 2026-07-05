# Healthcare Management System - Project Working and Configs

This project is a Dockerized Spring Boot microservices platform for healthcare workflows: authentication, doctor records, patient records, appointment scheduling, Kafka-based notifications, and Cassandra-backed appointment statistics.

## Architecture

| Component | Purpose | Main Tech |
| --- | --- | --- |
| `api-gateway` | Single entry point, JWT validation, route forwarding, retry, circuit breakers, fallback handlers | Spring Cloud Gateway, Resilience4j, JWT |
| `auth-security` | Signup, signin, password hashing, JWT issuing, user/role persistence | Spring Boot, Spring Security, Cassandra |
| `doctor-info` | Doctor creation, lookup, listing, status/rating updates | Spring Boot, Cassandra |
| `patient-info` | Patient creation, lookup, listing | Spring Boot, Cassandra |
| `appointment-services` | Appointment create/list/update, Kafka event production, daily/weekly stats aggregation | Spring Boot, Cassandra, Kafka, Spring Scheduler |
| `notification-services` | Kafka consumer for appointment events and email notification attempt | Spring Boot, Spring Kafka, JavaMail |
| `kafka` | Event broker for asynchronous appointment notifications | Apache Kafka `3.9.0` |
| `cassandra` | Primary database for service data and stats rollups | Cassandra `4.1` |
| `cassandra-init` | One-shot schema bootstrap for missing keyspaces, tables, indexes, and auth roles | Cassandra `cqlsh`, `cassandra/schema.cql` |

## Main Request Flow

1. Client calls `api-gateway` on port `8080`.
2. Gateway allows `/api/auth/signup` and `/api/auth/signin` without JWT.
3. Gateway validates Bearer JWT for secured routes and forwards identity headers downstream.
4. Doctor, patient, and appointment services persist domain data in Cassandra.
5. Appointment create/update publishes an event to Kafka topic `appointments`.
6. Notification service consumes appointment events from Kafka and attempts email delivery.
7. Appointment stats can be rebuilt manually or by scheduler and read through `/api/v1/appointments/stats`.

## Gateway Routes

| Public Path | Downstream URI | Protection |
| --- | --- | --- |
| `/api/auth/**` | `http://auth-security:8080` | Signup/signin allowed, other auth paths filtered |
| `/api/v1/doctor/**` | `http://doctor-info:8080` | JWT required |
| `/api/v1/patient/**` | `http://patient-info:8080` | JWT required |
| `/api/v1/appointments/**` | `http://appointment-services:8080` | JWT required |

The gateway applies:

- Central authentication filter.
- Retry policy: 2 retries for `GET`, `POST`, `PUT`, and `DELETE` on `502`, `503`, and `504`.
- Circuit breakers with fallback URIs for auth, doctor, patient, and appointment services.
- Duplicate header cleanup and direct `OPTIONS` handling.

## Docker Runtime

Start or rebuild:

```bash
docker compose up -d --build
```

Check status:

```bash
docker compose ps
```

Schema bootstrap:

- `cassandra/schema.cql` contains idempotent `CREATE KEYSPACE IF NOT EXISTS`, `CREATE TABLE IF NOT EXISTS`, index creation, and auth role seed inserts.
- Docker Compose runs `cassandra-init` after Cassandra accepts CQL connections.
- Spring services wait for `cassandra-init` to complete successfully before starting.
- Data services use `SPRING_CASSANDRA_SCHEMA_ACTION=NONE`, so schema ownership stays in the CQL file.

Important persistence note:

- Cassandra data is mounted at `./cassandra-data:/var/lib/cassandra`.
- Do not run `docker compose down -v` if Cassandra data should be preserved.

## Docker Ports

| Service | Host Port | Container Port |
| --- | ---: | ---: |
| API Gateway | `8080` | `8080` |
| API Gateway debug | `5005` | `5005` |
| Cassandra | `9042` | `9042` |
| Kafka | `9092` | `9092` |

Other application services are reachable inside the Docker network on port `8080`.

## Cassandra Keyspaces

| Keyspace | Used By |
| --- | --- |
| `security_info` | Auth users and roles |
| `doctor_info` | Doctor data |
| `patient_info` | Patient data |
| `appointment_info` | Appointments, doctor appointment mapping, stats rollups |

The stats table is `appointment_info.appointment_stats` with primary key:

```sql
PRIMARY KEY ((period_type, period_start), doctor_id)
```

This supports rollups for:

- `period_type`: `DAY` or `WEEK`
- `doctor_id`: real doctor UUID or `ALL`
- counts: total, pending, confirmed, rejected, completed, other

## Kafka Config

Docker image: `apache/kafka:3.9.0`

Topic used by services:

```text
appointments
```

Producer:

- Service: `appointment-services`
- Bootstrap server: `kafka:9092`
- Sends JSON appointment notification events on create and update.

Consumer:

- Service: `notification-services`
- Bootstrap server: `kafka:9092`
- Group ID: `appointment-notifications-group`
- Consumes from topic `appointments`.

## Auth APIs

Signup:

```http
POST /api/auth/signup
Content-Type: application/json
```

```json
{
  "username": "u040740",
  "email": "u040740@example.com",
  "roles": ["patient"],
  "password": "Test@12345"
}
```

Signin:

```http
POST /api/auth/signin
Content-Type: application/json
```

```json
{
  "id": "u040740",
  "password": "Test@12345"
}
```

Secured calls use:

```http
Authorization: Bearer <jwt>
```

## Doctor APIs

Create doctor:

```http
POST /api/v1/doctor
```

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

Other doctor endpoints:

- `GET /api/v1/doctor/all`
- `GET /api/v1/doctor/{id}`
- `PUT /api/v1/doctor/{id}`

## Patient APIs

Create patient:

```http
POST /api/v1/patient
```

```json
{
  "patient_name": "Ravi Kumar",
  "email": "ravi@example.com",
  "age": "34"
}
```

Other patient endpoints:

- `GET /api/v1/patient`
- `GET /api/v1/patient/{id}`

## Appointment APIs

Create appointment:

```http
POST /api/v1/appointments/create
```

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

Update appointment:

```http
PUT /api/v1/appointments
```

```json
{
  "id": "93d42025-a9bf-407f-b323-8e426e068fb7",
  "doctorId": "c8f87a2a-b3f4-4c6a-b717-d052ee61d743",
  "patientId": "d678e60a-76fc-45a8-a361-af427ecc1f19",
  "appointmentTime": "2026-07-06T10:30:00",
  "status": "COMPLETED",
  "notes": "Routine cardiac consultation completed",
  "doctorComments": "Vitals stable"
}
```

Other appointment endpoints:

- `GET /api/v1/appointments/all`
- `GET /api/v1/appointments/doctor/{doctorId}`

Valid statuses:

- `PENDING`
- `CONFIRMED`
- `REJECTED`
- `COMPLETED`

## Stats APIs

Manual rebuild:

```http
POST /api/v1/appointments/stats/rebuild
```

Daily stats:

```http
GET /api/v1/appointments/stats?period=DAY&from=2026-07-06&to=2026-07-06&refresh=true
```

Weekly stats:

```http
GET /api/v1/appointments/stats?period=WEEK&from=2026-07-06&to=2026-07-12&refresh=true
```

Doctor-specific stats:

```http
GET /api/v1/appointments/stats?period=DAY&from=2026-07-06&to=2026-07-06&doctorId=<doctorUuid>&refresh=true
```

Scheduler:

```yaml
stats:
  scheduler:
    cron: ${APPOINTMENT_STATS_CRON:0 0 1 * * *}
```

The scheduler rebuilds daily and weekly Cassandra rollup rows from appointment data.

## Important Environment Variables

| Variable | Used By | Purpose |
| --- | --- | --- |
| `JWT_SECRET` | Gateway, auth | JWT signing/validation secret |
| `GATEWAY_SERVICE_PORT` | Gateway | Gateway HTTP port |
| `SPRING_CASSANDRA_CONTACT_POINTS` | Data services | Cassandra host, `cassandra` in Docker |
| `SPRING_CASSANDRA_PORT` | Data services | Cassandra port `9042` |
| `SPRING_CASSANDRA_KEYSPACE_NAME` | Data services | Service-specific keyspace |
| `SPRING_CASSANDRA_LOCAL_DATACENTER` | Data services | `datacenter1` |
| `KAFKA_BOOTSTRAP_SERVER` | Appointment, notification | Kafka broker `kafka:9092` |
| `KAFKA_TOPIC` | Appointment, notification | `appointments` |
| `KAFKA_GROUP_ID` | Notification | Consumer group |
| `MAIL_SERVER_HOST` | Notification | SMTP host |
| `MAIL_SERVER_PORT` | Notification | SMTP port |
| `MAIL_SERVER_USERNAME` | Notification | SMTP username |
| `MAIL_SERVER_PASSWORD` | Notification | SMTP password/app password |
| `APPOINTMENT_SERVICE_PORT` | Appointment | Internal service port, set to `8080` for gateway routing |
| `APPOINTMENT_STATS_CRON` | Appointment | Stats scheduler cron override |

## Known Runtime Notes

- `MAIL_SERVER_PASSWORD=password` is a placeholder in Docker compose, so SMTP authentication fails until a real app password/secret is supplied.
- The notification event currently uses development/mock doctor and patient details inside appointment service. Real IDs are saved on appointments, but notification names/emails come from the mock branch.
- Spring Boot Admin registration warnings are expected unless an `admin-service` container is added.
- Cassandra may log an authentication warning because the driver is configured with username/password while the default Cassandra container accepts unauthenticated local connections.
