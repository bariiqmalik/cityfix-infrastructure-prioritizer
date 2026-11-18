/**
 * CityFix Application UI Controller.
 * Handles DOM manipulation, Leaflet Map integration, and synchronization
 * with the CustomMaxHeap backend / client model.
 */

// Initial seed dataset matching Indian infrastructure prioritizer
const INITIAL_REPAIR_TASKS = [
  {
    taskId: 'RT-1042',
    issueType: 'Road Damage',
    location: 'Connaught Place & Barakhamba Rd, New Delhi',
    description: 'Deep pothole and surface damage near Rajiv Chowk Metro Gate 4. Causing minor traffic disruption.',
    latitude: 28.6315,
    longitude: 77.2167,
    severity: 9,
    trafficVolume: 8,
    reportCount: 12,
    status: 'Pending',
    reportedBy: 'Citizen #4587',
    timestamp: '2025-04-26T09:12:00'
  },
  {
    taskId: 'RT-1041',
    issueType: 'Broken Streetlight',
    location: 'MG Road & Brigade Rd, Bengaluru',
    description: 'Streetlight flickering violently, then going dark at night creating hazard at intersection.',
    latitude: 12.9745,
    longitude: 77.6080,
    severity: 6,
    trafficVolume: 9,
    reportCount: 8,
    status: 'In Progress',
    reportedBy: 'Citizen #3204',
    timestamp: '2025-04-26T08:47:00'
  },
  {
    taskId: 'RT-1040',
    issueType: 'Water Logging',
    location: 'Ring Road near AIIMS, New Delhi',
    description: 'Major storm drain blockage after monsoon rain. Water accumulating across two lanes.',
    latitude: 28.5684,
    longitude: 77.2104,
    severity: 8,
    trafficVolume: 7,
    reportCount: 7,
    status: 'In Progress',
    reportedBy: 'Citizen #8911',
    timestamp: '2025-04-26T08:21:00'
  },
  {
    taskId: 'RT-1039',
    issueType: 'Road Damage',
    location: 'Marine Drive & Nariman Point, Mumbai',
    description: 'Deep asphalt trench following utility excavation. Vehicles bottoming out.',
    latitude: 18.9220,
    longitude: 72.8258,
    severity: 7,
    trafficVolume: 8,
    reportCount: 5,
    status: 'Pending',
    reportedBy: 'Citizen #1029',
    timestamp: '2025-04-26T07:56:00'
  },
  {
    taskId: 'RT-1038',
    issueType: 'Broken Streetlight',
    location: 'Park Street & Chowringhee, Kolkata',
    description: 'Pedestrian crossing lamp fixture damaged by fallen branch.',
    latitude: 22.5513,
    longitude: 88.3524,
    severity: 4,
    trafficVolume: 3,
    reportCount: 2,
    status: 'Resolved',
    reportedBy: 'Citizen #5541',
    timestamp: '2025-04-26T06:32:00'
  },
  {
    taskId: 'RT-1037',
    issueType: 'Water Logging',
    location: 'Anna Salai (Mount Road), Chennai',
    description: 'Clogged culvert near metro station entrance causing pedestrian pathway overflow.',
    latitude: 13.0604,
    longitude: 80.2496,
    severity: 5,
    trafficVolume: 5,
    reportCount: 4,
    status: 'Open',
    reportedBy: 'Citizen #7712',
    timestamp: '2025-04-26T03:48:00'
  },
  {
    taskId: 'RT-1036',
    issueType: 'Road Damage',
    location: 'ITO Junction & Vikas Marg, New Delhi',
    description: 'Manhole cover displaced and loose rim creating loud clanking and tire puncture risk.',
    latitude: 28.6304,
    longitude: 77.2410,
    severity: 8,
    trafficVolume: 9,
    reportCount: 6,
    status: 'In Progress',
    reportedBy: 'Citizen #9940',
    timestamp: '2025-04-26T03:48:00'
  },
  {
    taskId: 'RT-1035',
    issueType: 'Broken Streetlight',
    location: 'Bandra-Kurla Complex (BKC), Mumbai',
    description: 'Series of 3 decorative street lamps non-functional along transit corridor.',
    latitude: 19.0657,
    longitude: 72.8687,
    severity: 5,
    trafficVolume: 6,
    reportCount: 3,
    status: 'Pending',
    reportedBy: 'Citizen #2284',
    timestamp: '2025-04-26T02:11:00'
  }
];

// Initialize global client heap from seed tasks
let globalHeap = new CustomMaxHeap(INITIAL_REPAIR_TASKS.map(t => new RepairTask(t)));
let activeSelectedTask = null;
let leafletMap = null;

// Format Date string: "Apr 26, 2025 09:12 AM"
function formatDate(dateObj) {
  const d = dateObj instanceof Date ? dateObj : new Date(dateObj);
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const month = months[d.getMonth()];
  const day = d.getDate();
  const year = d.getFullYear();
  let hours = d.getHours();
  const minutes = d.getMinutes().toString().padStart(2, '0');
  const ampm = hours >= 12 ? 'PM' : 'AM';
  hours = hours % 12;
  hours = hours ? hours : 12;
  const hoursStr = hours.toString().padStart(2, '0');
  return `${month} ${day}, ${year} ${hoursStr}:${minutes} ${ampm}`;
}

// Issue Type Icon helper
function getIssueIconHtml(type) {
  if (type.includes('Road') || type.includes('Pothole')) {
    return `<span class="issue-icon-dot" style="background: #FEE2E2; color: #EF4444;">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
    </span>`;
  } else if (type.includes('Streetlight') || type.includes('Light')) {
    return `<span class="issue-icon-dot" style="background: #FEF3C7; color: #D97706;">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 18h6"></path><path d="M10 22h4"></path><path d="M15.09 14c.18-.98.65-1.74 1.41-2.5A4.65 4.65 0 0 0 18 8 6 6 0 0 0 6 8c0 1 .23 2.23 1.5 3.5.76.76 1.23 1.52 1.41 2.5"></path></svg>
    </span>`;
  } else if (type.includes('Water') || type.includes('Drain')) {
    return `<span class="issue-icon-dot" style="background: #EFF6FF; color: #2563EB;">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"></path></svg>
    </span>`;
  }
  return `<span class="issue-icon-dot" style="background: #F1F5F9; color: #64748B;">
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle><circle cx="5" cy="12" r="1"></circle></svg>
  </span>`;
}

// Badge helpers
function getPriorityBadgeHtml(level) {
  const l = (level || 'Medium').toLowerCase();
  if (l === 'high') return `<span class="badge-priority badge-priority-high">High</span>`;
  if (l === 'low') return `<span class="badge-priority badge-priority-low">Low</span>`;
  return `<span class="badge-priority badge-priority-medium">Medium</span>`;
}

function getStatusBadgeHtml(status) {
  const s = (status || 'pending').toLowerCase();
  if (s === 'in-progress' || s === 'in progress') return `<span class="badge-status badge-status-inprogress">In Progress</span>`;
  if (s === 'resolved') return `<span class="badge-status badge-status-resolved">Resolved</span>`;
  if (s === 'open') return `<span class="badge-status badge-status-open">Open</span>`;
  return `<span class="badge-status badge-status-pending">Pending</span>`;
}

function getScorePillClass(score) {
  if (score >= 90) return 'score-90-plus';
  if (score >= 80) return 'score-80-plus';
  if (score >= 70) return 'score-70-plus';
  return 'score-below-70';
}

// Show Toast
function showToast(message) {
  let toast = document.getElementById('app-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'app-toast';
    toast.className = 'toast-banner';
    document.body.appendChild(toast);
  }
  toast.innerHTML = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#10B981" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg> <span>${message}</span>`;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 3800);
}

// Indian Municipal Jurisdictions & Landmarks
const INDIAN_CITIES = {
  all: { name: 'All India', coords: [21.7679, 78.8718], zoom: 5 },
  delhi: { name: 'Delhi NCR', coords: [28.6139, 77.2090], zoom: 12 },
  mumbai: { name: 'Mumbai', coords: [19.0760, 72.8777], zoom: 12 },
  bengaluru: { name: 'Bengaluru', coords: [12.9716, 77.5946], zoom: 12 },
  kolkata: { name: 'Kolkata', coords: [22.5726, 88.3639], zoom: 12 },
  chennai: { name: 'Chennai', coords: [13.0827, 80.2496], zoom: 12 }
};

window.jumpToCity = function(cityKey) {
  if (!leafletMap) return;
  const target = INDIAN_CITIES[cityKey] || INDIAN_CITIES.delhi;
  leafletMap.flyTo(target.coords, target.zoom, { duration: 1.0 });
  document.querySelectorAll('.city-jump-btn').forEach(btn => {
    if (btn.getAttribute('data-city') === cityKey) {
      btn.style.backgroundColor = '#2563EB';
      btn.style.color = '#FFFFFF';
      btn.style.borderColor = '#2563EB';
    } else {
      btn.style.backgroundColor = '#FFFFFF';
      btn.style.color = '#475569';
      btn.style.borderColor = '#CBD5E1';
    }
  });
};

// Initialize Free Leaflet Map Centered in India (OpenStreetMap)
function initMap(elementId) {
  const container = document.getElementById(elementId);
  if (!container || typeof L === 'undefined') return;

  // Clean existing map instance if any
  if (leafletMap) {
    try { leafletMap.remove(); } catch (e) {}
    leafletMap = null;
  }

  // Centered on New Delhi, India by default
  const defaultCenter = [28.6139, 77.2090];
  leafletMap = L.map(elementId, {
    zoomControl: false,
    attributionControl: true
  }).setView(defaultCenter, 12);

  // 100% Free OpenStreetMap & Open Source Tile Providers
  const osmStandard = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors'
  });

  const cartoVoyager = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> & CartoDB'
  });

  const cartoPositron = L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> & CartoDB'
  });

  // Default layer is standard free OpenStreetMap
  osmStandard.addTo(leafletMap);

  // Free Base Layers Switcher
  L.control.layers({
    '🗺️ OpenStreetMap (Free)': osmStandard,
    '🏙️ Carto Voyager': cartoVoyager,
    '📄 Clean Light': cartoPositron
  }, null, { position: 'topright' }).addTo(leafletMap);

  L.control.zoom({ position: 'topleft' }).addTo(leafletMap);

  // Add issue pins from CustomMaxHeap
  const allTasks = globalHeap.extractAllSorted();
  const markerGroup = L.featureGroup();

  allTasks.forEach(task => {
    const score = task.getDisplayScore();
    let pinColor = '#10B981'; // Green (Low)
    if (score >= 80) pinColor = '#EF4444'; // Red (High)
    else if (score >= 60) pinColor = '#F59E0B'; // Amber (Medium)

    const markerIcon = L.divIcon({
      className: 'custom-map-pin',
      html: `<div style="background-color: ${pinColor}; width: 16px; height: 16px; border-radius: 50%; border: 2.5px solid #FFFFFF; box-shadow: 0 2px 5px rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; font-size: 8px; font-weight: bold; color: white;"></div>`,
      iconSize: [16, 16],
      iconAnchor: [8, 8]
    });

    const marker = L.marker([task.latitude, task.longitude], { icon: markerIcon });
    marker.bindPopup(`
      <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-size: 12px; min-width: 180px; padding: 2px;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px;">
          <strong style="color: #0F172A;">${task.taskId}</strong>
          <span style="background: ${pinColor}20; color: ${pinColor}; font-weight: 700; padding: 2px 6px; border-radius: 4px; font-size: 11px;">Score: ${score}</span>
        </div>
        <div style="font-weight: 600; color: #334155; margin-bottom: 2px;">${task.issueType}</div>
        <div style="color: #64748B; font-size: 11px; margin-bottom: 4px;">📍 ${task.location}</div>
        <div style="font-size: 11px; color: #475569; border-top: 1px solid #E2E8F0; padding-top: 4px;">Status: <strong>${task.status}</strong></div>
      </div>
    `);
    marker.addTo(leafletMap);
    markerGroup.addLayer(marker);
  });
}

// Renders the Repair Priority Queue on Dashboard
function renderDashboardPriorityQueue() {
  const tbody = document.getElementById('priority-queue-tbody');
  if (!tbody) return;

  // STRICT REQUIREMENT: Pulled directly from CustomMaxHeap in descending order of urgency
  const prioritizedTasks = globalHeap.extractAllSorted();
  const topFive = prioritizedTasks.slice(0, 5);

  let html = '';
  topFive.forEach((task, idx) => {
    const score = task.getDisplayScore();
    const pillClass = getScorePillClass(score);
    html += `
      <tr>
        <td style="font-weight: 500; color: #64748B;">${idx + 1}</td>
        <td>
          <div class="issue-type-cell">
            ${task.issueType}
          </div>
        </td>
        <td style="color: #64748B;">${task.location}</td>
        <td style="text-align: right;">
          <span class="urgency-score-pill ${pillClass}">${score}</span>
        </td>
      </tr>
    `;
  });
  tbody.innerHTML = html;
}

// Renders Dashboard Recent Reports table
function renderDashboardRecentReports() {
  const tbody = document.getElementById('recent-reports-tbody');
  if (!tbody) return;

  const allTasks = globalHeap.extractAllSorted();
  const recent = allTasks.slice(0, 5);

  let html = '';
  recent.forEach(task => {
    html += `
      <tr>
        <td style="font-weight: 600; color: #0F172A;">#${task.taskId.replace('RT-', 'R-')}</td>
        <td>
          <div class="issue-type-cell">
            ${task.issueType}
          </div>
        </td>
        <td style="color: #64748B;">${task.location}</td>
        <td>${getPriorityBadgeHtml(task.getPriorityLevel())}</td>
        <td>${getStatusBadgeHtml(task.status)}</td>
        <td style="color: #64748B;">${formatDate(task.timestamp)}</td>
      </tr>
    `;
  });
  tbody.innerHTML = html;
}

// Renders the main Repair Tasks Table
function renderRepairTasksTable(filterQuery = '', statusFilter = 'all', priorityFilter = 'all') {
  const tbody = document.getElementById('repair-tasks-tbody');
  if (!tbody) return;

  // Pulled strictly from CustomMaxHeap
  let tasks = globalHeap.extractAllSorted();

  if (filterQuery) {
    const q = filterQuery.toLowerCase();
    tasks = tasks.filter(t => 
      t.taskId.toLowerCase().includes(q) || 
      t.location.toLowerCase().includes(q) || 
      t.issueType.toLowerCase().includes(q)
    );
  }

  if (statusFilter !== 'all') {
    tasks = tasks.filter(t => t.status.toLowerCase().replace(' ', '-') === statusFilter.toLowerCase());
  }

  if (priorityFilter !== 'all') {
    tasks = tasks.filter(t => t.getPriorityLevel().toLowerCase() === priorityFilter.toLowerCase());
  }

  let html = '';
  tasks.forEach(task => {
    html += `
      <tr data-task-id="${task.taskId}" onclick="selectTaskDetails('${task.taskId}')" style="cursor: pointer;">
        <td onclick="event.stopPropagation();">
          <input type="checkbox" style="border-radius: 4px; accent-color: #2563EB;">
        </td>
        <td style="font-weight: 600; color: #0F172A;">${task.taskId}</td>
        <td>
          <div class="issue-type-cell">
            ${getIssueIconHtml(task.issueType)}
            <span>${task.issueType}</span>
          </div>
        </td>
        <td style="color: #475569;">${task.location}</td>
        <td style="color: #64748B; font-family: monospace; font-size: 12px;">${task.latitude.toFixed(4)}, ${task.longitude.toFixed(4)}</td>
        <td>${getPriorityBadgeHtml(task.getPriorityLevel())}</td>
        <td>${getStatusBadgeHtml(task.status)}</td>
        <td style="color: #64748B;">${formatDate(task.timestamp)}</td>
        <td onclick="event.stopPropagation();">
          <div style="display: flex; align-items: center; gap: 8px;">
            <button class="btn-outline" onclick="selectTaskDetails('${task.taskId}')">View</button>
            <button style="background: none; border: none; color: #94A3B8; cursor: pointer; padding: 4px;">⋮</button>
          </div>
        </td>
      </tr>
    `;
  });

  tbody.innerHTML = html;

  // Select the top task by default if none selected
  if (!activeSelectedTask && tasks.length > 0) {
    selectTaskDetails(tasks[0].taskId);
  }
}

// Selects and displays task in the right details panel
function selectTaskDetails(taskId) {
  const allTasks = globalHeap.extractAllSorted();
  const task = allTasks.find(t => t.taskId === taskId);
  if (!task) return;

  activeSelectedTask = task;

  const panel = document.getElementById('task-details-panel');
  if (!panel) return;

  document.getElementById('details-task-id').innerText = task.taskId;
  document.getElementById('details-issue-type').innerText = task.issueType;
  document.getElementById('details-location').innerText = task.location;
  document.getElementById('details-coordinates').innerText = `${task.latitude.toFixed(4)}, ${task.longitude.toFixed(4)}`;
  document.getElementById('details-reported-at').innerText = formatDate(task.timestamp);
  document.getElementById('details-reported-by').innerText = task.reportedBy || 'Citizen #4587';
  document.getElementById('details-description').innerText = task.description || 'No description provided.';
  
  const score = task.getDisplayScore();
  const scoreElem = document.getElementById('details-priority-score');
  scoreElem.innerText = score;
  scoreElem.style.color = score >= 80 ? '#EF4444' : (score >= 60 ? '#F59E0B' : '#10B981');

  // Update badge
  const priorityBadge = document.getElementById('details-priority-badge');
  if (priorityBadge) {
    priorityBadge.className = `badge-priority badge-priority-${task.getPriorityLevel().toLowerCase()}`;
    priorityBadge.innerText = `${task.getPriorityLevel()} Priority`;
  }

  // Update status select
  const statusSelect = document.getElementById('details-status-select');
  if (statusSelect) {
    statusSelect.value = task.status.toLowerCase().replace(' ', '-');
  }
}

// Updates selected task status and re-heapifies
function updateCurrentTask() {
  if (!activeSelectedTask) return;

  const statusSelect = document.getElementById('details-status-select');
  const newStatus = statusSelect.value;
  activeSelectedTask.status = newStatus === 'in-progress' ? 'In Progress' : (newStatus.charAt(0).toUpperCase() + newStatus.slice(1));

  // Rebuild heap to enforce updated invariants
  const all = globalHeap.extractAllSorted();
  globalHeap = new CustomMaxHeap(all);

  showToast(`Task ${activeSelectedTask.taskId} successfully updated to "${activeSelectedTask.status}"!`);
  renderRepairTasksTable();
  selectTaskDetails(activeSelectedTask.taskId);
}

// Algorithmic extractMax dispatch action
function dispatchExtractMax() {
  if (globalHeap.isEmpty()) {
    alert("Max-Heap is currently empty. No pending tasks to extract.");
    return;
  }
  const mostUrgent = globalHeap.extractMax();
  showToast(`Dispatched Highest Priority Task: [${mostUrgent.taskId}] ${mostUrgent.issueType} (Urgency: ${mostUrgent.getDisplayScore()})`);
  
  // Refresh views
  renderDashboardPriorityQueue();
  renderRepairTasksTable();
}

// Global search and filter bindings
function setupSearchAndFilters() {
  const searchInput = document.getElementById('task-search-input');
  const statusSelect = document.getElementById('task-status-filter');
  const prioritySelect = document.getElementById('task-priority-filter');

  function applyFilters() {
    renderRepairTasksTable(
      searchInput ? searchInput.value : '',
      statusSelect ? statusSelect.value : 'all',
      prioritySelect ? prioritySelect.value : 'all'
    );
  }

  if (searchInput) searchInput.addEventListener('input', applyFilters);
  if (statusSelect) statusSelect.addEventListener('change', applyFilters);
  if (prioritySelect) prioritySelect.addEventListener('change', applyFilters);
}

// Run on page load
document.addEventListener('DOMContentLoaded', () => {
  renderDashboardPriorityQueue();
  renderDashboardRecentReports();
  renderRepairTasksTable();
  setupSearchAndFilters();

  if (document.getElementById('cityfix-map')) {
    initMap('cityfix-map');
  }
});

