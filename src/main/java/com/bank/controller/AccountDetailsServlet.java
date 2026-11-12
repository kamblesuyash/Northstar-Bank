package com.bank.controller;

import com.bank.dao.AccountDAO;
import com.bank.model.Account;
import com.bank.util.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/account")
public class AccountDetailsServlet extends ApiServlet {
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> {
            String number = (String) request.getSession(false).getAttribute("accountNumber");
            Account account = accountDAO.findByAccountNumber(number);
            if (account == null) {
                throw new IllegalArgumentException("Account was not found.");
            }
            return account;
        });
    }
}
