# Database design

The database uses PostgreSQL identity primary keys, `VARCHAR` for text, `NUMERIC(19,2)` for money, and `TIMESTAMP WITH TIME ZONE` for timestamps.

## Tables and relationships

- **accounts:** generated `id`, unique `account_number`, holder name, type, non-negative balance, status, and creation/update timestamps.
- **transactions:** unique reference, transaction type and status, positive amount, description, creation time, and nullable source/destination account foreign keys. A check prevents the same account being both sides.
- **ledger_entries:** transaction and account foreign keys, `DEBIT`/`CREDIT` entry type, positive amount, and creation time.
- **audit_logs:** action, entity type and ID, description, and creation time.

Transactions reference accounts as their source and/or destination. Each ledger entry references one transaction and one account. Indexes support account transaction history, account ledger history, transaction-to-ledger lookup, and audit entity lookup.

## Constraints and indexes

The schema constrains enum-like values with PostgreSQL `CHECK` conditions; requires essential columns; enforces unique account numbers and transaction references; and ensures transaction and ledger amounts are positive. Foreign keys prevent orphan transaction and ledger records. History indexes order lookups by account and timestamp.

## Double-entry ledger

A transfer produces equal-value ledger entries: one `DEBIT` against the source account and one `CREDIT` against the destination. The account balances and both entries are changed in a single database transaction. A deposit creates a credit and a withdrawal creates a debit. The sample accounts start with balances but no ledger history, so statements derive the opening balance baseline from the current balance and recorded net ledger activity.
