
import { useEffect, useState } from "react";
import {
  Users,
  BedDouble,
  DoorOpen,
  Bed,
  Activity,
  RefreshCw,
  Building2,
  CheckCircle2,
  AlertCircle,
  TrendingUp,
} from "lucide-react";
import { apiRequest } from "../services/api";
import "./Dashboard.css";

const initialStats = {
  totalStudents: 0,
  activeStudents: 0,
  totalRooms: 0,
  availableBeds: 0,
  occupiedBeds: 0,
  totalBeds: 0,
  occupancyPercentage: 0,
  activeRooms: 0,
  activeAllocations: 0,
};

function StatCard({ title, value, description, icon: Icon, color }) {
  return (
    <article className="dashboard-stat-card">
      <div className={`dashboard-stat-icon ${color}`}>
        <Icon size={23} strokeWidth={2} />
      </div>

      <div className="dashboard-stat-content">
        <p className="dashboard-stat-title">{title}</p>
        <h2>{value ?? 0}</h2>
        <span className="dashboard-stat-caption">{description}</span>
      </div>
    </article>
  );
}

export default function Dashboard() {
  let admin = null;
  try { admin = JSON.parse(localStorage.getItem("user") || "null"); } catch { admin = null; }
  const [stats, setStats] = useState(initialStats);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const loadDashboard = async (isRefresh = false) => {
    if (isRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    setError("");

    try {
      const response = await apiRequest("/dashboard/stats");
      setStats({ ...initialStats, ...(response || {}) });
    } catch (err) {
      console.error("Dashboard loading error:", err);
      setError(
        err?.response?.data?.message ||
          "Unable to load dashboard statistics. Please check your login and backend."
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    void Promise.resolve().then(loadDashboard);
  }, []);

  const occupancy = Math.max(
    0,
    Math.min(100, Number(stats.occupancyPercentage) || 0)
  );

  const formatNumber = (value) =>
    new Intl.NumberFormat("en-IN").format(Number(value) || 0);

  if (loading) {
    return (
      <main className="dashboard-page">
        <div className="dashboard-loading">
          <div className="dashboard-spinner" />
          <p>Loading your dashboard...</p>
        </div>
      </main>
    );
  }

  return (
    <main className="dashboard-page">
      <section className="dashboard-welcome">
        <div className="dashboard-welcome-content">
          <div className="dashboard-eyebrow">
            <Building2 size={15} />
            {admin?.hostelName || "HOSTEL MANAGEMENT SYSTEM"}
          </div>

          <h1>Dashboard</h1>
          <p>
            {admin?.hostelName ? `Welcome back! Here's an overview of ${admin.hostelName}.` : "Welcome back! Here's an overview of your hostel at a glance."}
          </p>
        </div>

        <button
          className="dashboard-refresh"
          type="button"
          onClick={() => loadDashboard(true)}
          disabled={refreshing}
        >
          <RefreshCw
            size={16}
            className={refreshing ? "dashboard-spin" : ""}
          />
          {refreshing ? "Refreshing..." : "Refresh"}
        </button>
      </section>

      {error && (
        <div className="dashboard-alert" role="alert">
          <AlertCircle size={19} />
          <span>{error}</span>
          <button type="button" onClick={() => loadDashboard(true)}>
            Retry
          </button>
        </div>
      )}

      <section className="dashboard-section">
        <div className="dashboard-section-heading">
          <div>
            <h2>Overview</h2>
            <p>Key hostel statistics</p>
          </div>
          <span className="dashboard-live-label">
            <span className="dashboard-live-dot" />
            Live statistics
          </span>
        </div>

        <div className="dashboard-stats-grid">
          <StatCard
            title="Total Students"
            value={formatNumber(stats.totalStudents)}
            description="Registered students"
            icon={Users}
            color="purple"
          />

          <StatCard
            title="Active Students"
            value={formatNumber(stats.activeStudents)}
            description="Currently active"
            icon={Activity}
            color="green"
          />

          <StatCard
            title="Total Rooms"
            value={formatNumber(stats.totalRooms)}
            description="Rooms in hostel"
            icon={DoorOpen}
            color="orange"
          />

          <StatCard
            title="Available Beds"
            value={formatNumber(stats.availableBeds)}
            description="Beds available"
            icon={Bed}
            color="blue"
          />
        </div>
      </section>

      <section className="dashboard-section">
        <div className="dashboard-section-heading">
          <div>
            <h2>Hostel Details</h2>
            <p>Room availability and occupancy</p>
          </div>
        </div>

        <div className="dashboard-detail-grid">
          <article className="dashboard-panel">
            <div className="dashboard-panel-heading">
              <div className="dashboard-panel-icon purple">
                <BedDouble size={20} />
              </div>
              <div>
                <h3>Bed Occupancy</h3>
                <p>Current bed utilization</p>
              </div>
            </div>

            <div className="dashboard-occupancy-main">
              <div>
                <h2>{occupancy.toFixed(1)}%</h2>
                <p>Occupancy rate</p>
              </div>
              <div className="dashboard-occupancy-circle">
                <TrendingUp size={25} />
              </div>
            </div>

            <div
              className="dashboard-progress-track"
              role="progressbar"
              aria-label="Bed occupancy"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={occupancy}
            >
              <div
                className="dashboard-progress-fill"
                style={{ width: `${occupancy}%` }}
              />
            </div>

            <div className="dashboard-occupancy-legend">
              <div>
                <span className="dashboard-legend-dot occupied" />
                <span>Occupied beds</span>
                <strong>{formatNumber(stats.occupiedBeds)}</strong>
              </div>

              <div>
                <span className="dashboard-legend-dot available" />
                <span>Available beds</span>
                <strong>{formatNumber(stats.availableBeds)}</strong>
              </div>
            </div>

            <div className="dashboard-total-beds">
              <span>Total bed capacity</span>
              <strong>{formatNumber(stats.totalBeds)}</strong>
            </div>
          </article>

          <article className="dashboard-panel">
            <div className="dashboard-panel-heading">
              <div className="dashboard-panel-icon blue">
                <Building2 size={20} />
              </div>
              <div>
                <h3>Room Summary</h3>
                <p>Current hostel status</p>
              </div>
            </div>

            <div className="dashboard-summary-list">
              <div className="dashboard-summary-row">
                <div className="dashboard-summary-label">
                  <span className="dashboard-summary-icon blue">
                    <DoorOpen size={18} />
                  </span>
                  <span>Total rooms</span>
                </div>
                <strong>{formatNumber(stats.totalRooms)}</strong>
              </div>

              <div className="dashboard-summary-row">
                <div className="dashboard-summary-label">
                  <span className="dashboard-summary-icon green">
                    <CheckCircle2 size={18} />
                  </span>
                  <span>Active rooms</span>
                </div>
                <strong>{formatNumber(stats.activeRooms)}</strong>
              </div>

              <div className="dashboard-summary-row">
                <div className="dashboard-summary-label">
                  <span className="dashboard-summary-icon purple">
                    <Users size={18} />
                  </span>
                  <span>Active allocations</span>
                </div>
                <strong>{formatNumber(stats.activeAllocations)}</strong>
              </div>

              <div className="dashboard-summary-row">
                <div className="dashboard-summary-label">
                  <span className="dashboard-summary-icon orange">
                    <Bed size={18} />
                  </span>
                  <span>Available beds</span>
                </div>
                <strong>{formatNumber(stats.availableBeds)}</strong>
              </div>
            </div>

            <div className="dashboard-panel-footer">
              <CheckCircle2 size={17} />
              <span>Statistics from the hostel system</span>
            </div>
          </article>
        </div>
      </section>
    </main>
  );
}
