-- ============================================================================
-- Municipal Infrastructure Repair Prioritizer (CityFix)
-- Database Initialization Script (PostgreSQL & MySQL Compatible)
-- ============================================================================

-- Drop existing tables if they exist
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. Table: users
-- Represents citizens reporting infrastructure issues and municipal admins
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('citizen', 'admin')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Table: reports
-- Stores infrastructure failure reports submitted by citizens and field staff
CREATE TABLE reports (
    report_id SERIAL PRIMARY KEY,
    task_code VARCHAR(20) UNIQUE,              -- Display code like 'RT-1042' or '#R-1042'
    user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    issue_type VARCHAR(100) NOT NULL,          -- e.g., 'Road Damage', 'Broken Streetlight', 'Water Logging'
    location_name VARCHAR(255) NOT NULL,       -- e.g., 'Maple St & 5th Ave'
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

-- 3. Optimization Indexes for High Performance
-- Ensuring rapid filtering and heap reconstruction by priority status and issue category
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_issue_type ON reports(issue_type);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);
CREATE INDEX idx_reports_severity_traffic ON reports(severity_rating, traffic_impact);

-- ----------------------------------------------------------------------------
-- Seed Initial Users
-- ----------------------------------------------------------------------------
INSERT INTO users (user_id, name, role) VALUES
(1, 'John Doe', 'admin'),
(2, 'Sarah Jenkins', 'citizen'),
(3, 'Michael Chang', 'citizen'),
(4, 'Emily Rodriguez', 'citizen'),
(5, 'David Kim', 'citizen');

-- ----------------------------------------------------------------------------
-- Seed Initial Municipal Infrastructure Reports
-- Coordinates centered around India metropolitan jurisdictions (~28.6139, 77.2090)
-- ----------------------------------------------------------------------------
INSERT INTO reports (
    report_id, task_code, user_id, issue_type, location_name, description, 
    latitude, longitude, severity_rating, traffic_impact, report_count, status, created_at
) VALUES
(1042, 'RT-1042', 2, 'Road Damage', 'Connaught Place & Barakhamba Rd, New Delhi', 
 'Deep pothole and surface damage near Rajiv Chowk Metro Gate 4. Causing minor traffic disruption.', 
 28.631500, 77.216700, 9, 8, 12, 'pending', '2025-04-26 09:12:00'),

(1041, 'RT-1041', 3, 'Broken Streetlight', 'MG Road & Brigade Rd, Bengaluru', 
 'Streetlight flickering violently, then going dark at night creating hazard at intersection.', 
 12.974500, 77.608000, 6, 9, 8, 'in-progress', '2025-04-26 08:47:00'),

(1040, 'RT-1040', 4, 'Water Logging', 'Ring Road near AIIMS, New Delhi', 
 'Major storm drain blockage after monsoon rain. Water accumulating across two lanes.', 
 28.568400, 77.210400, 8, 7, 7, 'in-progress', '2025-04-26 08:21:00'),

(1039, 'RT-1039', 5, 'Road Damage', 'Marine Drive & Nariman Point, Mumbai', 
 'Deep asphalt trench following utility excavation. Vehicles bottoming out.', 
 18.922000, 72.825800, 7, 8, 5, 'pending', '2025-04-26 07:56:00'),

(1038, 'RT-1038', 2, 'Broken Streetlight', 'Park Street & Chowringhee, Kolkata', 
 'Pedestrian crossing lamp fixture damaged by fallen branch.', 
 22.551300, 88.352400, 4, 3, 2, 'resolved', '2025-04-26 06:32:00'),

(1037, 'RT-1037', 3, 'Water Logging', 'Anna Salai (Mount Road), Chennai', 
 'Clogged culvert near metro station entrance causing pedestrian pathway overflow.', 
 13.060400, 80.249600, 5, 5, 4, 'pending', '2025-04-26 03:48:00'),

(1036, 'RT-1036', 4, 'Road Damage', 'ITO Junction & Vikas Marg, New Delhi', 
 'Manhole cover displaced and loose rim creating loud clanking and tire puncture risk.', 
 28.630400, 77.241000, 8, 9, 6, 'in-progress', '2025-04-26 03:48:00'),

(1035, 'RT-1035', 5, 'Broken Streetlight', 'Bandra-Kurla Complex (BKC), Mumbai', 
 'Series of 3 decorative street lamps non-functional along transit corridor.', 
 19.065700, 72.868700, 5, 6, 3, 'pending', '2025-04-26 02:11:00');

-- Reset sequences for PostgreSQL if applicable
SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
SELECT setval('reports_report_id_seq', (SELECT MAX(report_id) FROM reports));
