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

The CLI reads from standard input and writes its menu to the attached terminal. Inside Docker, the Java CLI connects to PostgreSQL at `db:5432`. Windows applications and pgAdmin connect to that same PostgreSQL container and database at `localhost:5433`; Compose binds the published port to loopback only.

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

## Local Maven execution without Docker

Prerequisites are Java 17, Maven 3.8+, and a running PostgreSQL 16 server. The required database is `core_banking_ledger`. To use the PostgreSQL container described above, start it first; its first initialization loads the existing schema and demo data scripts:

```powershell
docker compose up -d db
```

The container exposes PostgreSQL at `localhost:5433` on Windows. The same database can be used from the Java application in either mode: the local Maven process connects to `localhost:5433`, while the Java container connects to `db:5432`. Do not create a second database for local execution.

If using a separately installed local PostgreSQL server instead, create `core_banking_ledger` and initialize it with the existing scripts (adjust the port if your PostgreSQL server does not listen on `5433`):

```powershell
createdb -U postgres -p 5433 core_banking_ledger
psql -U postgres -p 5433 -d core_banking_ledger -f src\main\resources\schema.sql
psql -U postgres -p 5433 -d core_banking_ledger -f src\main\resources\data.sql
```

From PowerShell, configure the JDBC URL and credentials for the local Maven process. The example username `ledger_app` must already exist as a PostgreSQL role on the server; before using it, create/configure that role and grant it appropriate permissions on `core_banking_ledger`, including access to the schema, tables, and sequences the application uses.

These credentials must match the PostgreSQL server in `DB_URL`. For a separately installed local PostgreSQL server, use the role and password configured on that server. When connecting to the Compose database at `localhost:5433`, use the `POSTGRES_USER` and `POSTGRES_PASSWORD` values from `.env` instead; Compose credentials are distinct from credentials on a separately installed server.

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5433/core_banking_ledger"
$env:DB_USERNAME = "ledger_app"
$env:DB_PASSWORD = Read-Host "Enter the PostgreSQL password"
mvn exec:java "-Dexec.mainClass=com.bank.Main"
```

Remove the temporary process environment variables when finished:

```powershell
Remove-Item Env:DB_URL, Env:DB_USERNAME, Env:DB_PASSWORD
```

Build and run the test suite as needed:

```powershell
mvn clean test
mvn package
```

## PostgreSQL integration tests

PostgreSQL service and repository tests require `DB_TEST_URL`, `DB_TEST_USERNAME`, and `DB_TEST_PASSWORD`. They only accept a URL whose database path is exactly `/core_banking_ledger_test`; `TestDatabase.reset()` validates this before running schema or truncate statements. These tests reset that database, so use only a dedicated disposable test database and never valuable or production data.

## Architecture

`controller` handles console interaction; `service` contains banking rules; `repository` contains prepared JDBC statements; `infrastructure` provides JDBC connections and transaction boundaries; `model`, `dto`, `enums`, and `exception` define application data and outcomes. See [docs/architecture.md](docs/architecture.md).
