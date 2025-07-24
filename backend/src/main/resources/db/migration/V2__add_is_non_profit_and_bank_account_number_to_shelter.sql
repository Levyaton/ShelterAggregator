ALTER table shelter_entity
    ADD COLUMN is_non_profit BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN bank_account_number TEXT;