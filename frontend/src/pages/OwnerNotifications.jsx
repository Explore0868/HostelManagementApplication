import { useEffect, useState } from "react";
import { Bell, RefreshCw } from "lucide-react";
import { apiRequest } from "../services/api";
import "./Hostels.css";

export default function OwnerNotifications() {
  const [notifications, setNotifications] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  async function load() {
    setLoading(true);
    try { setNotifications(await apiRequest("/owner/notifications")); setError(""); }
    catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, []);
  async function markRead(id) {
    try { await apiRequest(`/owner/notifications/${id}/read`, { method: "PATCH" }); await load(); }
    catch (e) { setError(e.message); }
  }
  return <main className="hostels-page"><header><span>HOSTEL OWNER WORKSPACE</span><h1>Notifications</h1><p>Updates for your account and assigned hostel.</p></header>
    {error && <p className="hostels-message" role="alert">{error}</p>}
    <section className="hostels-card"><div className="dashboard-section-heading"><div><h2>Recent notifications</h2></div><button className="dashboard-refresh" type="button" onClick={load} disabled={loading}><RefreshCw size={16}/> Refresh</button></div>
      {loading ? <p>Loading notifications…</p> : notifications.length ? notifications.map(item => <article className="hostel-item" key={item.id}>
        <div><h3><Bell size={16}/> {item.eventType.replaceAll("_", " ")}</h3><p>{item.message}</p><p>{item.hostelName} · {item.createdAt ? new Date(item.createdAt).toLocaleString() : ""} · {item.read ? "Read" : "Unread"}</p></div>
        {!item.read && <div className="hostel-actions"><button type="button" onClick={() => markRead(item.id)}>Mark read</button></div>}
      </article>) : <p>No notifications yet.</p>}
    </section>
  </main>;
}
