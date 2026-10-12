import { useState } from "react";
import { ChevronRight, GraduationCap, LayoutDashboard, LogOut, Menu, Building2, Bell, X } from "lucide-react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import "./AdminLayout.css";
import "./SystemAdminLayout.css";

const links = [
  { label: "Overview", path: "/system-admin", icon: LayoutDashboard, end: true },
  { label: "Hostels", path: "/system-admin/hostels", icon: Building2 },
  { label: "Notifications", path: "/system-admin/notifications", icon: Bell },
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
      <nav className="admin-navigation" aria-label="System administrator navigation">{links.map(({ label, path, icon: Icon, end }) => <NavLink key={path} to={path} end={end} onClick={() => setOpen(false)} className={({ isActive }) => `admin-nav-link ${isActive ? "active" : ""}`}><Icon size={19} /><span>{label}</span>{location.pathname === path && <ChevronRight className="admin-nav-chevron" size={16} />}</NavLink>)}</nav>
      <div className="admin-sidebar-bottom"><div className="admin-user-card"><div className="admin-avatar">{(user.fullName || "A").charAt(0).toUpperCase()}</div><div className="admin-user-details"><strong>{user.fullName || "Administrator"}</strong><span>{user.email}</span><small>SYSTEM ADMINISTRATOR</small></div></div><button className="admin-logout" onClick={logout}><LogOut size={18} /><span>Sign out</span></button></div>
    </aside>
    <div className="admin-workspace"><header className="admin-topbar"><div className="admin-topbar-left"><button className="admin-mobile-menu" onClick={() => setOpen(true)} aria-label="Open navigation"><Menu size={22} /></button><div><div className="admin-breadcrumb">Administration <ChevronRight size={14} /> {current}</div><h1>{current}</h1></div></div><div className="admin-topbar-account"><div className="admin-status-dot" /><span>Main system administrator</span></div></header><main className="admin-page-content"><Outlet /></main><footer className="admin-footer">Hostel registry and system administration</footer></div>
  </div>;
}
