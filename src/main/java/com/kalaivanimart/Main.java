package com.kalaivanimart;

import com.kalaivanimart.config.DatabaseConfig;
import com.kalaivanimart.filter.AuthFilter;
import com.kalaivanimart.filter.EncodingFilter;
import com.kalaivanimart.servlet.*;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws LifecycleException {
        System.out.println("==================================================");
        System.out.println("           KalaivaniMart Server Starting          ");
        System.out.println("==================================================");

        DatabaseConfig.initializeDatabase();

        String portStr = System.getenv("PORT");
        if (portStr == null || portStr.trim().isEmpty()) {
            portStr = System.getProperty("port", "8080");
        }
        int port = 8080;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ignored) {}

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        File assembledDir = new File("target/kalaivanimart");
        String docBase = assembledDir.exists()
            ? assembledDir.getAbsolutePath()
            : new File("src/main/webapp").getAbsolutePath();

        System.out.println("Webapp Document Base: " + docBase);

        Context ctx = tomcat.addContext("/kalaivanimart", docBase);

        // Fix classloader: delegate=true makes webapp classloader use parent-first
        // so exec:exec and Tomcat share the same Filter/Servlet class definitions
        WebappLoader wloader = new WebappLoader();
        wloader.setDelegate(true);
        ctx.setLoader(wloader);

        // ---- CRITICAL: DefaultServlet for static file serving ----
        // Without this, embedded Tomcat returns 404 for index.html, CSS, JS files.
        // The DefaultServlet handles all static resources (HTML, CSS, JS, images).
        org.apache.catalina.Wrapper defaultServletWrapper = Tomcat.addServlet(ctx, "default", "org.apache.catalina.servlets.DefaultServlet");
        defaultServletWrapper.addInitParameter("debug", "0");
        defaultServletWrapper.addInitParameter("listings", "false");
        defaultServletWrapper.setLoadOnStartup(1);
        ctx.addServletMappingDecoded("/", "default");

        // Welcome files: /kalaivanimart/ → index.html
        ctx.addWelcomeFile("index.html");
        ctx.addWelcomeFile("index.jsp");

        // ---- CRITICAL: Standard MIME mappings for embedded Tomcat ----
        // Without these, embedded Tomcat DefaultServlet returns null Content-Type,
        // which causes browsers to render HTML as raw plain text due to nosniff header.
        ctx.addMimeMapping("html", "text/html");
        ctx.addMimeMapping("htm", "text/html");
        ctx.addMimeMapping("css", "text/css");
        ctx.addMimeMapping("js", "application/javascript");
        ctx.addMimeMapping("mjs", "application/javascript");
        ctx.addMimeMapping("json", "application/json");
        ctx.addMimeMapping("svg", "image/svg+xml");
        ctx.addMimeMapping("png", "image/png");
        ctx.addMimeMapping("jpg", "image/jpeg");
        ctx.addMimeMapping("jpeg", "image/jpeg");
        ctx.addMimeMapping("gif", "image/gif");
        ctx.addMimeMapping("webp", "image/webp");
        ctx.addMimeMapping("ico", "image/x-icon");
        ctx.addMimeMapping("woff", "font/woff");
        ctx.addMimeMapping("woff2", "font/woff2");
        ctx.addMimeMapping("ttf", "font/ttf");
        ctx.addMimeMapping("txt", "text/plain");

        // Encoding Filter (UTF-8 for all requests/responses)
        FilterDef encDef = new FilterDef();
        encDef.setFilterName("EncodingFilter");
        encDef.setFilterClass(EncodingFilter.class.getName());
        ctx.addFilterDef(encDef);
        FilterMap encMap = new FilterMap();
        encMap.setFilterName("EncodingFilter");
        encMap.addURLPattern("/*");
        ctx.addFilterMap(encMap);

        // Auth/RBAC Filter for admin and seller routes
        FilterDef authDef = new FilterDef();
        authDef.setFilterName("AuthFilter");
        authDef.setFilterClass(AuthFilter.class.getName());
        ctx.addFilterDef(authDef);
        FilterMap authMapAdmin = new FilterMap();
        authMapAdmin.setFilterName("AuthFilter");
        authMapAdmin.addURLPattern("/api/admin/*");
        ctx.addFilterMap(authMapAdmin);
        FilterMap authMapSeller = new FilterMap();
        authMapSeller.setFilterName("AuthFilter");
        authMapSeller.addURLPattern("/api/seller/*");
        ctx.addFilterMap(authMapSeller);

        // --- Servlets ---
        Tomcat.addServlet(ctx, "HealthServlet", new HealthServlet());
        ctx.addServletMappingDecoded("/health", "HealthServlet");
        ctx.addServletMappingDecoded("/api/health", "HealthServlet");

        Tomcat.addServlet(ctx, "AuthServlet", new AuthServlet());
        ctx.addServletMappingDecoded("/api/login", "AuthServlet");
        ctx.addServletMappingDecoded("/api/register", "AuthServlet");
        ctx.addServletMappingDecoded("/api/logout", "AuthServlet");
        ctx.addServletMappingDecoded("/api/me", "AuthServlet");
        ctx.addServletMappingDecoded("/api/profile", "AuthServlet");
        ctx.addServletMappingDecoded("/api/admin/login", "AuthServlet");

        Tomcat.addServlet(ctx, "ProductServlet", new ProductServlet());
        ctx.addServletMappingDecoded("/api/products", "ProductServlet");
        ctx.addServletMappingDecoded("/api/products/*", "ProductServlet");

        Tomcat.addServlet(ctx, "CartServlet", new CartServlet());
        ctx.addServletMappingDecoded("/api/cart", "CartServlet");
        ctx.addServletMappingDecoded("/api/cart/*", "CartServlet");

        Tomcat.addServlet(ctx, "OrderServlet", new OrderServlet());
        ctx.addServletMappingDecoded("/api/orders", "OrderServlet");
        ctx.addServletMappingDecoded("/api/orders/*", "OrderServlet");

        Tomcat.addServlet(ctx, "WishlistServlet", new WishlistServlet());
        ctx.addServletMappingDecoded("/api/wishlist", "WishlistServlet");
        ctx.addServletMappingDecoded("/api/wishlist/*", "WishlistServlet");

        Tomcat.addServlet(ctx, "ReviewServlet", new ReviewServlet());
        ctx.addServletMappingDecoded("/api/reviews", "ReviewServlet");
        ctx.addServletMappingDecoded("/api/reviews/*", "ReviewServlet");

        Tomcat.addServlet(ctx, "AdminServlet", new AdminServlet());
        ctx.addServletMappingDecoded("/api/admin/*", "AdminServlet");

        Tomcat.addServlet(ctx, "SellerServlet", new SellerServlet());
        ctx.addServletMappingDecoded("/api/seller/*", "SellerServlet");

        // Root context: redirect / → /kalaivanimart/
        Context rootCtx = tomcat.addContext("", new File(".").getAbsolutePath());
        Tomcat.addServlet(rootCtx, "rootRedirectServlet", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.sendRedirect("/kalaivanimart/");
            }
        });
        rootCtx.addServletMappingDecoded("/*", "rootRedirectServlet");

        System.out.println("KalaivaniMart running on port: " + port);
        System.out.println("Application URL: http://localhost:" + port + "/kalaivanimart/");
        System.out.println("Health check:    http://localhost:" + port + "/kalaivanimart/health");
        System.out.println("==================================================");

        // Graceful shutdown hook
        final Tomcat finalTomcat = tomcat;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                finalTomcat.stop();
                System.out.println("KalaivaniMart server stopped.");
            } catch (LifecycleException ignored) {}
        }));

        tomcat.start();
        tomcat.getServer().await();
    }
}