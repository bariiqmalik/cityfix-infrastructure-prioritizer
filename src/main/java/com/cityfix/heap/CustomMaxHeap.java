package com.cityfix.heap;

import com.cityfix.model.RepairTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * CustomMaxHeap - A custom binary Max-Heap data structure implemented from scratch.
 * Strictly avoids java.util.PriorityQueue per architectural specification.
 *
 * Maintains the max-heap invariant based on RepairTask.calculateUrgencyScore():
 * For any node at index i, urgencyScore(parent(i)) >= urgencyScore(i).
 */
public class CustomMaxHeap {

    private static final int DEFAULT_INITIAL_CAPACITY = 32;

    // Internal array representation of the complete binary tree
    private RepairTask[] heap;
    private int size;

    /**
     * Initializes an empty CustomMaxHeap with default capacity.
     */
    public CustomMaxHeap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Initializes an empty CustomMaxHeap with custom initial capacity.
     */
    public CustomMaxHeap(int initialCapacity) {
        if (initialCapacity <= 0) {
            initialCapacity = DEFAULT_INITIAL_CAPACITY;
        }
        this.heap = new RepairTask[initialCapacity];
        this.size = 0;
    }

    /**
     * Constructs a Max-Heap from an existing collection of RepairTask objects
     * using Floyd's linear-time O(N) build-heap algorithm.
     *
     * @param tasks initial list of repair tasks
     */
    public CustomMaxHeap(List<RepairTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            this.heap = new RepairTask[DEFAULT_INITIAL_CAPACITY];
            this.size = 0;
        } else {
            this.size = tasks.size();
            this.heap = new RepairTask[Math.max(DEFAULT_INITIAL_CAPACITY, size * 2)];
            for (int i = 0; i < size; i++) {
                this.heap[i] = tasks.get(i);
            }
            // Floyd's algorithm: start heapifying down from last non-leaf node up to root
            for (int i = parentIndex(size - 1); i >= 0; i--) {
                heapifyDown(i);
            }
        }
    }

    /**
     * Inserts a new repair task into the Max-Heap and restores the heap property
     * in O(log N) time via upward heapification.
     *
     * @param task the repair task to prioritize
     */
    public void insert(RepairTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Cannot insert null RepairTask into heap");
        }
        ensureCapacity();
        heap[size] = task;
        int currentIndex = size;
        size++;
        heapifyUp(currentIndex);
    }

    /**
     * Retrieves and removes the task with the highest urgency score from the heap.
     * Time Complexity: O(log N).
     *
     * @return the most urgent RepairTask for municipal workers to address
     * @throws NoSuchElementException if the heap is empty
     */
    public RepairTask extractMax() {
        if (isEmpty()) {
            throw new NoSuchElementException("Cannot extractMax from an empty CustomMaxHeap");
        }
        RepairTask maxTask = heap[0];
        // Move the last element to root and shrink heap
        heap[0] = heap[size - 1];
        heap[size - 1] = null; // facilitate garbage collection
        size--;

        if (size > 0) {
            heapifyDown(0);
        }
        return maxTask;
    }

    /**
     * Peeks at the most urgent task without removing it.
     * Time Complexity: O(1).
     *
     * @return highest priority task
     * @throws NoSuchElementException if heap is empty
     */
    public RepairTask peekMax() {
        if (isEmpty()) {
            throw new NoSuchElementException("CustomMaxHeap is empty");
        }
        return heap[0];
    }

    /**
     * Restores max-heap property upwards from given index.
     * Compares child urgency score with parent; swaps if child > parent.
     *
     * @param index starting node index
     */
    public void heapifyUp(int index) {
        while (index > 0) {
            int pIndex = parentIndex(index);
            double currentUrgency = heap[index].calculateUrgencyScore();
            double parentUrgency = heap[pIndex].calculateUrgencyScore();

            if (currentUrgency > parentUrgency) {
                swap(index, pIndex);
                index = pIndex;
            } else {
                break;
            }
        }
    }

    /**
     * Restores max-heap property downwards from given index.
     * Compares parent with left and right children; swaps with the largest child
     * until the parent is greater than or equal to both children.
     *
     * @param index starting node index
     */
    public void heapifyDown(int index) {
        int largest = index;

        while (true) {
            int left = leftChildIndex(index);
            int right = rightChildIndex(index);

            if (left < size && heap[left].calculateUrgencyScore() > heap[largest].calculateUrgencyScore()) {
                largest = left;
            }

            if (right < size && heap[right].calculateUrgencyScore() > heap[largest].calculateUrgencyScore()) {
                largest = right;
            }

            if (largest != index) {
                swap(index, largest);
                index = largest;
            } else {
                break;
            }
        }
    }

    /**
     * Clones and drains this heap to return all repair tasks strictly sorted
     * in descending order of urgency score.
     *
     * @return list of tasks in prioritized order (highest urgency first)
     */
    public List<RepairTask> extractAllSorted() {
        // Create an isolated copy to prevent draining the original heap state
        CustomMaxHeap tempHeap = new CustomMaxHeap(this.size);
        for (int i = 0; i < this.size; i++) {
            tempHeap.insert(this.heap[i]);
        }

        List<RepairTask> sortedList = new ArrayList<>(this.size);
        while (!tempHeap.isEmpty()) {
            sortedList.add(tempHeap.extractMax());
        }
        return sortedList;
    }

    /**
     * Current count of elements in the heap.
     */
    public int size() {
        return size;
    }

    /**
     * Returns true if heap contains no tasks.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Clears all elements from the heap.
     */
    public void clear() {
        Arrays.fill(heap, 0, size, null);
        size = 0;
    }

    // --- Internal Helpers ---

    private int parentIndex(int i) {
        return (i - 1) / 2;
    }

    private int leftChildIndex(int i) {
        return 2 * i + 1;
    }

    private int rightChildIndex(int i) {
        return 2 * i + 2;
    }

    private void swap(int i, int j) {
        RepairTask temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    private void ensureCapacity() {
        if (size >= heap.length) {
            int newCapacity = heap.length * 2;
            heap = Arrays.copyOf(heap, newCapacity);
        }
    }

    /**
     * Raw array snapshot for debugging and inspection.
     */
    public RepairTask[] toArray() {
        return Arrays.copyOf(heap, size);
    }
}

