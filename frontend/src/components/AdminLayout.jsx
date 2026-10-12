
import { useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Users,
  DoorOpen,
  BedDouble,
  Settings,
  LogOut,
  Menu,
  X,
  Megaphone,
  GraduationCap,
  ChevronRight,
  ClipboardList,
  CreditCard,
  Lock,
  ShieldAlert,
} from "lucide-react";
import "./AdminLayout.css";


const menuItems = [
  { label: "Dashboard", path: "/dashboard", icon: LayoutDashboard },
  { label: "Students", path: "/students", icon: Users },
  { label: "Rooms", path: "/rooms", icon: DoorOpen },
  { label: "Room Allocation", path: "/allocations", icon: BedDouble },
  { label: "Attendance", path: "/attendance", icon: ClipboardList },
  { label: "Fees & Payments", path: "/fees", icon: CreditCard },
  { label: "Settings", path: "/settings", icon: Settings },
  { label: "Announcements", path: "/announcements", icon: Megaphone }
];


function readUser() {
  try {
    return JSON.parse(localStorage.getItem("user") || "null");
  } catch {
    return null;
  }
}

export default function AdminLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const user = readUser();

  const currentPage =
    menuItems.find((item) => item.path === location.pathname)?.label ||
    "Dashboard";

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    navigate("/login", { replace: true });
  }

  return (
    <div className="admin-layout">
      {sidebarOpen && (
        <button
          className="admin-sidebar-overlay"
          aria-label="Close navigation"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <aside
        id="admin-sidebar"
        className={`admin-sidebar ${sidebarOpen ? "is-open" : ""}`}
      >
        <div className="admin-brand">
          <div className="admin-brand-icon">
            <GraduationCap size={26} />
          </div>

          <div className="admin-brand-text">
            <strong>HostelHub</strong>
            <span>Management System</span>
          </div>

          <button
            className="admin-mobile-close"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close sidebar"
          >
            <X size={21} />
          </button>
        </div>

        <p className="admin-menu-label">WORKSPACE</p>

        <nav className="admin-navigation" aria-label="Admin navigation">
          {menuItems.map(({ label, path, icon: Icon }) => {
            const isLocked = Boolean(user?.mustChangePassword && path !== "/settings");
            return (
              <NavLink
                key={path}
                to={isLocked ? "#" : path}
                end
                onClick={(e) => {
                  if (isLocked) {
                    e.preventDefault();
                    navigate("/settings");
                    return;
                  }
                  setSidebarOpen(false);
                }}
                className={({ isActive }) =>
                  `admin-nav-link ${isActive && !isLocked ? "active" : ""}`
                }
                title={isLocked ? "Set a permanent password in Settings to unlock this feature" : undefined}
                style={isLocked ? { opacity: 0.55, cursor: "not-allowed" } : undefined}
              >
                <Icon size={19} />
                <span>{label}</span>
                {isLocked ? (
                  <Lock size={14} style={{ marginLeft: "auto", opacity: 0.7 }} />
                ) : (
                  location.pathname === path && (
                    <ChevronRight className="admin-nav-chevron" size={16} />
                  )
                )}
              </NavLink>
            );
          })}
        </nav>

        <div className="admin-sidebar-bottom">
          <div className="admin-user-card">
            <div className="admin-avatar">
              {(user?.fullName || "Admin").charAt(0).toUpperCase()}
            </div>
            <div className="admin-user-details">
              <strong>{user?.fullName || "Administrator"}</strong>
              <span>{user?.email || "Admin account"}</span>
              <small>{user?.hostelName || "SYSTEM ADMINISTRATOR"}</small>
            </div>
          </div>

          <button className="admin-logout" onClick={logout}>
            <LogOut size={18} />
            <span>Sign out</span>
          </button>
        </div>
      </aside>

      <div className="admin-workspace">
        <header className="admin-topbar">
          <div className="admin-topbar-left">
            <button
              className="admin-mobile-menu"
              onClick={() => setSidebarOpen(true)}
              aria-label="Open navigation"
              aria-controls="admin-sidebar"
              aria-expanded={sidebarOpen}
            >
              <Menu size={22} />
            </button>

            <div>
              <div className="admin-breadcrumb">
                Workspace <ChevronRight size={14} /> {currentPage}
              </div>
              <h1>{currentPage}</h1>
            </div>
          </div>

          <div className="admin-topbar-account">
            <div className="admin-status-dot" />
            <span>{user?.hostelName || "System administrator"}</span>
          </div>
        </header>

        {user?.mustChangePassword && (
          <div
            role="alert"
            style={{
              margin: "1.25rem 1.5rem 0",
              padding: "1rem 1.25rem",
              backgroundColor: "#fffbeb",
              border: "1px solid #fde68a",
              borderRadius: "10px",
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: "1rem",
              boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: "0.85rem", color: "#92400e" }}>
              <ShieldAlert size={22} style={{ flexShrink: 0, color: "#d97706" }} />
              <div>
                <strong style={{ display: "block", fontSize: "0.95rem", color: "#78350f" }}>
                  Action Required: Temporary Password Detected
                </strong>
                <span style={{ fontSize: "0.875rem", color: "#92400e" }}>
                  You are signed in with a temporary password. All management features (Attendance, Rooms, Allocations, Fees) are locked until you set a permanent password in Settings.
                </span>
              </div>
            </div>
            {location.pathname !== "/settings" && (
              <button
                type="button"
                onClick={() => navigate("/settings")}
                style={{
                  padding: "0.5rem 1rem",
                  backgroundColor: "#d97706",
                  color: "#ffffff",
                  border: "none",
                  borderRadius: "6px",
                  fontWeight: 600,
                  fontSize: "0.875rem",
                  cursor: "pointer",
                  whiteSpace: "nowrap",
                }}
              >
                Go to Settings
              </button>
            )}
          </div>
        )}

        <main className="admin-page-content">
          <Outlet />
        </main>

        <footer className="admin-footer">
          Centralized Hostel Management System
        </footer>
      </div>
    </div>
  );
}
