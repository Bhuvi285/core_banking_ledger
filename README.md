# Core Banking Ledger & Transaction CLI

A Java 17 console application for managing demo bank accounts and posting deposits, withdrawals, and transfers to a PostgreSQL-backed double-entry ledger. It uses JDBC directly so the database and transaction boundaries remain visible and easy to explain.

## Features

- Create and view savings/current accounts; freeze or reactivate accounts.
- Deposit, withdraw, and transfer money using `BigDecimal`.
- Store each transaction and its debit/credit ledger entries atomically.
- Produce transaction history and date-filtered account statements.
- Write audit events without placing credentials or full account details in application logs.

This project is an educational CLI demonstration only. Do not use real financial or customer data.

## Technology and prerequisites

- Java 17 and Maven 3.8+ for local execution and tests.
- Docker Engine with Docker Compose v2 for container deployment.
- PostgreSQL 16+ for a local, non-containerized database.

There is no Spring Boot, ORM, JPA, or web application.

## Docker deployment (Windows PowerShell)

From the project directory, create a local environment file from the placeholders:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and replace `POSTGRES_PASSWORD` with a unique, long demo password. Do not commit `.env`; credentials are passed into containers at runtime and are not included in the Docker image.

Build the Java image and start PostgreSQL:

```powershell
docker compose build
docker compose up -d db
docker compose ps
```

Wait for the database to report `healthy`, then run the CLI interactively in the current terminal:

```powershell
docker compose run --rm cli
```

The CLI reads from standard input and writes its menu to the attached terminal. It connects to PostgreSQL at `db:5432` on the private Compose network; the database port is not published to the host.

### Database setup and persistence

On the first initialization of this Compose project's named volume, the official PostgreSQL image runs the existing `src/main/resources/schema.sql` and `data.sql` scripts in order. The scripts are mounted read-only; the schema is not duplicated. PostgreSQL retains the database across container restarts and `docker compose down`. Initialization scripts do not run again when the volume already contains a database. Existing local PostgreSQL data is not used or modified by this Compose setup.

Stop containers without deleting the database volume:

```powershell
docker compose down
```

To permanently remove this Compose project's containers and named volume (including its demo database and data), run:

```powershell
docker compose down --volumes
```

Volume removal is destructive and cannot be undone. Use it only when you intend to discard this project's demo data.

## Local Maven execution

Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the project-root `.env` or process environment. For a local PostgreSQL server, create a database and initialize it once:

```powershell
createdb -U postgres core_banking_ledger
psql -U postgres -d core_banking_ledger -f src\main\resources\schema.sql
psql -U postgres -d core_banking_ledger -f src\main\resources\data.sql
```

Run the tests and package the application:

```powershell
mvn clean test
mvn package
```

Launch the CLI from the project directory with its runtime dependencies:

```powershell
mvn exec:java "-Dexec.mainClass=com.bank.Main"
```

## PostgreSQL integration tests

PostgreSQL service and repository tests require `DB_TEST_URL`, `DB_TEST_USERNAME`, and `DB_TEST_PASSWORD`. They only accept a URL whose database path is exactly `/core_banking_ledger_test`; `TestDatabase.reset()` validates this before running schema or truncate statements. These tests reset that database, so use only a dedicated disposable test database and never valuable or production data.

## Architecture

`controller` handles console interaction; `service` contains banking rules; `repository` contains prepared JDBC statements; `infrastructure` provides JDBC connections and transaction boundaries; `model`, `dto`, `enums`, and `exception` define application data and outcomes. See [docs/architecture.md](docs/architecture.md).
