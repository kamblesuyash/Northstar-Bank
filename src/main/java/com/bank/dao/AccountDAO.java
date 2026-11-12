package com.bank.dao;

import com.bank.model.Account;
import com.bank.util.DBConnection;
import com.bank.util.PasswordUtil;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AccountDAO {
    private static final SecureRandom RANDOM = new SecureRandom();

    public Account authenticate(String accountNumber, char[] password) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !PasswordUtil.verify(password, result.getString("password_hash"))) {
                    return null;
                }
                return map(result);
            }
        }
    }

    public Account findByAccountNumber(String accountNumber) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    public String createAccount(Account account, String passwordHash, java.math.BigDecimal initialDeposit)
            throws SQLException {
        String accountSql = """
                INSERT INTO accounts (account_number, full_name, dob, gender, mobile, email, address,
                                      account_type, balance, password_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String transactionSql = """
                INSERT INTO transactions (account_number, transaction_type, amount, balance_after_transaction)
                VALUES (?, 'OPENING', ?, ?)
                """;
        for (int attempt = 0; attempt < 5; attempt++) {
            String accountNumber = newAccountNumber();
            try (Connection connection = DBConnection.getConnection()) {
                connection.setAutoCommit(false);
                try (PreparedStatement accountStatement =
                             connection.prepareStatement(accountSql, Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement transactionStatement = connection.prepareStatement(transactionSql)) {
                    accountStatement.setString(1, accountNumber);
                    accountStatement.setString(2, account.getFullName());
                    accountStatement.setDate(3, Date.valueOf(account.getDob()));
                    accountStatement.setString(4, account.getGender());
                    accountStatement.setString(5, account.getMobile());
                    accountStatement.setString(6, account.getEmail());
                    accountStatement.setString(7, account.getAddress());
                    accountStatement.setString(8, account.getAccountType());
                    accountStatement.setBigDecimal(9, initialDeposit);
                    accountStatement.setString(10, passwordHash);
                    accountStatement.executeUpdate();

                    if (initialDeposit.signum() > 0) {
                        transactionStatement.setString(1, accountNumber);
                        transactionStatement.setBigDecimal(2, initialDeposit);
                        transactionStatement.setBigDecimal(3, initialDeposit);
                        transactionStatement.executeUpdate();
                    }
                    connection.commit();
                    return accountNumber;
                } catch (SQLException ex) {
                    connection.rollback();
                    if ("23000".equals(ex.getSQLState()) && attempt < 4) {
                        continue;
                    }
                    throw ex;
                }
            }
        }
        throw new SQLException("Unable to allocate a unique account number.");
    }

    private static String newAccountNumber() {
        return String.format("%012d", 100_000_000_000L + RANDOM.nextLong(900_000_000_000L));
    }

    private static Account map(ResultSet result) throws SQLException {
        Date dob = result.getDate("dob");
        java.sql.Timestamp createdAt = result.getTimestamp("created_at");
        if (dob == null || createdAt == null) {
            throw new SQLException("Account record is missing a required date.");
        }
        return new Account(
                result.getLong("account_id"),
                result.getString("account_number"),
                result.getString("full_name"),
                dob.toLocalDate(),
                result.getString("gender"),
                result.getString("mobile"),
                result.getString("email"),
                result.getString("address"),
                result.getString("account_type"),
                result.getBigDecimal("balance"),
                createdAt.toLocalDateTime());
    }
}
