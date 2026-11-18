package com.cityfix.model;

import java.time.Instant;

/**
 * Entity representing an infrastructure failure report in the reports database table.
 */
public class Report {
    private int reportId;
    private String taskCode;
    private Integer userId;
    private String issueType;
    private String locationName;
    private String description;
    private double latitude;
    private double longitude;
    private int severityRating;  // 1-10
    private int trafficImpact;   // 1-10
    private int reportCount;
    private String status;       // 'pending', 'in-progress', 'resolved'
    private Instant createdAt;

    public Report() {
        this.reportCount = 1;
        this.status = "pending";
        this.createdAt = Instant.now();
    }

    public Report(int reportId, String taskCode, Integer userId, String issueType,
                  String locationName, String description, double latitude, double longitude,
                  int severityRating, int trafficImpact, int reportCount, String status, Instant createdAt) {
        this.reportId = reportId;
        this.taskCode = taskCode;
        this.userId = userId;
        this.issueType = issueType;
        this.locationName = locationName;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.severityRating = severityRating;
        this.trafficImpact = trafficImpact;
        this.reportCount = reportCount;
        this.status = status != null ? status : "pending";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    /**
     * Converts this database report record into a heap-compatible RepairTask object.
     */
    public RepairTask toRepairTask() {
        String code = (taskCode != null && !taskCode.isEmpty()) ? taskCode : ("RT-" + reportId);
        String reportedBy = (userId != null) ? "Citizen #" + userId : "Citizen";
        return new RepairTask(
                code,
                issueType,
                locationName,
                description,
                latitude,
                longitude,
                severityRating,
                trafficImpact,
                reportCount,
                status,
                createdAt,
                reportedBy
        );
    }

    // Getters and Setters
    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getSeverityRating() { return severityRating; }
    public void setSeverityRating(int severityRating) { this.severityRating = severityRating; }

    public int getTrafficImpact() { return trafficImpact; }
    public void setTrafficImpact(int trafficImpact) { this.trafficImpact = trafficImpact; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

