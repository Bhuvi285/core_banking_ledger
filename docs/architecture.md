# Architecture

## Layers and responsibilities

- **Controller:** prompts for console input, invokes services, and prints results or user-facing errors. It contains no SQL.
- **Service:** validates banking rules and coordinates one logical operation.
- **Repository:** maps PostgreSQL rows to Java models and executes parameterized SQL with JDBC.
- **Infrastructure:** loads database settings, opens connections, and commits or rolls back a unit of work.
- **Model, DTO, enums, exceptions:** describe persisted entities, request/result data, allowed values, and domain failures.

## Application data flow

The CLI in `Main` creates the shared configuration, connection factory, repositories, services, and controllers. A controller forwards a user's request to a service. The service validates the request, then uses repositories with a connection supplied by `TransactionManager`. The result returns through the controller for display.

## JDBC integration

`DatabaseConfig` reads the root `.env` file (when present) and process environment variables. `ConnectionFactory` uses `DriverManager` and the PostgreSQL driver. Repositories use `PreparedStatement`; SQL exceptions are translated at the transaction boundary so database implementation details are not shown to users.

## Transfer transaction flow

1. Validate account numbers, positive amount, and that accounts differ.
2. Begin a JDBC transaction with auto-commit disabled.
3. Select both accounts using `SELECT ... FOR UPDATE`, ordered by database ID to establish a consistent lock order.
4. Verify both accounts are active and the source balance covers the transfer.
5. Insert one transaction record, update both balances, and insert a debit and credit ledger row.
6. Insert an audit event and commit.
7. Roll back all changes if any operation fails.

The lock and every write use the same JDBC connection. This prevents a partial transfer and reduces deadlock risk when two transfers involve the same pair of accounts.
