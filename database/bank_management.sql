CREATE DATABASE IF NOT EXISTS bank_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE bank_management;

CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_number CHAR(12) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    email VARCHAR(254) NOT NULL,
    address VARCHAR(500) NOT NULL,
    account_type ENUM('SAVINGS', 'CURRENT') NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id),
    UNIQUE KEY uq_accounts_account_number (account_number),
    CONSTRAINT chk_accounts_balance_nonnegative CHECK (balance >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_number CHAR(12) NOT NULL,
    transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'OPENING') NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_after_transaction DECIMAL(15, 2) NOT NULL,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id),
    KEY idx_transactions_account_date (account_number, transaction_date, transaction_id),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_number) REFERENCES accounts (account_number)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
