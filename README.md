# patient-service

This is a minimal Spring Boot service for the HMS assignment.

Features:
- REST endpoints
- JPA + MySQL schema (auto-update)
- Dockerfile and docker-compose for local run
- GitHub Actions workflow for build & test

To run locally with docker-compose:
```bash
docker compose up --build
```

API examples:
- GET /api/patient-service/health
- standard CRUD endpoints provided by controller
