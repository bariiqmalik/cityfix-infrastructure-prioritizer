package com.cityfix.dao;

import com.cityfix.model.RepairTask;
import com.cityfix.model.Report;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure JDBC Implementation of ReportDAO.
 * Interacts directly with SQL database tables (PostgreSQL, MySQL, H2).
 */
public class ReportDAOImpl implements ReportDAO {

    @Override
    public int create(Report report) {
        String sql = "INSERT INTO reports (user_id, issue_type, location_name, description, " +
                     "latitude, longitude, severity_rating, traffic_impact, report_count, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (report.getUserId() != null) {
                ps.setInt(1, report.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, report.getIssueType());
            ps.setString(3, report.getLocationName() != null ? report.getLocationName() : "Unspecified Location");
            ps.setString(4, report.getDescription());
            ps.setBigDecimal(5, java.math.BigDecimal.valueOf(report.getLatitude()));
            ps.setBigDecimal(6, java.math.BigDecimal.valueOf(report.getLongitude()));
            ps.setInt(7, report.getSeverityRating());
            ps.setInt(8, report.getTrafficImpact());
            ps.setInt(9, Math.max(1, report.getReportCount()));
            ps.setString(10, report.getStatus() != null ? report.getStatus() : "pending");
            ps.setTimestamp(11, Timestamp.from(report.getCreatedAt() != null ? report.getCreatedAt() : Instant.now()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        report.setReportId(generatedId);
                        
                        // Assign task code if not present (e.g., RT-1043)
                        String taskCode = "RT-" + generatedId;
                        report.setTaskCode(taskCode);
                        updateTaskCode(generatedId, taskCode);
                        
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating report in DB: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    private void updateTaskCode(int reportId, String taskCode) {
        String sql = "UPDATE reports SET task_code = ? WHERE report_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskCode);
            ps.setInt(2, reportId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Warning: Could not update task_code: " + e.getMessage());
        }
    }

    @Override
    public Optional<Report> findById(int reportId) {
        String sql = "SELECT * FROM reports WHERE report_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReport(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding report by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Report> findByTaskCode(String taskCode) {
        String sql = "SELECT * FROM reports WHERE task_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReport(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding report by taskCode: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Report> findAll() {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reports.add(mapResultSetToReport(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all reports: " + e.getMessage());
        }
        return reports;
    }

    @Override
    public List<Report> findByStatus(String status) {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE status = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching reports by status: " + e.getMessage());
        }
        return reports;
    }

    @Override
    public List<Report> findByIssueType(String issueType) {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE issue_type = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, issueType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching reports by issue type: " + e.getMessage());
        }
        return reports;
    }

    @Override
    public boolean updateStatus(int reportId, String newStatus) {
        String sql = "UPDATE reports SET status = ? WHERE report_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.toLowerCase());
            ps.setInt(2, reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating report status: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean incrementReportCount(int reportId) {
        String sql = "UPDATE reports SET report_count = report_count + 1 WHERE report_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error incrementing report count: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(int reportId) {
        String sql = "DELETE FROM reports WHERE report_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting report: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<RepairTask> getActiveRepairTasks() {
        List<RepairTask> tasks = new ArrayList<>();
        // Municipal prioritizer schedules 'pending' and 'in-progress' issues
        String sql = "SELECT r.*, u.name as user_name FROM reports r " +
                     "LEFT JOIN users u ON r.user_id = u.user_id " +
                     "WHERE r.status != 'resolved' " +
                     "ORDER BY r.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("report_id");
                String taskCode = rs.getString("task_code");
                if (taskCode == null || taskCode.isEmpty()) {
                    taskCode = "RT-" + id;
                }
                String issueType = rs.getString("issue_type");
                String location = rs.getString("location_name");
                String description = rs.getString("description");
                double lat = rs.getBigDecimal("latitude").doubleValue();
                double lng = rs.getBigDecimal("longitude").doubleValue();
                int severity = rs.getInt("severity_rating");
                int traffic = rs.getInt("traffic_impact");
                int reportCount = rs.getInt("report_count");
                String status = rs.getString("status");
                Timestamp ts = rs.getTimestamp("created_at");
                Instant createdAt = ts != null ? ts.toInstant() : Instant.now();
                String userName = rs.getString("user_name");
                String reportedBy = (userName != null && !userName.isEmpty()) ? userName : "Citizen #" + rs.getInt("user_id");

                RepairTask task = new RepairTask(
                        taskCode,
                        issueType,
                        location,
                        description,
                        lat,
                        lng,
                        severity,
                        traffic,
                        reportCount,
                        status,
                        createdAt,
                        reportedBy
                );
                tasks.add(task);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching active repair tasks: " + e.getMessage());
        }
        return tasks;
    }

    private Report mapResultSetToReport(ResultSet rs) throws SQLException {
        int id = rs.getInt("report_id");
        String taskCode = rs.getString("task_code");
        if (taskCode == null || taskCode.isEmpty()) {
            taskCode = "RT-" + id;
        }
        Integer userId = rs.getObject("user_id") != null ? rs.getInt("user_id") : null;
        String issueType = rs.getString("issue_type");
        String location = rs.getString("location_name");
        String description = rs.getString("description");
        double lat = rs.getBigDecimal("latitude") != null ? rs.getBigDecimal("latitude").doubleValue() : 0.0;
        double lng = rs.getBigDecimal("longitude") != null ? rs.getBigDecimal("longitude").doubleValue() : 0.0;
        int severity = rs.getInt("severity_rating");
        int traffic = rs.getInt("traffic_impact");
        int count = rs.getInt("report_count");
        String status = rs.getString("status");
        Timestamp ts = rs.getTimestamp("created_at");
        Instant createdAt = ts != null ? ts.toInstant() : Instant.now();

        return new Report(id, taskCode, userId, issueType, location, description, lat, lng, severity, traffic, count, status, createdAt);
    }
}

