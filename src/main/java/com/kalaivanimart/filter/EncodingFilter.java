package com.kalaivanimart.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "EncodingFilter", urlPatterns = "/*")
public class EncodingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;
        httpResp.setHeader("X-Content-Type-Options", "nosniff");
        httpResp.setHeader("X-Frame-Options", "DENY");
        httpResp.setHeader("X-XSS-Protection", "1; mode=block");

        String uri = httpReq.getRequestURI();
        if (uri.endsWith("/") || uri.endsWith("/index.html") || uri.endsWith(".html") || uri.endsWith(".htm")) {
            httpResp.setContentType("text/html; charset=UTF-8");
        } else if (uri.endsWith(".css")) {
            httpResp.setContentType("text/css; charset=UTF-8");
        } else if (uri.endsWith(".js") || uri.endsWith(".mjs")) {
            httpResp.setContentType("application/javascript; charset=UTF-8");
        } else if (uri.endsWith(".svg")) {
            httpResp.setContentType("image/svg+xml; charset=UTF-8");
        } else if (uri.endsWith(".json")) {
            httpResp.setContentType("application/json; charset=UTF-8");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
