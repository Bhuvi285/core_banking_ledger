# Core Banking Ledger & Transaction CLI

A Java 17 console application for managing basic bank accounts and posting deposits, withdrawals, and transfers to a PostgreSQL-backed double-entry ledger. It uses JDBC directly so the database and transaction boundaries remain visible and easy to explain.

## Objectives and features

- Demonstrate layered Java design, JDBC, PostgreSQL, exception handling, and JUnit 5.
- Create and view savings/current accounts; freeze or reactivate accounts.
- Deposit, withdraw, and transfer money using `BigDecimal`.
- Store each transaction and its debit/credit ledger entries atomically.
- Produce transaction history and a date-filtered account statement.
- Write audit events without placing credentials or full account details in application logs.

## Technology

Java 17, Maven, PostgreSQL 16+, PostgreSQL JDBC, dotenv-java, SLF4J/Logback, and JUnit 5. There is no Spring Boot, ORM, JPA, or MySQL dependency.

## Architecture

`controller` handles console interaction; `service` contains banking rules; `repository` contains prepared JDBC statements; `infrastructure` provides JDBC connections and transaction boundaries; `model`, `dto`, `enums`, and `exception` define application data and outcomes. See [docs/architecture.md](docs/architecture.md).

## Structure

```text
src/main/java/com/bank/       Application, controllers, services, repositories, models
src/main/resources/            PostgreSQL schema, sample data, configuration, logging
src/test/java/com/bank/        Unit and PostgreSQL test suites
docs/                          Requirements, architecture, database, and testing notes
```

## Prerequisites

- JDK 17 (`java -version`)
- Maven 3.8+ (`mvn -version`)
- PostgreSQL 16+ and its command-line tools (`createdb`, `psql`)

## PostgreSQL setup (Windows PowerShell)

From the project directory, create the application database and load the schema and sample accounts:

```powershell
createdb -U postgres core_banking_ledger
psql -U postgres -d core_banking_ledger -f src\main\resources\schema.sql
psql -U postgres -d core_banking_ledger -f src\main\resources\data.sql
```

If PostgreSQL is not on `PATH`, run these commands from its `bin` directory or use the full paths to `createdb.exe` and `psql.exe`. The SQL scripts are safe to run again: tables/indexes use `IF NOT EXISTS` and sample account numbers are inserted only once.

Copy `.env.example` to `.env` and set the local PostgreSQL password:

```powershell
Copy-Item .env.example .env
```

Edit `.env` with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The application loads this root-level file using dotenv-java; environment variables take precedence. `.env` is ignored by Git and must not contain credentials committed to the repository.

## Build, test, and run

```powershell
mvn clean test
mvn package
mvn exec:java -Dexec.mainClass="com.bank.Main"
```

The Maven Exec plugin is configured so the third command runs the application with its dependencies. You can also run `com.bank.Main` from IntelliJ after reloading the Maven project.

The always-on JUnit tests cover input validation. PostgreSQL service, repository, and integration tests run only when `DB_TEST_URL`, `DB_TEST_USERNAME`, and `DB_TEST_PASSWORD` are set, and only when `DB_TEST_URL` ends in `_test`. Create a dedicated test database before enabling them:

```powershell
createdb -U postgres core_banking_ledger_test
$env:DB_TEST_URL = "jdbc:postgresql://localhost:5432/core_banking_ledger_test"
$env:DB_TEST_USERNAME = "postgres"
$env:DB_TEST_PASSWORD = "your_test_password"
mvn test
```

The gated tests reset all tables in this dedicated test database. Never point `DB_TEST_URL` at a production or other valuable database.

## Future improvements

Add pagination for history, account-number collision retry, richer transaction status workflows, database migration tooling, and concurrency/load testing. Production deployments also need authentication, authorization, operational controls, and an externalized secret manager; this project is an educational CLI, not a production banking system.
