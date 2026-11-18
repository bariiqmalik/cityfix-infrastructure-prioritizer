package com.cityfix.controller;

import com.cityfix.model.RepairTask;
import com.cityfix.model.Report;
import com.cityfix.service.PrioritizerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final PrioritizerService prioritizerService;

    public ApiController(PrioritizerService prioritizerService) {
        this.prioritizerService = prioritizerService;
    }

    /**
     * Requirement: Returns repair tasks strictly extracted from CustomMaxHeap
     * in descending order of urgency score.
     */
    @GetMapping("/tasks/prioritized")
    public ResponseEntity<List<RepairTask>> getPrioritizedTasks() {
        return ResponseEntity.ok(prioritizerService.getPrioritizedQueue());
    }

    /**
     * Requirement: Extracts the single most urgent task using CustomMaxHeap.extractMax().
     */
    @PostMapping("/tasks/extract-max")
    public ResponseEntity<?> extractMaxTask() {
        try {
            RepairTask maxTask = prioritizerService.extractNextUrgentTask();
            return ResponseEntity.ok(maxTask);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Returns all infrastructure reports for Reports view.
     */
    @GetMapping("/reports")
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(prioritizerService.getAllReports());
    }

    /**
     * Citizen Form Submission endpoint.
     */
    @PostMapping("/reports")
    public ResponseEntity<?> submitCitizenReport(@RequestBody Report report) {
        try {
            if (report.getIssueType() == null || report.getLocationName() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "issueType and locationName are required"));
            }
            Report created = prioritizerService.submitReport(report);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update status of a report ("pending", "in-progress", "resolved").
     */
    @PatchMapping("/reports/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable int id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
        }
        boolean ok = prioritizerService.updateTaskStatus(id, newStatus);
        return ok ? ResponseEntity.ok(Map.of("success", true)) : ResponseEntity.badRequest().build();
    }

    /**
     * KPI Metrics summary for the dashboard cards.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(prioritizerService.getMetrics());
    }
}

