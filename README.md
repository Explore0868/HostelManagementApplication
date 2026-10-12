# Hostel Management System

A full-stack hostel operations portal built with React, Vite, Spring Boot, Spring Security, JWT, and MySQL. It provides separate administrator and student experiences, with hostel-scoped access enforced by the backend.

## Features

- Administrator dashboards for hostel operations and system administration.
- Student portal for personal profile, room, attendance, fees, and hostel announcements.
- Student, room, room-allocation, fee, attendance, and announcement management.
- JWT-based sign-in with role-based authorization.
- Multi-hostel data isolation enforced by backend authorization and service checks.
- MySQL persistence through Spring Data JPA.

## Project layout

```text
frontend/          React and Vite application
hostel-management/ Spring Boot REST API
```

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- Node.js and npm compatible with the Vite version in `frontend/package.json`
- MySQL 8 (or a compatible MySQL server)

## Run locally

### 1. Configure MySQL and the backend

Create a MySQL database and a user with access to it. The backend defaults to `localhost:3306/hostelManagement`; set environment variables to match your local database before starting it. Do not commit real credentials or JWT secrets.

PowerShell example:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/hostelManagement?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME = "your-local-mysql-user"
$env:SPRING_DATASOURCE_PASSWORD = "your-local-mysql-password"
$env:JWT_SECRET = "<base64-encoded-secret-of-at-least-32-bytes>"
```

Generate a local JWT secret with `openssl rand -base64 32` or another secure random generator. The local Spring profile has a development fallback; always set a private secret explicitly, especially outside local development.

Start the backend from the repository root:

```powershell
cd hostel-management
mvn spring-boot:run
```

The API listens on `http://localhost:8080` by default. Set `PORT` to use another port. The backend creates or updates the schema according to `JPA_DDL_AUTO` (default: `update`).

### 2. Start the frontend

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The frontend defaults to the API at `http://localhost:8080/api`. To use a different backend, set `VITE_API_BASE_URL` to its API base URL, such as `http://localhost:8080/api`. The backend's default CORS origin is `http://localhost:5173`; configure `CORS_ALLOWED_ORIGINS` when the frontend is hosted elsewhere.

## Initial administrator

Administrator creation is intentionally not enabled by default. To provision the first administrator, set `BOOTSTRAP_ADMIN_ENABLED=true` and provide `BOOTSTRAP_ADMIN_NAME`, `BOOTSTRAP_ADMIN_EMAIL`, and `BOOTSTRAP_ADMIN_PASSWORD` (12 to 72 characters). Start the backend once, then disable bootstrap again. Alternatively, provision an account through your existing database/admin setup. Do not put real administrator credentials in source control.

## Deployment configuration

For a hosted deployment, use the `prod` Spring profile and configure the database URL, username, password, JWT secret, and allowed frontend origins as environment variables. See [`hostel-management/.env.example`](hostel-management/.env.example) and [`frontend/.env.example`](frontend/.env.example) for the variable names and examples. Keep secrets in the hosting provider's environment settings, not in committed files.

## Security notes

- The backend determines roles from signed JWTs and the persisted account; client-side route checks are only for navigation.
- Hostel data access is scoped on the backend. Do not use frontend filtering as an authorization boundary.
- Use HTTPS and a unique, randomly generated JWT secret in deployed environments.
- Disable initial-admin bootstrap after the first account is created.
