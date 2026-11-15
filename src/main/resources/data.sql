INSERT INTO accounts
    (account_number, account_holder_name, account_type, balance, account_status)
VALUES
    ('100000000001', 'Aarav Sharma', 'SAVINGS', 12500.00, 'ACTIVE'),
    ('100000000002', 'Diya Patel', 'SAVINGS', 28750.50, 'ACTIVE'),
    ('100000000003', 'Ishaan Mehta', 'SAVINGS', 9300.00, 'ACTIVE'),
    ('200000000001', 'Ananya Rao', 'CURRENT', 54000.00, 'ACTIVE'),
    ('200000000002', 'Kabir Nair', 'CURRENT', 41250.75, 'ACTIVE')
ON CONFLICT (account_number) DO NOTHING;
