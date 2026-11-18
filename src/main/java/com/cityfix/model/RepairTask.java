package com.cityfix.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Model representing an infrastructure repair task in the municipal workflow.
 * Encapsulates the algorithmic priority calculation based on severity, traffic volume,
 * report frequency, and elapsed time penalty.
 */
public class RepairTask implements Comparable<RepairTask> {

    private String taskId;          // e.g. "RT-1042"
    private String issueType;       // e.g. "Road Damage", "Broken Streetlight", "Water Logging"
    private int severity;           // 1 - 10 scale
    private int trafficVolume;      // 1 - 10 scale (traffic impact)
    private int reportCount;        // integer frequency count
    private Instant timestamp;      // creation / last update timestamp

    // Extended domain fields for full operational UI support
    private String location;        // e.g. "Maple St & 5th Ave"
    private String description;     // e.g. "Pothole and surface damage..."
    private double latitude;
    private double longitude;
    private String status;          // "pending", "in-progress", "resolved"
    private String reportedBy;      // e.g. "Citizen #4587"

    public RepairTask() {
        this.timestamp = Instant.now();
        this.reportCount = 1;
        this.status = "pending";
    }

    public RepairTask(String taskId, String issueType, int severity, int trafficVolume, int reportCount, Instant timestamp) {
        this.taskId = taskId;
        this.issueType = issueType;
        this.severity = severity;
        this.trafficVolume = trafficVolume;
        this.reportCount = reportCount;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.status = "pending";
    }

    public RepairTask(String taskId, String issueType, String location, String description,
                      double latitude, double longitude, int severity, int trafficVolume,
                      int reportCount, String status, Instant timestamp, String reportedBy) {
        this.taskId = taskId;
        this.issueType = issueType;
        this.location = location;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.severity = severity;
        this.trafficVolume = trafficVolume;
        this.reportCount = reportCount;
        this.status = status != null ? status : "pending";
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.reportedBy = reportedBy != null ? reportedBy : "Citizen";
    }

    /**
     * Calculates the urgency score for the municipal repair task.
     * Core formula specified:
     * (severity * 0.5) + (trafficVolume * 0.3) + (reportCount * 0.2) + (timeElapsedPenalty)
     *
     * Older untreated issues progressively accumulate a time-elapsed penalty to
     * prevent neglect of lower-traffic repairs.
     *
     * @return raw urgency score (double precision)
     */
    public double calculateUrgencyScore() {
        // Enforce input bounds
        int clampedSeverity = Math.max(1, Math.min(10, severity));
        int clampedTraffic = Math.max(1, Math.min(10, trafficVolume));
        int clampedReports = Math.max(1, reportCount);

        // Time elapsed penalty calculation:
        // Issues accumulate 0.05 penalty points per hour since reporting, capped at 2.0
        double hoursElapsed = 0.0;
        if (timestamp != null) {
            long seconds = Math.max(0, Duration.between(timestamp, Instant.now()).getSeconds());
            hoursElapsed = seconds / 3600.0;
        }
        double timeElapsedPenalty = Math.min(2.0, hoursElapsed * 0.05);

        // Exact formula evaluation
        return (clampedSeverity * 0.5) + (clampedTraffic * 0.3) + (clampedReports * 0.2) + timeElapsedPenalty;
    }

    /**
     * Normalized display score on a standard 0 - 100 integer scale matching
     * the Municipal Dashboard UI (e.g. 95, 88, 82, 76, 71).
     *
     * @return scaled integer score between 0 and 100
     */
    public int getDisplayScore() {
        double raw = calculateUrgencyScore();
        // Base scale: 10 * raw score, clamped to [10, 100]
        int score = (int) Math.round(raw * 10.0);
        return Math.max(10, Math.min(100, score));
    }

    /**
     * Returns priority level category ('High', 'Medium', 'Low') matching the UI pill tags.
     */
    public String getPriorityLevel() {
        int score = getDisplayScore();
        if (score >= 80) return "High";
        if (score >= 60) return "Medium";
        return "Low";
    }

    @Override
    public int compareTo(RepairTask other) {
        if (other == null) return 1;
        return Double.compare(this.calculateUrgencyScore(), other.calculateUrgencyScore());
    }

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }

    public int getTrafficVolume() { return trafficVolume; }
    public void setTrafficVolume(int trafficVolume) { this.trafficVolume = trafficVolume; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepairTask)) return false;
        RepairTask that = (RepairTask) o;
        return Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return "RepairTask{" +
                "taskId='" + taskId + '\'' +
                ", issueType='" + issueType + '\'' +
                ", severity=" + severity +
                ", trafficVolume=" + trafficVolume +
                ", reportCount=" + reportCount +
                ", urgencyScore=" + String.format("%.2f", calculateUrgencyScore()) +
                ", displayScore=" + getDisplayScore() +
                ", status='" + status + '\'' +
                '}';
    }
}

