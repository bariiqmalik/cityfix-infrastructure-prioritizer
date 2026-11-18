/**
 * CustomMaxHeap Client-Side Algorithmic Engine.
 * Replicates the exact custom binary MaxHeap logic from CustomMaxHeap.java.
 *
 * Enforces formula: (severity * 0.5) + (trafficVolume * 0.3) + (reportCount * 0.2) + (timeElapsedPenalty)
 * No reliance on built-in priority queue libraries.
 */

class RepairTask {
  constructor(data) {
    this.taskId = data.taskId || ('RT-' + Math.floor(1000 + Math.random() * 9000));
    this.issueType = data.issueType || 'Road Damage';
    this.location = data.location || data.locationName || 'Unspecified';
    this.description = data.description || '';
    this.latitude = parseFloat(data.latitude) || 28.6139;
    this.longitude = parseFloat(data.longitude) || 77.2090;
    this.severity = Math.max(1, Math.min(10, parseInt(data.severity || data.severityRating) || 5));
    this.trafficVolume = Math.max(1, Math.min(10, parseInt(data.trafficVolume || data.trafficImpact) || 5));
    this.reportCount = Math.max(1, parseInt(data.reportCount) || 1);
    this.status = data.status || 'pending';
    this.reportedBy = data.reportedBy || 'Citizen';
    this.timestamp = data.timestamp ? new Date(data.timestamp) : (data.createdAt ? new Date(data.createdAt) : new Date());
  }

  calculateUrgencyScore() {
    const hoursElapsed = Math.max(0, (Date.now() - this.timestamp.getTime()) / (1000 * 60 * 60));
    const timeElapsedPenalty = Math.min(2.0, hoursElapsed * 0.05);

    // Architectural formula: (severity * 0.5) + (trafficVolume * 0.3) + (reportCount * 0.2) + (timeElapsedPenalty)
    return (this.severity * 0.5) + (this.trafficVolume * 0.3) + (this.reportCount * 0.2) + timeElapsedPenalty;
  }

  getDisplayScore() {
    const raw = this.calculateUrgencyScore();
    const score = Math.round(raw * 10.0);
    return Math.max(10, Math.min(100, score));
  }

  getPriorityLevel() {
    const score = this.getDisplayScore();
    if (score >= 80) return 'High';
    if (score >= 60) return 'Medium';
    return 'Low';
  }
}

class CustomMaxHeap {
  constructor(initialTasks = []) {
    this.heap = [];
    if (initialTasks && initialTasks.length > 0) {
      this.heap = initialTasks.map(t => (t instanceof RepairTask ? t : new RepairTask(t)));
      // Floyd's O(N) heap construction
      for (let i = Math.floor((this.heap.length - 2) / 2); i >= 0; i--) {
        this.heapifyDown(i);
      }
    }
  }

  parent(i) { return Math.floor((i - 1) / 2); }
  left(i) { return 2 * i + 1; }
  right(i) { return 2 * i + 2; }

  swap(i, j) {
    const temp = this.heap[i];
    this.heap[i] = this.heap[j];
    this.heap[j] = temp;
  }

  insert(task) {
    const item = task instanceof RepairTask ? task : new RepairTask(task);
    this.heap.push(item);
    this.heapifyUp(this.heap.length - 1);
  }

  extractMax() {
    if (this.isEmpty()) return null;
    const max = this.heap[0];
    const last = this.heap.pop();
    if (this.heap.length > 0) {
      this.heap[0] = last;
      this.heapifyDown(0);
    }
    return max;
  }

  peekMax() {
    return this.isEmpty() ? null : this.heap[0];
  }

  heapifyUp(i) {
    while (i > 0) {
      const p = this.parent(i);
      if (this.heap[i].calculateUrgencyScore() > this.heap[p].calculateUrgencyScore()) {
        this.swap(i, p);
        i = p;
      } else {
        break;
      }
    }
  }

  heapifyDown(i) {
    let largest = i;
    const n = this.heap.length;

    while (true) {
      const l = this.left(i);
      const r = this.right(i);

      if (l < n && this.heap[l].calculateUrgencyScore() > this.heap[largest].calculateUrgencyScore()) {
        largest = l;
      }
      if (r < n && this.heap[r].calculateUrgencyScore() > this.heap[largest].calculateUrgencyScore()) {
        largest = r;
      }

      if (largest !== i) {
        this.swap(i, largest);
        i = largest;
      } else {
        break;
      }
    }
  }

  extractAllSorted() {
    const tempHeap = new CustomMaxHeap([...this.heap]);
    const sorted = [];
    while (!tempHeap.isEmpty()) {
      sorted.push(tempHeap.extractMax());
    }
    return sorted;
  }

  size() { return this.heap.length; }
  isEmpty() { return this.heap.length === 0; }
  clear() { this.heap = []; }
}

// Export for module or global window scope
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { RepairTask, CustomMaxHeap };
} else {
  window.RepairTask = RepairTask;
  window.CustomMaxHeap = CustomMaxHeap;
}

