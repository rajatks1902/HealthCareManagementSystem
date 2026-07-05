# Healthcare Management System - Use Case Test Report

Tested on: 2026-07-06 03:57-04:08 IST  
Environment: Docker Desktop using `docker compose up -d --build`  
Gateway URL: `http://127.0.0.1:8080`  
Cassandra persistence: preserved. No Cassandra volume/data deletion was performed.

## Runtime Status

All main containers were running during validation:

| Service | Container | Status |
| --- | --- | --- |
| API Gateway | `api-gateway` | Running, gateway health returned `200 {"status":"UP"}` |
| Auth Service | `auth-security` | Running |
| Doctor Service | `doctor-info` | Running |
| Patient Service | `patient-info` | Running |
| Appointment Service | `appointment-services` | Running |
| Notification Service | `notification-services` | Running |
| Kafka | `kafka` | Running |
| Cassandra | `cassandra` | Running |

## Test Data

| Field | Value |
| --- | --- |
| Auth username | `u040740` |
| Auth email | `u040740@example.com` |
| Doctor | `Dr Asha Rao`, Cardiology, 12 years experience |
| Doctor ID | `c8f87a2a-b3f4-4c6a-b717-d052ee61d743` |
| Patient | `Ravi Kumar 20260706040740`, age `34` |
| Patient ID | `d678e60a-76fc-45a8-a361-af427ecc1f19` |
| Appointment time | `2026-07-06T10:30:00` |
| Appointment ID | `93d42025-a9bf-407f-b323-8e426e068fb7` |

## Use Cases Validated

| Use Case | Endpoint | Result | Evidence |
| --- | --- | --- | --- |
| Gateway health | `GET /actuator/health` | PASS | HTTP `200`, body `{"status":"UP"}` |
| User signup | `POST /api/auth/signup` | PASS | HTTP `200`, body `{"message":"User registered successfully!"}` |
| User signin/JWT | `POST /api/auth/signin` | PASS | HTTP `200`, returned Bearer JWT for `u040740` |
| Protected API without token | `GET /api/v1/doctor/all` | PASS | HTTP `401`, gateway rejected unauthenticated access |
| Create doctor | `POST /api/v1/doctor` | PASS | HTTP `200`, returned doctor ID `c8f87a2a-b3f4-4c6a-b717-d052ee61d743` |
| List doctors | `GET /api/v1/doctor/all` | PASS | HTTP `200` |
| Get doctor by ID | `GET /api/v1/doctor/{id}` | PASS | HTTP `200` |
| Update doctor | `PUT /api/v1/doctor/{id}` | PASS | HTTP `200` |
| Create patient | `POST /api/v1/patient` | PASS | HTTP `200`, returned patient ID `d678e60a-76fc-45a8-a361-af427ecc1f19` |
| List patients | `GET /api/v1/patient` | PASS | HTTP `200` |
| Get patient by ID | `GET /api/v1/patient/{id}` | PASS | HTTP `200` |
| Create appointment | `POST /api/v1/appointments/create` | PASS | HTTP `200`, appointment ID `93d42025-a9bf-407f-b323-8e426e068fb7` |
| List appointments | `GET /api/v1/appointments/all` | PASS | HTTP `200` |
| Get doctor appointments | `GET /api/v1/appointments/doctor/{doctorId}` | PASS | HTTP `200` |
| Complete appointment | `PUT /api/v1/appointments` | PASS | HTTP `200`, appointment updated successfully |
| Manual stats rebuild | `POST /api/v1/appointments/stats/rebuild` | PASS | HTTP `200` |
| Daily stats | `GET /api/v1/appointments/stats?period=DAY&from=2026-07-06&to=2026-07-06&refresh=true` | PASS | Returned total `1`, completed `1`, pending `0` |
| Weekly stats | `GET /api/v1/appointments/stats?period=WEEK&from=2026-07-06&to=2026-07-12&refresh=true` | PASS | Returned total `1`, completed `1`, pending `0` |
| Doctor daily stats | `GET /api/v1/appointments/stats?period=DAY&doctorId={doctorId}&refresh=true` | PASS | Returned doctor-specific total `1`, completed `1`, pending `0` |
| Kafka notification publish | Appointment create/update | PASS | Producer logged sends to topic `appointments`, partition `1`, offsets `0` and `1` |
| Kafka notification consume | Notification service listener | PASS | Consumer logged received appointment events for statuses `PENDING` and `COMPLETED` |
| Email delivery | Notification service SMTP | CONFIG BLOCKED | Kafka consumed events, but SMTP failed because Docker config uses placeholder Gmail password |

## Stats Evidence

API daily stats response:

```json
[{"periodType":"DAY","periodStart":"2026-07-06","periodEnd":"2026-07-06","doctorId":"ALL","total":1,"pending":0,"confirmed":0,"rejected":0,"completed":1,"other":0}]
```

API weekly stats response:

```json
[{"periodType":"WEEK","periodStart":"2026-07-06","periodEnd":"2026-07-12","doctorId":"ALL","total":1,"pending":0,"confirmed":0,"rejected":0,"completed":1,"other":0}]
```

Cassandra `appointment_info.appointment_stats` rows after rebuild:

| period_type | period_start | doctor_id | total_count | pending_count | completed_count |
| --- | --- | --- | ---: | ---: | ---: |
| `WEEK` | `2026-07-06` | `ALL` | 1 | 0 | 1 |
| `WEEK` | `2026-07-06` | `c8f87a2a-b3f4-4c6a-b717-d052ee61d743` | 1 | 0 | 1 |
| `DAY` | `2026-07-06` | `ALL` | 1 | 0 | 1 |
| `DAY` | `2026-07-06` | `c8f87a2a-b3f4-4c6a-b717-d052ee61d743` | 1 | 0 | 1 |

## Notes From Testing

- `docker-compose.yml` was adjusted so `appointment-services` runs on internal port `8080`, matching the API gateway route `http://appointment-services:8080`.
- Kafka worked end to end: appointment service produced events and notification service consumed them.
- Actual email sending did not complete because `MAIL_SERVER_PASSWORD=password` is a placeholder. The notification workflow is validated through Kafka consumption and attempted SMTP delivery.
- Appointment service currently uses development/mock doctor and patient details when building notification events. The appointment itself uses the real doctor and patient IDs created during the test.
- Spring Boot Admin registration warnings appeared because `admin-service` is not part of the Docker compose stack. This did not block core use cases.
