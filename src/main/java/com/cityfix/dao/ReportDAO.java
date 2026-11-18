package com.cityfix.dao;

import com.cityfix.model.RepairTask;
import com.cityfix.model.Report;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for municipal reports.
 */
public interface ReportDAO {

    /**
     * Inserts a new infrastructure report into the database.
     * @param report the report entity to persist
     * @return generated report_id
     */
    int create(Report report);

    /**
     * Finds a single report by its primary key.
     */
    Optional<Report> findById(int reportId);

    /**
     * Finds a report by its task code (e.g. "RT-1042").
     */
    Optional<Report> findByTaskCode(String taskCode);

    /**
     * Retrieves all reports ordered by creation time descending.
     */
    List<Report> findAll();

    /**
     * Filters reports by status ('pending', 'in-progress', 'resolved').
     */
    List<Report> findByStatus(String status);

    /**
     * Filters reports by issue category ('Road Damage', 'Broken Streetlight', etc.).
     */
    List<Report> findByIssueType(String issueType);

    /**
     * Updates the status of an ongoing repair report.
     */
    boolean updateStatus(int reportId, String newStatus);

    /**
     * Increments report frequency count when multiple citizens report the same issue.
     */
    boolean incrementReportCount(int reportId);

    /**
     * Deletes a report by id.
     */
    boolean delete(int reportId);

    /**
     * Extracts active infrastructure reports (pending & in-progress) and maps
     * them into RepairTask domain objects ready to be loaded into the CustomMaxHeap.
     */
    List<RepairTask> getActiveRepairTasks();
}

