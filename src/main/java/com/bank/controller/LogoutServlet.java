package com.bank.controller;

import com.bank.util.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/logout")
public class LogoutServlet extends ApiServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession(false).invalidate();
        respond(response, HttpServletResponse.SC_OK, Map.of("message", "Signed out."));
    }
}
