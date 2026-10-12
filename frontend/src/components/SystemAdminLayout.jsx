import { useState } from "react";
import { ChevronRight, GraduationCap, LayoutDashboard, LogOut, Menu, Building2, Bell, X, Settings as SettingsIcon, Lock, ShieldAlert } from "lucide-react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import "./AdminLayout.css";
import "./SystemAdminLayout.css";

const links = [
  { label: "Overview", path: "/system-admin", icon: LayoutDashboard, end: true },
  { label: "Hostels", path: "/system-admin/hostels", icon: Building2 },
  { label: "Notifications", path: "/system-admin/notifications", icon: Bell },
  { label: "Settings", path: "/system-admin/settings", icon: SettingsIcon },
];

export default function SystemAdminLayout() {
  const [open, setOpen] = useState(false);
  const user = JSON.parse(localStorage.getItem("user") || "{}");
  const location = useLocation();
  const navigate = useNavigate();
  const current = links.find((item) => item.path === location.pathname)?.label || "System administration";
  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    navigate("/login", { replace: true });
  }
  return <div className="admin-layout system-admin-layout">
    {open && <button className="admin-sidebar-overlay" aria-label="Close navigation" onClick={() => setOpen(false)} />}
    <aside className={`admin-sidebar ${open ? "is-open" : ""}`}>
      <div className="admin-brand"><div className="admin-brand-icon"><GraduationCap size={26} /></div><div className="admin-brand-text"><strong>HostelHub</strong><span>System Administration</span></div><button className="admin-mobile-close" onClick={() => setOpen(false)} aria-label="Close sidebar"><X size={21} /></button></div>
      <p className="admin-menu-label">MAIN ADMIN</p>
      <nav className="admin-navigation" aria-label="System administrator navigation">
        {links.map(({ label, path, icon: Icon, end }) => {
          const isLocked = Boolean(user?.mustChangePassword && path !== "/system-admin/settings");
          return (
            <NavLink
              key={path}
              to={isLocked ? "#" : path}
              end={end}
              onClick={(e) => {
                if (isLocked) {
                  e.preventDefault();
                  navigate("/system-admin/settings");
                  return;
                }
                setOpen(false);
              }}
              className={({ isActive }) => `admin-nav-link ${isActive && !isLocked ? "active" : ""}`}
              title={isLocked ? "Set a permanent password in Settings to unlock this feature" : undefined}
              style={isLocked ? { opacity: 0.55, cursor: "not-allowed" } : undefined}
            >
              <Icon size={19} />
              <span>{label}</span>
              {isLocked ? (
                <Lock size={14} style={{ marginLeft: "auto", opacity: 0.7 }} />
              ) : (
                location.pathname === path && <ChevronRight className="admin-nav-chevron" size={16} />
              )}
            </NavLink>
          );
        })}
      </nav>
      <div className="admin-sidebar-bottom"><div className="admin-user-card"><div className="admin-avatar">{(user.fullName || "A").charAt(0).toUpperCase()}</div><div className="admin-user-details"><strong>{user.fullName || "Administrator"}</strong><span>{user.email}</span><small>SYSTEM ADMINISTRATOR</small></div></div><button className="admin-logout" onClick={logout}><LogOut size={18} /><span>Sign out</span></button></div>
    </aside>
    <div className="admin-workspace">
      <header className="admin-topbar">
        <div className="admin-topbar-left">
          <button className="admin-mobile-menu" onClick={() => setOpen(true)} aria-label="Open navigation"><Menu size={22} /></button>
          <div><div className="admin-breadcrumb">Administration <ChevronRight size={14} /> {current}</div><h1>{current}</h1></div>
        </div>
        <div className="admin-topbar-account"><div className="admin-status-dot" /><span>Main system administrator</span></div>
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
                You are signed in with a temporary password. All management features are locked until you set a permanent password in Settings.
              </span>
            </div>
          </div>
          {location.pathname !== "/system-admin/settings" && (
            <button
              type="button"
              onClick={() => navigate("/system-admin/settings")}
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
      <main className="admin-page-content"><Outlet /></main>
      <footer className="admin-footer">Hostel registry and system administration</footer>
    </div>
  </div>;
}
