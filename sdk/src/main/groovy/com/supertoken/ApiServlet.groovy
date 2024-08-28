package com.supertoken

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class ApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Handle GET request
        String path = req.getRequestURI();
        // Process the request based on the path
        // Example: retrieve data from a database or service
        resp.setContentType("application/json");
        resp.getWriter().write("{\"message\": \"GET request handled\"}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Handle POST request
        // Read the request body
        StringBuilder body = new StringBuilder();
        String line;
        BufferedReader reader = req.getReader();
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        // Process the request body
        // Example: create or update a resource
        resp.setContentType("application/json");
        resp.getWriter().write("{\"message\": \"POST request handled\"}");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Handle PUT request
        StringBuilder body = new StringBuilder();
        String line;
        BufferedReader reader = req.getReader();
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        // Process the request body
        // Example: update an existing resource
        resp.setContentType("application/json");
        resp.getWriter().write("{\"message\": \"PUT request handled\"}");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Handle DELETE request
        // Example: delete a resource
        resp.setContentType("application/json");
        resp.getWriter().write("{\"message\": \"DELETE request handled\"}");
    }
}

