# Requirements

## Functional requirements

- Create savings and current accounts and retrieve account details.
- Deposit, withdraw, and transfer a positive monetary amount.
- Freeze an active account and reactivate a frozen account.
- Record transactions, debit/credit ledger entries, and audit events.
- Retrieve transaction history and generate statements for a selected date range.
- Reject malformed input, missing accounts, inactive accounts, same-account transfers, and insufficient funds.

## Non-functional requirements

- Use Java 17, Maven, JDBC, and PostgreSQL; do not use an ORM.
- Keep controllers, services, repositories, and database infrastructure separate.
- Use prepared statements, database constraints, and `BigDecimal` for money.
- Keep each multi-row banking operation atomic and avoid logging credentials.
- Provide understandable errors to CLI users and meaningful database diagnostics to application logs.

## Business rules and banking constraints

- Account numbers are unique; account balances cannot be negative.
- Only active accounts may participate in deposits, withdrawals, and transfers.
- Transaction amounts must be positive and fit `NUMERIC(19,2)`.
- A transfer must have different source and destination accounts and sufficient source funds.
- A successful transfer debits the source and credits the destination by the same amount.
- Every transfer, both balance updates, both ledger entries, and its audit event commit together or roll back together.
- For each transfer, total debit and credit ledger amounts are equal.
