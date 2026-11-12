package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Account {
    private final long accountId;
    private final String accountNumber;
    private final String fullName;
    private final LocalDate dob;
    private final String gender;
    private final String mobile;
    private final String email;
    private final String address;
    private final String accountType;
    private final BigDecimal balance;
    private final LocalDateTime createdAt;

    public Account(long accountId, String accountNumber, String fullName, LocalDate dob, String gender,
                   String mobile, String email, String address, String accountType, BigDecimal balance,
                   LocalDateTime createdAt) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.fullName = fullName;
        this.dob = dob;
        this.gender = gender;
        this.mobile = mobile;
        this.email = email;
        this.address = address;
        this.accountType = accountType;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public long getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public String getFullName() { return fullName; }
    public LocalDate getDob() { return dob; }
    public String getGender() { return gender; }
    public String getMobile() { return mobile; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
