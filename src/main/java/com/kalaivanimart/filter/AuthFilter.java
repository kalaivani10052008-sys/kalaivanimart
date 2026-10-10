package com.kalaivanimart.filter;

import com.kalaivanimart.model.User;
import com.kalaivanimart.util.JsonUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(filterName = "AuthFilter", urlPatterns = {"/api/admin/*", "/api/seller/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // Allow admin login without authentication
        if (path.endsWith("/api/admin/login")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("user") : null;

        if (currentUser == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please login.");
            return;
        }

        if (path.contains("/api/admin/") && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied. Administrator privileges required.");
            return;
        }

        if (path.contains("/api/seller/") && !"seller".equalsIgnoreCase(currentUser.getRole()) && !"admin".equalsIgnoreCase(currentUser.getRole())) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied. Seller privileges required.");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
