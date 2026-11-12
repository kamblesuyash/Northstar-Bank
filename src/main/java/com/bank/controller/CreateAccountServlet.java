package com.bank.controller;

import com.bank.dao.AccountDAO;
import com.bank.model.Account;
import com.bank.util.ApiServlet;
import com.bank.util.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@WebServlet("/api/accounts")
public class CreateAccountServlet extends ApiServlet {
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            String fullName = required(request, "fullName");
            String dobValue = required(request, "dob");
            String gender = required(request, "gender");
            String mobile = required(request, "mobile");
            String email = required(request, "email");
            String address = required(request, "address");
            String accountType = required(request, "accountType");
            String password = required(request, "password");
            BigDecimal initialDeposit;
            try {
                initialDeposit = new BigDecimal(required(request, "initialDeposit"))
                        .setScale(2, java.math.RoundingMode.UNNECESSARY);
            } catch (NumberFormatException | ArithmeticException ex) {
                throw new IllegalArgumentException("Enter a valid initial deposit with at most 2 decimal places.");
            }

            if (fullName.length() > 120 || fullName.isBlank()) {
                throw new IllegalArgumentException("Name must be 1 to 120 characters.");
            }
            LocalDate dob;
            try {
                dob = LocalDate.parse(dobValue);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Enter a valid date of birth.");
            }
            if (!dob.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Date of birth must be in the past.");
            }
            if (!"FEMALE".equals(gender) && !"MALE".equals(gender) && !"OTHER".equals(gender)) {
                throw new IllegalArgumentException("Choose a valid gender.");
            }
            if (!mobile.matches("[+0-9][0-9 ()-]{7,18}")) {
                throw new IllegalArgumentException("Enter a valid mobile number.");
            }
            if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalArgumentException("Enter a valid email address.");
            }
            if (address.length() > 500) {
                throw new IllegalArgumentException("Address must be 500 characters or fewer.");
            }
            if (!"SAVINGS".equals(accountType) && !"CURRENT".equals(accountType)) {
                throw new IllegalArgumentException("Choose a valid account type.");
            }
            if (initialDeposit.signum() < 0 || initialDeposit.precision() > 15) {
                throw new IllegalArgumentException("Initial deposit cannot be negative or exceed the account limit.");
            }
            if (password.length() < 8 || password.length() > 128) {
                throw new IllegalArgumentException("Password must be between 8 and 128 characters.");
            }

            Account account = new Account(0, null, fullName, dob, gender, mobile, email, address,
                    accountType, initialDeposit, null);
            char[] passwordChars = password.toCharArray();
            String accountNumber;
            try {
                accountNumber = accountDAO.createAccount(account, PasswordUtil.hash(passwordChars), initialDeposit);
            } finally {
                java.util.Arrays.fill(passwordChars, '\0');
            }
            return Map.of("message", "Account created successfully.", "accountNumber", accountNumber);
        });
    }
}
