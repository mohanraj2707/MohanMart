package com.mohan.mohanmart.controller;

import com.mohan.mohanmart.util.DatabaseUtil;
import com.mohan.mohanmart.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint verifying service and database pool availability (§18).
 * GET /api/v1/health -> 200 {"status":"UP","db":"UP"} or 503 {"status":"DOWN","db":"DOWN"}.
 */
@WebServlet(name = "HealthServlet", urlPatterns = {"/api/v1/health", "/api/health", "/health"})
public class HealthServlet extends BaseServlet {

    private static final Logger logger = LoggerFactory.getLogger(HealthServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean dbUp = false;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 1) {
                dbUp = true;
            }
        } catch (Exception e) {
            logger.warn("Database health check failed: {}", e.getMessage());
            dbUp = false;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", dbUp ? "UP" : "DOWN");
        payload.put("db", dbUp ? "UP" : "DOWN");
        payload.put("success", dbUp);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("status", dbUp ? "UP" : "DOWN");
        data.put("db", dbUp ? "UP" : "DOWN");
        payload.put("data", data);

        int httpStatus = dbUp ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        resp.setStatus(httpStatus);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(JsonUtil.toJson(payload));
    }
}
