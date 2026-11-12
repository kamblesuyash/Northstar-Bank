package com.bank.controller;

import com.bank.dao.AccountDAO;
import com.bank.model.Account;
import com.bank.util.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;

@WebServlet("/api/login")
public class LoginServlet extends ApiServlet {
    private final AccountDAO accountDAO = new AccountDAO();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accountNumber;
        String rawPassword;
        try {
            accountNumber = required(request, "accountNumber");
            rawPassword = required(request, "password");
        } catch (IllegalArgumentException ex) {
            respond(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", ex.getMessage()));
            return;
        }
        if (!accountNumber.matches("\\d{12}") || rawPassword.length() > 128) {
            respond(response, HttpServletResponse.SC_UNAUTHORIZED, Map.of("error", "Invalid account number or password."));
            return;
        }
        char[] password = rawPassword.toCharArray();
        try {
            Account account = accountDAO.authenticate(accountNumber, password);
            if (account == null) {
                respond(response, HttpServletResponse.SC_UNAUTHORIZED,
                        Map.of("error", "Invalid account number or password."));
                return;
            }
            HttpSession session = request.getSession();
            request.changeSessionId();
            session = request.getSession(false);
            session.setAttribute("accountNumber", account.getAccountNumber());
            byte[] token = new byte[32];
            RANDOM.nextBytes(token);
            session.setAttribute("csrfToken", Base64.getUrlEncoder().withoutPadding().encodeToString(token));
            respond(response, HttpServletResponse.SC_OK,
                    Map.of("message", "Welcome back.", "redirect", "dashboard.html"));
        } catch (SQLException ex) {
            getServletContext().log("Login database operation failed", ex);
            respond(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Sign-in is temporarily unavailable."));
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}
