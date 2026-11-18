package com.cityfix.heap;

import com.cityfix.model.RepairTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class CustomMaxHeapTest {

    private CustomMaxHeap heap;

    @BeforeEach
    public void setUp() {
        heap = new CustomMaxHeap(4); // small initial capacity to test dynamic resizing
    }

    @Test
    @DisplayName("Verify custom urgency score formula: (severity * 0.5) + (traffic * 0.3) + (reports * 0.2) + elapsed penalty")
    public void testUrgencyCalculation() {
        Instant now = Instant.now();
        RepairTask task = new RepairTask("RT-TEST", "Road Damage", 8, 6, 5, now);

        // Expected: (8 * 0.5) + (6 * 0.3) + (5 * 0.2) + 0 = 4.0 + 1.8 + 1.0 = 6.8
        double score = task.calculateUrgencyScore();
        assertEquals(6.8, score, 0.01);

        // Test elapsed time penalty (10 hours ago -> 10 * 0.05 = 0.5)
        Instant tenHoursAgo = now.minus(10, ChronoUnit.HOURS);
        RepairTask agedTask = new RepairTask("RT-AGED", "Water Logging", 8, 6, 5, tenHoursAgo);
        assertEquals(7.3, agedTask.calculateUrgencyScore(), 0.05);
    }

    @Test
    @DisplayName("Verify CustomMaxHeap.extractMax() strictly returns tasks in descending urgency score")
    public void testExtractMaxOrder() {
        Instant now = Instant.now();

        RepairTask low = new RepairTask("RT-LOW", "Streetlight", 3, 2, 1, now);    // Score: 1.5 + 0.6 + 0.2 = 2.3
        RepairTask med = new RepairTask("RT-MED", "Pothole", 6, 5, 3, now);        // Score: 3.0 + 1.5 + 0.6 = 5.1
        RepairTask high = new RepairTask("RT-HIGH", "Road Damage", 9, 8, 10, now);  // Score: 4.5 + 2.4 + 2.0 = 8.9

        // Insert in arbitrary order
        heap.insert(med);
        heap.insert(low);
        heap.insert(high);

        assertEquals(3, heap.size());

        RepairTask max1 = heap.extractMax();
        assertEquals("RT-HIGH", max1.getTaskId(), "First extraction must be highest urgency");

        RepairTask max2 = heap.extractMax();
        assertEquals("RT-MED", max2.getTaskId(), "Second extraction must be medium urgency");

        RepairTask max3 = heap.extractMax();
        assertEquals("RT-LOW", max3.getTaskId(), "Third extraction must be lowest urgency");

        assertTrue(heap.isEmpty());
    }

    @Test
    @DisplayName("Verify dynamic capacity expansion of internal array")
    public void testDynamicResizing() {
        Instant now = Instant.now();
        // Insert 20 tasks into a heap with initial capacity of 4
        for (int i = 1; i <= 20; i++) {
            heap.insert(new RepairTask("RT-" + i, "Damage", (i % 10) + 1, (i % 10) + 1, i, now));
        }

        assertEquals(20, heap.size());

        // Draining must remain strictly sorted
        double prevScore = Double.MAX_VALUE;
        while (!heap.isEmpty()) {
            RepairTask t = heap.extractMax();
            double score = t.calculateUrgencyScore();
            assertTrue(score <= prevScore, "Extracted score " + score + " must be <= previous score " + prevScore);
            prevScore = score;
        }
    }

    @Test
    @DisplayName("Verify NoSuchElementException on empty heap extraction")
    public void testEmptyHeapException() {
        assertTrue(heap.isEmpty());
        assertThrows(NoSuchElementException.class, () -> heap.extractMax());
        assertThrows(NoSuchElementException.class, () -> heap.peekMax());
    }

    @Test
    @DisplayName("Verify extractAllSorted() drains non-destructively via clone")
    public void testExtractAllSorted() {
        Instant now = Instant.now();
        heap.insert(new RepairTask("T1", "A", 2, 2, 1, now));
        heap.insert(new RepairTask("T2", "B", 8, 8, 5, now));
        heap.insert(new RepairTask("T3", "C", 5, 5, 2, now));

        List<RepairTask> sorted = heap.extractAllSorted();
        assertEquals(3, sorted.size());
        assertEquals("T2", sorted.get(0).getTaskId());
        assertEquals("T3", sorted.get(1).getTaskId());
        assertEquals("T1", sorted.get(2).getTaskId());

        // Original heap size remains intact
        assertEquals(3, heap.size());
    }
}

