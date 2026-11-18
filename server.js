/**
 * CityFix Local Web Server & REST API Simulator.
 * Allows immediate preview and interactive testing of the CityFix application.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const STATIC_DIR = path.join(__dirname, 'src', 'main', 'resources', 'static');

// In-Memory CustomMaxHeap mirror
const { CustomMaxHeap, RepairTask } = require(path.join(STATIC_DIR, 'js', 'maxheap-client.js'));

const SEED_TASKS = [
  { taskId: 'RT-1042', issueType: 'Road Damage', location: 'Connaught Place & Barakhamba Rd, New Delhi', description: 'Deep pothole and surface damage near Rajiv Chowk Metro Gate 4. Causing traffic disruption.', latitude: 28.6315, longitude: 77.2167, severity: 9, trafficVolume: 8, reportCount: 12, status: 'Pending', reportedBy: 'Citizen #4587', timestamp: '2025-04-26T09:12:00' },
  { taskId: 'RT-1041', issueType: 'Broken Streetlight', location: 'MG Road & Brigade Rd, Bengaluru', description: 'Streetlight flickering violently, then going dark at night creating hazard at intersection.', latitude: 12.9745, longitude: 77.6080, severity: 6, trafficVolume: 9, reportCount: 8, status: 'In Progress', reportedBy: 'Citizen #3204', timestamp: '2025-04-26T08:47:00' },
  { taskId: 'RT-1040', issueType: 'Water Logging', location: 'Ring Road near AIIMS, New Delhi', description: 'Major storm drain blockage after monsoon rain. Water accumulating across two lanes.', latitude: 28.5684, longitude: 77.2104, severity: 8, trafficVolume: 7, reportCount: 7, status: 'In Progress', reportedBy: 'Citizen #8911', timestamp: '2025-04-26T08:21:00' },
  { taskId: 'RT-1039', issueType: 'Road Damage', location: 'Marine Drive & Nariman Point, Mumbai', description: 'Deep asphalt trench following utility excavation. Vehicles bottoming out.', latitude: 18.9220, longitude: 72.8258, severity: 7, trafficVolume: 8, reportCount: 5, status: 'Pending', reportedBy: 'Citizen #1029', timestamp: '2025-04-26T07:56:00' },
  { taskId: 'RT-1038', issueType: 'Broken Streetlight', location: 'Park Street & Chowringhee, Kolkata', description: 'Pedestrian crossing lamp fixture damaged by fallen branch.', latitude: 22.5513, longitude: 88.3524, severity: 4, trafficVolume: 3, reportCount: 2, status: 'Resolved', reportedBy: 'Citizen #5541', timestamp: '2025-04-26T06:32:00' },
  { taskId: 'RT-1037', issueType: 'Water Logging', location: 'Anna Salai (Mount Road), Chennai', description: 'Clogged culvert near metro station entrance causing pedestrian pathway overflow.', latitude: 13.0604, longitude: 80.2496, severity: 5, trafficVolume: 5, reportCount: 4, status: 'Open', reportedBy: 'Citizen #7712', timestamp: '2025-04-26T03:48:00' },
  { taskId: 'RT-1036', issueType: 'Road Damage', location: 'ITO Junction & Vikas Marg, New Delhi', description: 'Manhole cover displaced and loose rim creating loud clanking and tire puncture risk.', latitude: 28.6304, longitude: 77.2410, severity: 8, trafficVolume: 9, reportCount: 6, status: 'In Progress', reportedBy: 'Citizen #9940', timestamp: '2025-04-26T03:48:00' },
  { taskId: 'RT-1035', issueType: 'Broken Streetlight', location: 'Bandra-Kurla Complex (BKC), Mumbai', description: 'Series of 3 decorative street lamps non-functional along transit corridor.', latitude: 19.0657, longitude: 72.8687, severity: 5, trafficVolume: 6, reportCount: 3, status: 'Pending', reportedBy: 'Citizen #2284', timestamp: '2025-04-26T02:11:00' }
];

let serverHeap = new CustomMaxHeap(SEED_TASKS.map(t => new RepairTask(t)));

const server = http.createServer((req, res) => {
  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PATCH, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // REST API Routes
  if (req.url === '/api/tasks/prioritized' && req.method === 'GET') {
    const list = serverHeap.extractAllSorted();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(list));
    return;
  }

  if (req.url === '/api/tasks/extract-max' && req.method === 'POST') {
    if (serverHeap.isEmpty()) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Heap is empty' }));
      return;
    }
    const max = serverHeap.extractMax();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(max));
    return;
  }

  if (req.url === '/api/reports' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      const data = JSON.parse(body);
      const task = new RepairTask({
        taskId: 'RT-' + Math.floor(1050 + Math.random() * 8000),
        issueType: data.issueType,
        location: data.locationName,
        description: data.description,
        latitude: data.latitude,
        longitude: data.longitude,
        severity: data.severityRating,
        trafficVolume: data.trafficImpact,
        reportCount: 1,
        status: 'Pending',
        reportedBy: data.reportedBy
      });
      serverHeap.insert(task);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(task));
    });
    return;
  }

  // Static File Serving
  let reqPath = req.url.split('?')[0];
  if (reqPath === '/' || reqPath === '') reqPath = '/index.html';

  const filePath = path.join(STATIC_DIR, reqPath);
  const ext = path.extname(filePath);
  const contentTypes = {
    '.html': 'text/html',
    '.css': 'text/css',
    '.js': 'application/javascript',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.svg': 'image/svg+xml'
  };

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('404 Not Found');
    } else {
      res.writeHead(200, { 'Content-Type': contentTypes[ext] || 'application/octet-stream' });
      res.end(content);
    }
  });
});

server.listen(PORT, () => {
  console.log(`=============================================================`);
  console.log(` CityFix Infrastructure Repair Prioritizer is LIVE!        `);
  console.log(` URL: http://localhost:${PORT}/                              `);
  console.log(` Dashboard:    http://localhost:${PORT}/index.html           `);
  console.log(` Repair Tasks: http://localhost:${PORT}/tasks.html           `);
  console.log(` Reports:      http://localhost:${PORT}/reports.html         `);
  console.log(` Citizen Form: http://localhost:${PORT}/submit.html          `);
  console.log(`=============================================================`);
});

