package ru.itmo.movielab.web;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebFilter("/api/*")
public class AuthFilter implements Filter {

    public void doFilter(ServletRequest raw, ServletResponse output, FilterChain chain)
        throws IOException, ServletException {
        var req = (HttpServletRequest) raw;
        var res = (HttpServletResponse) output;
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("X-Content-Type-Options", "nosniff");
        String path = req.getRequestURI().substring(req.getContextPath().length());
        // Login принимает только JSON: обычная cross-site HTML-форма его послать не может.
        boolean mutation = !req.getMethod().equals("GET") && !req.getMethod().equals("HEAD");
        if (
            mutation &&
            (req.getContentType() == null ||
                !req.getContentType().toLowerCase().startsWith("application/json"))
        ) {
            error(res, 415, "Ожидается Content-Type: application/json");
            return;
        }
        if (path.equals("/api/auth/login") && req.getMethod().equals("POST")) {
            chain.doFilter(req, res);
            return;
        }
        var session = req.getSession(false);
        if (session == null || session.getAttribute("login") == null) {
            error(res, 401, "Войдите в систему");
            return;
        }
        if (mutation && !session.getAttribute("csrf").equals(req.getHeader("X-CSRF-Token"))) {
            error(res, 403, "Недействительный CSRF-токен. Войдите заново.");
            return;
        }
        chain.doFilter(req, res);
    }

    private void error(HttpServletResponse response, int status, String message)
        throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
