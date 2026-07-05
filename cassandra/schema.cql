-- Idempotent Cassandra schema bootstrap for the healthcare microservices.
-- Safe to run repeatedly: it only creates missing keyspaces/tables/indexes
-- and upserts the required auth role seed rows.

CREATE KEYSPACE IF NOT EXISTS security_info
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE KEYSPACE IF NOT EXISTS doctor_info
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE KEYSPACE IF NOT EXISTS patient_info
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE KEYSPACE IF NOT EXISTS appointment_info
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS security_info.roles (
    id text PRIMARY KEY,
    name text
);

CREATE TABLE IF NOT EXISTS security_info.users (
    id text PRIMARY KEY,
    email text,
    password text,
    username text,
    roles set<text>
);

CREATE INDEX IF NOT EXISTS roles_name_idx ON security_info.roles (name);
CREATE INDEX IF NOT EXISTS users_username_idx ON security_info.users (username);
CREATE INDEX IF NOT EXISTS users_email_idx ON security_info.users (email);

INSERT INTO security_info.roles (id, name) VALUES ('ROLE_PATIENT', 'ROLE_PATIENT');
INSERT INTO security_info.roles (id, name) VALUES ('ROLE_DOCTOR', 'ROLE_DOCTOR');
INSERT INTO security_info.roles (id, name) VALUES ('ROLE_ADMIN', 'ROLE_ADMIN');

CREATE TABLE IF NOT EXISTS doctor_info.doctor_info (
    id uuid PRIMARY KEY,
    email text,
    name text
);

CREATE TABLE IF NOT EXISTS doctor_info.doctor_details (
    id uuid,
    email text,
    name text,
    availability_status text,
    rating double,
    speciality text,
    year_of_exp double,
    PRIMARY KEY ((id, email), name)
) WITH CLUSTERING ORDER BY (name ASC);

CREATE TABLE IF NOT EXISTS patient_info.patient_details (
    patient_id uuid PRIMARY KEY,
    age text,
    email text,
    name text
);

CREATE TABLE IF NOT EXISTS appointment_info.appointment_detail (
    id uuid PRIMARY KEY,
    appointment_time timestamp,
    doctor_comments text,
    doctor_id uuid,
    notes text,
    patient_id uuid,
    status text
);

CREATE TABLE IF NOT EXISTS appointment_info.doctor_appointments (
    doctor_id uuid,
    appointment_id uuid,
    PRIMARY KEY (doctor_id, appointment_id)
) WITH CLUSTERING ORDER BY (appointment_id ASC);

CREATE TABLE IF NOT EXISTS appointment_info.appointment_stats (
    period_type text,
    period_start date,
    doctor_id text,
    completed_count bigint,
    confirmed_count bigint,
    other_count bigint,
    pending_count bigint,
    period_end date,
    rejected_count bigint,
    total_count bigint,
    PRIMARY KEY ((period_type, period_start), doctor_id)
) WITH CLUSTERING ORDER BY (doctor_id ASC);
