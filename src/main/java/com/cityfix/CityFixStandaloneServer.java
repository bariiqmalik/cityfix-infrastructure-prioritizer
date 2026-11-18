package com.cityfix;

import com.cityfix.dao.DatabaseConnection;
import com.cityfix.dao.ReportDAOImpl;
import com.cityfix.model.RepairTask;
import com.cityfix.model.Report;
import com.cityfix.service.PrioritizerService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Pure Java (Zero External Dependency) Web Server.
 * Runs on standard Java 17+ SE using com.sun.net.httpserver.
 */
public class CityFixStandaloneServer {

    private static final int PORT = 8080;
    private static PrioritizerService prioritizerService;

    public static void main(String[] args) throws IOException {
        System.out.println("Starting CityFix Pure Java Server on port " + PORT + "...");
        
        // Initialize schema
        try (InputStream in = CityFixStandaloneServer.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in != null) {
                String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                DatabaseConnection.initializeSchema(sql);
            }
        } catch (Exception ignored) {}

        prioritizerService = new PrioritizerService(new ReportDAOImpl());

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Contexts
        server.createContext("/api/tasks/prioritized", new PrioritizedTasksHandler());
        server.createContext("/api/tasks/extract-max", new ExtractMaxHandler());
        server.createContext("/api/reports", new ReportsHandler());
        server.createContext("/api/metrics", new MetricsHandler());

        // Static files handler
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("=================================================================");
        System.out.println(" CityFix Municipal Prioritizer is LIVE on: http://localhost:" + PORT);
        System.out.println(" Admin Dashboard: http://localhost:" + PORT + "/index.html");
        System.out.println(" Repair Tasks:    http://localhost:" + PORT + "/tasks.html");
        System.out.println(" Reports View:    http://localhost:" + PORT + "/reports.html");
        System.out.println(" Citizen Form:    http://localhost:" + PORT + "/submit.html");
        System.out.println("=================================================================");
    }

    static class PrioritizedTasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            List<RepairTask> tasks = prioritizerService.getPrioritizedQueue();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < tasks.size(); i++) {
                RepairTask t = tasks.get(i);
                if (i > 0) json.append(",");
                json.append(taskToJson(t));
            }
            json.append("]");
            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    static class ExtractMaxHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                RepairTask top = prioritizerService.extractNextUrgentTask();
                sendJsonResponse(exchange, 200, taskToJson(top));
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    static class ReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Parse simple json fields
                Report report = parseReportJson(body);
                Report saved = prioritizerService.submitReport(report);
                sendJsonResponse(exchange, 200, reportToJson(saved));
            } else {
                List<Report> list = prioritizerService.getAllReports();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(reportToJson(list.get(i)));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            }
        }
    }

    static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            var m = prioritizerService.getMetrics();
            String json = String.format("{\"totalReports\":%d,\"openReports\":%d,\"inProgress\":%d,\"resolved\":%d,\"highPriority\":%d,\"totalTasks\":%d,\"avgResolutionTime\":\"%s\"}",
                    m.get("totalReports"), m.get("openReports"), m.get("inProgress"), m.get("resolved"), m.get("highPriority"), m.get("totalTasks"), m.get("avgResolutionTime"));
            sendJsonResponse(exchange, 200, json);
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }
            InputStream in = getClass().getResourceAsStream("/static" + path);
            if (in == null) {
                File file = new File("src/main/resources/static" + path);
                if (file.exists() && !file.isDirectory()) {
                    in = new FileInputStream(file);
                }
            }
            if (in == null) {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }
            String contentType = "text/html";
            if (path.endsWith(".css")) contentType = "text/css";
            else if (path.endsWith(".js")) contentType = "application/javascript";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";
            else if (path.endsWith(".png")) contentType = "image/png";

            exchange.getResponseHeaders().set("Content-Type", contentType);
            byte[] bytes = in.readAllBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String taskToJson(RepairTask t) {
        return String.format(
                "{\"taskId\":\"%s\",\"issueType\":\"%s\",\"location\":\"%s\",\"description\":\"%s\",\"latitude\":%f,\"longitude\":%f,\"severity\":%d,\"trafficVolume\":%d,\"reportCount\":%d,\"status\":\"%s\",\"urgencyScore\":%.2f,\"displayScore\":%d,\"priorityLevel\":\"%s\",\"reportedBy\":\"%s\",\"timestamp\":\"%s\"}",
                escape(t.getTaskId()), escape(t.getIssueType()), escape(t.getLocation()), escape(t.getDescription()),
                t.getLatitude(), t.getLongitude(), t.getSeverity(), t.getTrafficVolume(), t.getReportCount(),
                escape(t.getStatus()), t.calculateUrgencyScore(), t.getDisplayScore(), t.getPriorityLevel(),
                escape(t.getReportedBy()), t.getTimestamp().toString()
        );
    }

    private static String reportToJson(Report r) {
        return String.format(
                "{\"reportId\":%d,\"taskCode\":\"%s\",\"issueType\":\"%s\",\"locationName\":\"%s\",\"description\":\"%s\",\"latitude\":%f,\"longitude\":%f,\"severityRating\":%d,\"trafficImpact\":%d,\"reportCount\":%d,\"status\":\"%s\",\"createdAt\":\"%s\"}",
                r.getReportId(), escape(r.getTaskCode()), escape(r.getIssueType()), escape(r.getLocationName()),
                escape(r.getDescription()), r.getLatitude(), r.getLongitude(), r.getSeverityRating(),
                r.getTrafficImpact(), r.getReportCount(), escape(r.getStatus()), r.getCreatedAt().toString()
        );
    }

    private static Report parseReportJson(String json) {
        Report r = new Report();
        r.setIssueType(extractJsonString(json, "issueType", "Road Damage"));
        r.setLocationName(extractJsonString(json, "locationName", "Main St"));
        r.setDescription(extractJsonString(json, "description", ""));
        r.setLatitude(extractJsonDouble(json, "latitude", 28.6139));
        r.setLongitude(extractJsonDouble(json, "longitude", 77.2090));
        r.setSeverityRating(extractJsonInt(json, "severityRating", 5));
        r.setTrafficImpact(extractJsonInt(json, "trafficImpact", 5));
        r.setReportCount(1);
        r.setStatus("pending");
        r.setCreatedAt(Instant.now());
        return r;
    }

    private static String extractJsonString(String json, String key, String defaultVal) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start == -1) return defaultVal;
        start += marker.length();
        int end = json.indexOf("\"", start);
        return end != -1 ? json.substring(start, end) : defaultVal;
    }

    private static int extractJsonInt(String json, String key, int defaultVal) {
        try {
            String marker = "\"" + key + "\":";
            int start = json.indexOf(marker);
            if (start == -1) return defaultVal;
            start += marker.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static double extractJsonDouble(String json, String key, double defaultVal) {
        try {
            String marker = "\"" + key + "\":";
            int start = json.indexOf(marker);
            if (start == -1) return defaultVal;
            start += marker.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}

