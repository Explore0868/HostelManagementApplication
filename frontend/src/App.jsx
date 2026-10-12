
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
} from "react-router-dom";
import { useEffect, useState } from "react";
import { Activity, BedDouble, CalendarDays, CreditCard, LogOut, Megaphone, RefreshCw, UserRound, ThumbsUp, ThumbsDown } from "lucide-react";

import Login from "./pages/Login";
import AdminLayout from "./components/AdminLayout";
import Dashboard from "./pages/Dashboard";
import Students from "./pages/Students";
import Rooms from "./pages/Rooms";
import RoomAllocations from "./pages/RoomAllocations";
import Fees from "./pages/Fees";
import Attendance from "./pages/Attendance";
import Announcements from "./pages/Announcements";
import Settings from "./pages/Settings";
import Hostels from "./pages/Hostels";
import AdminNotifications from "./pages/AdminNotifications";
import SystemAdminLayout from "./components/SystemAdminLayout";
import SystemAdminDashboard from "./pages/SystemAdminDashboard";
import { apiRequest } from "./services/api";
import "./StudentDashboard.css";

function getUser() {
  try {
    return JSON.parse(localStorage.getItem("user") || "null");
  } catch {
    return null;
  }
}

function getRole(user) {
  return String(user?.role || "")
    .replace("ROLE_", "")
    .toUpperCase();
}

function ProtectedRoute({ allowedRoles, systemAdminOnly = false, hostelAdminOnly = false, children }) {
  const token = localStorage.getItem("token");
  const user = getUser();

  if (!token || !user) {
    return <Navigate to="/login" replace />;
  }

  const role = getRole(user);
  if (systemAdminOnly && (role !== "ADMIN" || user.systemAdmin !== true)) return <Navigate to={role === "STUDENT" ? "/student-dashboard" : "/dashboard"} replace />;
  if (hostelAdminOnly && (role !== "ADMIN" || user.systemAdmin === true)) return <Navigate to={role === "STUDENT" ? "/student-dashboard" : "/system-admin"} replace />;

  if (allowedRoles && !allowedRoles.includes(role)) {
    return (
      <Navigate
        to={
          role === "ADMIN"
            ? "/dashboard"
            : "/student-dashboard"
        }
        replace
      />
    );
  }

  return children;
}

function StudentDashboard() {
  const user = getUser();
  const [portal, setPortal] = useState({ profile: null, attendance: [], fees: [], announcements: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reactionBusy, setReactionBusy] = useState(null);

  async function loadPortal() {
    const [profileResult, attendanceResult, feesResult, announcementsResult] = await Promise.allSettled([
      apiRequest("/student/me"),
      apiRequest("/student/attendance"),
      apiRequest("/student/fees"),
      apiRequest("/student/announcements"),
    ]);

    setPortal((previous) => ({
      profile: profileResult.status === "fulfilled" ? profileResult.value : previous.profile,
      attendance: attendanceResult.status === "fulfilled" ? attendanceResult.value : [],
      fees: feesResult.status === "fulfilled" ? feesResult.value : [],
      announcements: announcementsResult.status === "fulfilled" ? announcementsResult.value : [],
    }));
    const failed = [profileResult, attendanceResult, feesResult, announcementsResult]
      .some((result) => result.status === "rejected");
    if (profileResult.status === "rejected") {
      setError(profileResult.reason?.message || "Unable to load your student profile.");
    } else if (failed) {
      setError("Some account details could not be loaded. Refresh to try again.");
    } else {
      setError("");
    }
    setLoading(false);
  }

  function refreshPortal() {
    setLoading(true);
    setError("");
    loadPortal();
  }

  async function setAnnouncementReaction(item, reaction) {
    if (reactionBusy === item.id) return;
    const nextReaction = item.myReaction === reaction ? null : reaction;
    setReactionBusy(item.id);
    try {
      const updated = await apiRequest(`/student/announcements/${item.id}/reaction`, {
        method: "PATCH",
        body: JSON.stringify({ reaction: nextReaction }),
      });
      setPortal((previous) => ({ ...previous, announcements: previous.announcements.map((announcement) => announcement.id === item.id ? updated : announcement) }));
    } catch (err) {
      setError(err.message || "Could not save your announcement response.");
    } finally {
      setReactionBusy(null);
    }
  }

  useEffect(() => { void Promise.resolve().then(loadPortal); }, []);
  const profile = portal.profile;
  const allocation = profile?.roomAllocation;
  const dueTotal = portal.fees.reduce((sum, fee) => sum + Number(fee.balanceAmount || 0), 0);
  const presentCount = portal.attendance.filter((item) => item.status === "PRESENT").length;

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    window.location.replace("/login");
  }

  return (
    <main className="student-portal">
      <header className="student-portal-topbar">
        <a className="student-portal-brand" href="/student-dashboard"><span><UserRound size={20} /></span> HostelHub</a>
        <div className="student-portal-actions">
          <button className="student-refresh" onClick={refreshPortal} disabled={loading} aria-label="Refresh account">
            <RefreshCw size={17} className={loading ? "student-spin" : ""} /> Refresh
          </button>
          <button className="student-logout" onClick={logout}><LogOut size={17} /> Sign out</button>
        </div>
      </header>
      <div className="student-portal-content">
        <section className="student-hero">
          <div><span className="student-kicker">STUDENT PORTAL{profile?.hostelName ? ` · ${profile.hostelName}` : ""}</span>
            <h1>Hello, {profile?.fullName || user?.fullName || "Student"}</h1>
            <p>Your hostel life, account details, and important updates in one place.</p>
          </div>
          <div className="student-hero-mark"><BedDouble size={42} /></div>
        </section>
        {error && <div className="student-notice" role="alert">{error}</div>}
        {loading && !profile ? <div className="student-loading"><span className="student-spinner" /> Loading your account…</div> : (
          <>
            <section className="student-metrics" aria-label="Account overview">
              <article><span className="student-metric-icon violet"><BedDouble size={20} /></span><p>Room</p><strong>{allocation?.roomNumber ? `Room ${allocation.roomNumber}` : "Not assigned"}</strong><small>{allocation ? `${allocation.blockName || profile?.hostelName || "Hostel"} · Bed ${allocation.bedNumber}` : "Contact your hostel office"}</small></article>
              <article><span className="student-metric-icon green"><Activity size={20} /></span><p>Attendance</p><strong>{portal.attendance.length ? `${Math.round((presentCount / portal.attendance.length) * 100)}%` : "—"}</strong><small>{presentCount} present days recorded</small></article>
              <article><span className="student-metric-icon amber"><CreditCard size={20} /></span><p>Outstanding fees</p><strong>{new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(dueTotal)}</strong><small>{portal.fees.filter((fee) => Number(fee.balanceAmount) > 0).length} unpaid items</small></article>
            </section>
            <section className="student-details-grid">
              <article className="student-panel">
                <div className="student-panel-heading"><div><span>YOUR DETAILS</span><h2>Student profile</h2></div><UserRound size={20} /></div>
                <dl className="student-profile-list">
                  <div><dt>Student ID</dt><dd>{profile?.studentCode || "—"}</dd></div>
                  <div><dt>Course and year</dt><dd>{[profile?.course, profile?.studyYear ? `Year ${profile.studyYear}` : null].filter(Boolean).join(" · ") || "—"}</dd></div>
                  <div><dt>Email</dt><dd>{profile?.email || "—"}</dd></div>
                  <div><dt>Phone</dt><dd>{profile?.phone || "—"}</dd></div>
                  <div><dt>Guardian</dt><dd>{profile?.guardianName || "—"}</dd></div>
                </dl>
              </article>
              <article className="student-panel">
                <div className="student-panel-heading"><div><span>RECENT ACTIVITY</span><h2>Attendance</h2></div><CalendarDays size={20} /></div>
                {portal.attendance.length ? <div className="student-activity-list">{portal.attendance.slice(0, 5).map((item) => <div key={item.id || item.attendanceDate}><span>{new Date(`${item.attendanceDate}T00:00:00`).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}</span><strong className={item.status === "PRESENT" ? "present" : "absent"}>{item.status}</strong></div>)}</div> : <p className="student-empty">Attendance records will appear here when they are available.</p>}
              </article>
            </section>
            <section className="student-panel student-announcements">
              <div className="student-panel-heading"><div><span>HOSTEL UPDATES</span><h2>Announcements</h2></div><Megaphone size={20} /></div>
              {portal.announcements.length ? <div className="student-announcement-list">{portal.announcements.map((item) => <article key={item.id}><div><span>{item.category || "General"}</span>{item.createdAt && <time>{new Date(item.createdAt).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}</time>}</div><h3>{item.title}</h3><p>{item.content}</p><div className="announcement-reactions" aria-label="Respond to announcement"><button type="button" disabled={reactionBusy === item.id} aria-pressed={item.myReaction === "LIKE"} className={item.myReaction === "LIKE" ? "selected-like" : ""} onClick={() => setAnnouncementReaction(item, "LIKE")}><ThumbsUp size={16} /> Accept <span>{item.thumbsUpCount || 0}</span></button><button type="button" disabled={reactionBusy === item.id} aria-pressed={item.myReaction === "DISLIKE"} className={item.myReaction === "DISLIKE" ? "selected-dislike" : ""} onClick={() => setAnnouncementReaction(item, "DISLIKE")}><ThumbsDown size={16} /> Disagree <span>{item.thumbsDownCount || 0}</span></button></div></article>)}</div> : <p className="student-empty">No announcements yet.</p>}
            </section>
          </>
        )}
      </div>
    </main>
  );
}

function LoginRoute() {
  const token = localStorage.getItem("token");
  const user = getUser();

  if (token && user) {
    return (
      <Navigate
        to={
          getRole(user) === "ADMIN"
            ? (user.systemAdmin ? "/system-admin" : "/dashboard")
            : "/student-dashboard"
        }
        replace
      />
    );
  }

  return <Login />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public route */}
        <Route path="/login" element={<LoginRoute />} />

        {/* Protected admin application */}
        <Route
          element={
            <ProtectedRoute allowedRoles={["ADMIN"]} hostelAdminOnly>
              <AdminLayout />
            </ProtectedRoute>
          }
        >
          <Route
            path="/dashboard"
            element={<Dashboard />}
          />
          <Route path="/fees" element={<Fees />} />
 

          <Route
            path="/students"
            element={<Students />}
          />

          {/* Rooms Management */}
          <Route
            path="/rooms"
            element={<Rooms />}
          />

          {/* Room Allocation */}
          <Route
            path="/allocations"
            element={<RoomAllocations />}
          />

          {/* Support both sidebar URL patterns */}
          <Route
            path="/room-allocations"
            element={<RoomAllocations />}
          />

          <Route
            path="/attendance"
            element={<Attendance />}
          />

       <Route path="/settings" element={<Settings />} />
       <Route path="/announcements" element={<Announcements />} />
        </Route>

        <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} systemAdminOnly><SystemAdminLayout /></ProtectedRoute>}>
          <Route path="/system-admin" element={<SystemAdminDashboard />} />
          <Route path="/system-admin/hostels" element={<Hostels />} />
          <Route path="/system-admin/notifications" element={<AdminNotifications />} />
        </Route>
            

        {/* Student route */}
        <Route
          path="/student-dashboard"
          element={
            <ProtectedRoute allowedRoles={["STUDENT"]}>
              <StudentDashboard />
            </ProtectedRoute>
          }
        />

        {/* Default routes */}
        <Route
          path="/"
          element={<Navigate to="/login" replace />}
        />

        <Route
          path="*"
          element={<Navigate to="/" replace />}
        />
      </Routes>
    </BrowserRouter>
  );
}
