package com.bank.util;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("accountNumber") == null) {
            if (path.startsWith("/api/")) {
                writeJson(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                        Map.of("error", "Please sign in to continue."));
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html");
            }
            return;
        }

        if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
            String expected = (String) session.getAttribute("csrfToken");
            String supplied = httpRequest.getHeader("X-CSRF-Token");
            if (expected == null || supplied == null
                    || !java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                writeJson(httpResponse, HttpServletResponse.SC_FORBIDDEN,
                        Map.of("error", "Your session has expired. Refresh the page and try again."));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean isPublic(String path) {
        return path.equals("/") || path.equals("/index.html") || path.equals("/login.html")
                || path.equals("/create-account.html") || path.equals("/api/login")
                || path.equals("/api/accounts") || path.startsWith("/css/")
                || path.startsWith("/js/") || path.equals("/favicon.ico");
    }

    private static void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new com.google.gson.Gson().toJson(body, response.getWriter());
    }
}
