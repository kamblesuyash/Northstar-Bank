package com.bank.controller;

import com.bank.dao.TransactionDAO;
import com.bank.util.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/withdraw")
public class WithdrawServlet extends ApiServlet {
    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response, () -> Map.of("message", "Withdrawal completed.",
                "transaction", transactionDAO.perform(
                        (String) request.getSession(false).getAttribute("accountNumber"), "WITHDRAWAL", amount(request))));
    }
}
