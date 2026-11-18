# CityFix: Municipal Infrastructure Repair Prioritizer

A production-ready municipal infrastructure management system engineered to ingest citizen reports, persist geospatial incident records in SQL, and dynamically prioritize repair dispatch orders using a **from-scratch Binary Max-Heap algorithmic engine** (strictly avoiding `java.util.PriorityQueue`).

---

## 1. Architectural Specification

```
                                  [ Citizen Issue Portal ]
                                            │
                                            ▼
                                   [ REST API / JDBC ]
                                            │
                                            ▼
                                  ┌───────────────────┐
                                  │   SQL Database    │
                                  │ (reports & users) │
                                  └─────────┬─────────┘
                                            │
                                            ▼
                         ┌─────────────────────────────────────┐
                         │   CustomMaxHeap (From Scratch)      │
                         │                                     │
                         │  • heapifyUp() / heapifyDown()      │
                         │  • calculateUrgencyScore()          │
                         │  • extractMax() [Priority Dispatch] │
                         └──────────────────┬──────────────────┘
                                            │
                                            ▼
                           [ CityFix Municipal Admin UI ]
                             (Dashboard / Repair Tasks)
```

---

## 2. Core Algorithmic Engine (`CustomMaxHeap.java`)

### A. The Urgency Formula
Each `RepairTask` evaluates its priority dynamically via `calculateUrgencyScore()`:
$$\text{Urgency Score} = (\text{Severity} \times 0.5) + (\text{Traffic Volume} \times 0.3) + (\text{Report Count} \times 0.2) + \text{Time Elapsed Penalty}$$

*   **Severity Rating ($1 - 10$):** $50\%$ weight. Structural danger, cave-ins, exposed electrical cables.
*   **Traffic Volume ($1 - 10$):** $30\%$ weight. Arterial highways and intersections receive higher priority than quiet cul-de-sacs.
*   **Report Count (Integer $\ge 1$):** $20\%$ weight. Citizen call frequency indicates community urgency.
*   **Time Elapsed Penalty:** $\min(2.0, \text{hoursElapsed} \times 0.05)$. Untreated backlog issues progressively accumulate penalty points so that low-traffic issues do not suffer starvation.

### B. Custom Binary Max-Heap Properties
*   **No Built-In Queues:** Fully standalone array-based complete binary tree implementation.
*   **Dynamic Resizing:** Doubles internal capacity on saturation without memory leaks.
*   **Time Complexities:**
    *   `insert(RepairTask)`: $\mathcal{O}(\log N)$
    *   `extractMax()`: $\mathcal{O}(\log N)$
    *   `peekMax()`: $\mathcal{O}(1)$
    *   `buildHeap(List<RepairTask>)`: $\mathcal{O}(N)$ using Floyd’s downward heap construction.

---

## 3. Database Schema (`schema.sql`)

PostgreSQL & MySQL compliant DDL with B-tree indexes for accelerated status lookups and category breakdowns:

```sql
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('citizen', 'admin')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reports (
    report_id SERIAL PRIMARY KEY,
    task_code VARCHAR(20) UNIQUE,
    user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    issue_type VARCHAR(100) NOT NULL,
    location_name VARCHAR(255) NOT NULL,
    description TEXT,
    latitude DECIMAL(10, 6) NOT NULL,
    longitude DECIMAL(10, 6) NOT NULL,
    severity_rating INT NOT NULL CHECK (severity_rating BETWEEN 1 AND 10),
    traffic_impact INT NOT NULL CHECK (traffic_impact BETWEEN 1 AND 10),
    report_count INT NOT NULL DEFAULT 1 CHECK (report_count >= 1),
    status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'in-progress', 'resolved')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_issue_type ON reports(issue_type);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);
CREATE INDEX idx_reports_severity_traffic ON reports(severity_rating, traffic_impact);
```

---

## 4. UI/UX Design System (Extracted from Provided Mockups)

The user interface matches the provided mockups to the pixel:

| UI Attribute | Extracted Value | Usage |
| :--- | :--- | :--- |
| **Canvas Background** | `#F8FAFC` | Main app backdrop |
| **Surface Cards** | `#FFFFFF` | Card panels, table backgrounds |
| **Primary Accent** | `#2563EB` | Active nav, "+ New Task", primary buttons |
| **High Priority Badge** | `#FEE2E2` / `#EF4444` | Urgency scores $\ge 80$, severe road damage |
| **Medium Priority** | `#FEF3C7` / `#D97706` | Urgency scores $60 - 79$ |
| **Low Priority** | `#D1FAE5` / `#059669` | Urgency scores $< 60$ |
| **Pending Status** | `#F1F5F9` / `#64748B` | Unassigned tasks |
| **In Progress Status** | `#DBEAFE` / `#2563EB` | Active crew dispatch |
| **Resolved Status** | `#D1FAE5` / `#059669` | Completed infrastructure fixes |

### Included Views:
1.  **Dashboard (`index.html`):** 4 KPI summary cards, interactive Leaflet jurisdiction map, top 5 `CustomMaxHeap` repair queue, urgency factor weights, and recent reports table.
2.  **Repair Tasks (`tasks.html`):** 5 KPI stat cards, multi-facet filtering toolbar, sortable repair tasks table, and fixed **Task Details** drawer with score `95`, suggested action checklist, and live status mutation.
3.  **Reports (`reports.html`):** 4 KPI metrics, Leaflet map with geo-pins, category breakdown list (Road Damage 40%, Broken Streetlight 31%, Water Logging 21%), and complete historical report logs.
4.  **Citizen Portal (`submit.html`):** Citizen-facing submission form with interactive click-to-pin Leaflet map picker, severity & traffic sliders, and a **Live Max-Heap Algorithmic Projection Preview**.

---

## 5. How to Run the Application

### Option A: Spring Boot
```bash
cd cityfix-infrastructure-prioritizer
mvn spring-boot:run
```

### Option B: Pure Java (Zero External Dependencies)
```bash
cd cityfix-infrastructure-prioritizer
javac -d bin src/main/java/com/cityfix/**/*.java
java -cp bin com.cityfix.CityFixStandaloneServer
```

### Option C: Instant Local Preview Server (Node.js)
```bash
cd cityfix-infrastructure-prioritizer
node server.js
```
Then navigate your browser to `http://localhost:3000/`.

