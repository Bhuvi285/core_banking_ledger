# Testing strategy

## Unit tests

`InputValidatorTest` checks valid input, monetary scale, malformed account numbers, required text, and menu bounds without requiring PostgreSQL.

## Service and repository tests

`AccountServiceTest`, `TransactionServiceTest`, `AccountRepositoryTest`, and `TransactionRepositoryTest` exercise the real services/repositories against PostgreSQL. They verify account creation and retrieval, deposits, withdrawals, frozen-account rejection, successful transfers, insufficient-funds behavior, and transaction history.

## Integration and rollback tests

`DatabaseIntegrationTest` checks the persisted debit/credit entries for a transfer. The service transfer test verifies that insufficient funds leave both balances and ledger entries unchanged. These tests are enabled only when `DB_TEST_URL` ends with `_test`; they reset the database tables, so use a separate disposable test database and never use production credentials or data.

Run unit tests with `mvn test`. To enable database tests, create `core_banking_ledger_test`, set `DB_TEST_URL`, `DB_TEST_USERNAME`, and `DB_TEST_PASSWORD`, then run `mvn test` again.

## Edge cases

The suite also covers zero/over-precision amounts, malformed account numbers, out-of-range menu choices, and inactive-account transactions. Additional useful cases include concurrent transfers, duplicate account numbers, invalid date ranges, and failures injected midway through a transaction.
