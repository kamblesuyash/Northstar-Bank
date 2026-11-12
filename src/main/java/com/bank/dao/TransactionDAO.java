package com.bank.dao;

import com.bank.model.Transaction;
import com.bank.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    public Transaction perform(String accountNumber, String type, BigDecimal amount) throws SQLException {
        boolean deposit = "DEPOSIT".equals(type);
        if (!deposit && !"WITHDRAWAL".equals(type)) {
            throw new IllegalArgumentException("Unsupported transaction type.");
        }
        String lockSql = "SELECT balance FROM accounts WHERE account_number = ? FOR UPDATE";
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        String insertSql = """
                INSERT INTO transactions (account_number, transaction_type, amount, balance_after_transaction)
                VALUES (?, ?, ?, ?)
                """;
        String timestampSql = "SELECT transaction_date FROM transactions WHERE transaction_id = ?";
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement lock = connection.prepareStatement(lockSql);
                 PreparedStatement update = connection.prepareStatement(updateSql);
                 PreparedStatement insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement timestampQuery = connection.prepareStatement(timestampSql)) {
                lock.setString(1, accountNumber);
                BigDecimal balance;
                try (ResultSet result = lock.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("Account was not found.");
                    }
                    balance = result.getBigDecimal("balance");
                }
                BigDecimal newBalance = deposit ? balance.add(amount) : balance.subtract(amount);
                if (newBalance.signum() < 0) {
                    throw new IllegalArgumentException("Insufficient funds for this withdrawal.");
                }
                if (newBalance.precision() > 15) {
                    throw new IllegalArgumentException("The resulting balance exceeds the account limit.");
                }
                update.setBigDecimal(1, newBalance);
                update.setString(2, accountNumber);
                update.executeUpdate();

                insert.setString(1, accountNumber);
                insert.setString(2, type);
                insert.setBigDecimal(3, amount);
                insert.setBigDecimal(4, newBalance);
                insert.executeUpdate();
                long id;
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Transaction ID was not generated.");
                    }
                    id = keys.getLong(1);
                }
                timestampQuery.setLong(1, id);
                java.time.LocalDateTime transactionDate;
                try (ResultSet result = timestampQuery.executeQuery()) {
                    if (!result.next() || result.getTimestamp("transaction_date") == null) {
                        throw new SQLException("Transaction date was not generated.");
                    }
                    transactionDate = result.getTimestamp("transaction_date").toLocalDateTime();
                }
                connection.commit();
                return new Transaction(id, accountNumber, type, amount, newBalance, transactionDate);
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    public List<Transaction> findRecent(String accountNumber) throws SQLException {
        String sql = """
                SELECT transaction_id, account_number, transaction_type, amount,
                       balance_after_transaction, transaction_date
                FROM transactions WHERE account_number = ?
                ORDER BY transaction_date DESC, transaction_id DESC LIMIT 100
                """;
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp timestamp = result.getTimestamp("transaction_date");
                    if (timestamp == null) {
                        throw new SQLException("Transaction date is missing.");
                    }
                    transactions.add(new Transaction(
                            result.getLong("transaction_id"),
                            result.getString("account_number"),
                            result.getString("transaction_type"),
                            result.getBigDecimal("amount"),
                            result.getBigDecimal("balance_after_transaction"),
                            timestamp.toLocalDateTime()));
                }
            }
        }
        return transactions;
    }
}
