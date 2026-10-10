package com.kalaivanimart.servlet;

import com.kalaivanimart.config.DatabaseConfig;
import com.kalaivanimart.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "HealthServlet", urlPatterns = {"/health", "/api/health"})
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean dbHealthy = DatabaseConfig.checkHealth();
        String dbName = DatabaseConfig.getDatabaseProductName();
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        Map<String, Object> health = new HashMap<>();
        health.put("status", dbHealthy ? "UP" : "DOWN");
        health.put("service", "KalaivaniMart");
        health.put("databaseConnected", dbHealthy);
        health.put("databaseEngine", dbName);
        health.put("uptimeMs", uptime);
        health.put("timestamp", System.currentTimeMillis());

        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("totalMemoryMb", runtime.totalMemory() / (1024 * 1024));
        memory.put("freeMemoryMb", runtime.freeMemory() / (1024 * 1024));
        memory.put("usedMemoryMb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        health.put("memory", memory);

        int statusCode = dbHealthy ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        JsonUtil.sendJson(resp, statusCode, health);
    }
}
