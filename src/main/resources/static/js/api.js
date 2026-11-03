/*
 * Shared helpers used by every page:
 *  - token / user storage (localStorage)
 *  - api(): fetch wrapper that adds the JWT header and turns errors into exceptions
 *  - renderHeader(): the navigation bar (changes with the logged-in role)
 *  - toast(), esc(), todayStr(), requireRole()
 */

const TOKEN_KEY = 'docapp_token';
const USER_KEY = 'docapp_user';

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch (e) {
    return null;
  }
}

function saveAuth(data) {
  localStorage.setItem(TOKEN_KEY, data.token);
  localStorage.setItem(USER_KEY, JSON.stringify(data.user));
}

function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = 'login.html';
}

/**
 * Calls the backend. Example: api('/appointments', { method: 'POST', body: {...} })
 * Resolves with the parsed JSON, rejects with Error(message) when the server answers with an error.
 */
async function api(path, options = {}) {
  const method = options.method || 'GET';
  const headers = { 'Content-Type': 'application/json' };

  // the login / register calls must not send an old token
  const token = getToken();
  if (token && !path.startsWith('/auth/')) {
    headers['Authorization'] = 'Bearer ' + token;
  }

  const response = await fetch('/api' + path, {
    method: method,
    headers: headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  let data = null;
  const text = await response.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch (e) {
      data = null;
    }
  }

  if (!response.ok) {
    // token expired or invalid -> go back to login
    if (response.status === 401 && token && !path.startsWith('/auth/')) {
      logout();
    }
    const message = (data && (data.error || data.message)) || 'Request failed (' + response.status + ')';
    throw new Error(message);
  }
  return data;
}

/** Escapes text before putting it into innerHTML (prevents XSS). */
function esc(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** Today's date as yyyy-mm-dd in the browser's local time (used as min value for date inputs). */
function todayStr() {
  const d = new Date();
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}

function formatDate(isoDate) {
  const parts = isoDate.split('-');
  const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
  return d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
}

function formatTime(hhmm) {
  const parts = hhmm.split(':');
  let h = Number(parts[0]);
  const suffix = h >= 12 ? 'PM' : 'AM';
  h = h % 12;
  if (h === 0) h = 12;
  return h + ':' + parts[1] + ' ' + suffix;
}

function initials(name) {
  return name
    .replace('Dr. ', '')
    .split(' ')
    .map(function (p) { return p.charAt(0); })
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

function toast(message, type) {
  const el = document.createElement('div');
  el.className = 'toast ' + (type || 'success');
  el.textContent = message;
  document.body.appendChild(el);
  setTimeout(function () { el.remove(); }, 3200);
}

/** Redirects to the login page unless the logged-in user has one of the allowed roles. */
function requireRole(allowedRoles) {
  const user = getUser();
  if (!user || !getToken() || allowedRoles.indexOf(user.role) === -1) {
    window.location.href = 'login.html';
    return false;
  }
  return true;
}

/** Where each role lands after login. */
function homeFor(user) {
  if (user.role === 'DOCTOR') return 'doctor.html';
  if (user.role === 'ADMIN') return 'admin.html';
  return 'index.html';
}

/** Builds the top navigation bar inside <div id="site-header"></div>. */
function renderHeader() {
  const host = document.getElementById('site-header');
  if (!host) return;

  const user = getUser();
  let links = '<a href="index.html">Find Doctors</a>';

  if (user && getToken()) {
    if (user.role === 'PATIENT') links += '<a href="my-appointments.html">My Appointments</a>';
    if (user.role === 'DOCTOR') links += '<a href="doctor.html">My Dashboard</a>';
    if (user.role === 'ADMIN') links += '<a href="admin.html">Admin Panel</a>';
    links += '<span class="nav-user">' + esc(user.fullName) + ' (' + esc(user.role.toLowerCase()) + ')</span>';
    links += '<a href="#" id="logout-link">Logout</a>';
  } else {
    links += '<a href="login.html">Login</a>';
    links += '<a href="register.html" class="btn btn-sm">Register</a>';
  }

  host.innerHTML =
    '<header class="site-header"><div class="inner">' +
    '<a class="logo" href="index.html">&#9877; DocBook</a>' +
    '<nav class="nav">' + links + '</nav>' +
    '</div></header>';

  const logoutLink = document.getElementById('logout-link');
  if (logoutLink) {
    logoutLink.addEventListener('click', function (e) {
      e.preventDefault();
      logout();
    });
  }
}
