# BookClub Bot

Telegram group book-club management backend built with Kotlin and Spring Boot.

## Stack
- Kotlin / Spring Boot
- PostgreSQL / Spring Data JPA
- Flyway
- Docker Compose
- GitHub Actions

## Local setup
1. Copy `.env.example` to `.env` and change the password.
2. Start PostgreSQL: `docker compose --env-file .env up -d postgres`
3. Run: `gradle bootRun`
4. Health: `http://localhost:8080/actuator/health`

The first slice implements monthly submission policy and persistence ports/adapters.
Telegram webhook, metadata enrichment, poll lifecycle and cloud deployment are next.
