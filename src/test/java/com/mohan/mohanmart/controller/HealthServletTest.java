package com.mohan.mohanmart.controller;

import com.mohan.mohanmart.dao.BaseDAOTest;
import com.mohan.mohanmart.util.DatabaseUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthServletTest extends BaseDAOTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("GET /api/v1/health returns 200 with status UP and db UP when pool is healthy")
    void testHealthUp() throws Exception {
        HealthServlet servlet = new HealthServlet();
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String body = writer.toString();
        assertTrue(body.contains("\"status\":\"UP\""));
        assertTrue(body.contains("\"db\":\"UP\""));
    }

    @Test
    @DisplayName("GET /api/v1/health returns 503 with db DOWN when database connection fails")
    void testHealthDownReturns503() throws Exception {
        DataSource previous = DatabaseUtil.getDataSource();
        try {
            DatabaseUtil.setDataSource(null);
            HealthServlet servlet = new HealthServlet();
            StringWriter writer = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(writer));

            servlet.doGet(request, response);

            verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            String body = writer.toString();
            assertTrue(body.contains("\"db\":\"DOWN\""));
        } finally {
            DatabaseUtil.setDataSource(previous);
        }
    }
}
