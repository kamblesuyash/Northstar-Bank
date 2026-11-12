package com.bank.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public abstract class ApiServlet extends HttpServlet {
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (value, type, context) -> new JsonPrimitive(value.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (value, type, context) -> new JsonPrimitive(value.toString()))
            .create();

    @FunctionalInterface
    protected interface ApiAction {
        Object run() throws SQLException;
    }

    protected final void respond(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        GSON.toJson(body, response.getWriter());
    }

    protected final void execute(HttpServletRequest request, HttpServletResponse response, ApiAction action)
            throws IOException {
        try {
            respond(response, HttpServletResponse.SC_OK, action.run());
        } catch (IllegalArgumentException ex) {
            respond(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", ex.getMessage()));
        } catch (SQLException ex) {
            getServletContext().log("Database operation failed", ex);
            respond(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "The request could not be completed. Please try again."));
        }
    }

    protected final String required(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return value.trim();
    }

    protected final BigDecimal amount(HttpServletRequest request) {
        String value = required(request, "amount");
        try {
            BigDecimal amount = new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.signum() <= 0 || amount.precision() > 15) {
                throw new IllegalArgumentException("Enter an amount greater than zero with at most 2 decimal places.");
            }
            return amount;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException("Enter a valid amount with at most 2 decimal places.");
        }
    }
}
