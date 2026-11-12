package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private final long transactionId;
    private final String accountNumber;
    private final String transactionType;
    private final BigDecimal amount;
    private final BigDecimal balanceAfterTransaction;
    private final LocalDateTime transactionDate;

    public Transaction(long transactionId, String accountNumber, String transactionType, BigDecimal amount,
                       BigDecimal balanceAfterTransaction, LocalDateTime transactionDate) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.transactionDate = transactionDate;
    }

    public long getTransactionId() { return transactionId; }
    public String getAccountNumber() { return accountNumber; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfterTransaction() { return balanceAfterTransaction; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
}
