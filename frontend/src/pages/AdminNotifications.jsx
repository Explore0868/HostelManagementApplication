import { useEffect, useState } from "react";
import { apiRequest } from "../services/api";
import "./Hostels.css";

export default function AdminNotifications() {
  const [events, setEvents] = useState([]);
  const [error, setError] = useState("");
  async function load() { try { setEvents(await apiRequest("/admin/notifications")); } catch (e) { setError(e.message); } }
  useEffect(() => { void load(); }, []);
  async function markRead(id) { try { await apiRequest(`/admin/notifications/${id}/read`, { method: "PATCH" }); await load(); } catch (e) { setError(e.message); } }
  return <main className="hostels-page"><header><span>SYSTEM ADMINISTRATION</span><h1>Notifications</h1><p>Hostel registry changes and owner security events.</p></header>
    {error && <p className="hostels-message" role="alert">{error}</p>}
    <section className="hostels-card"><h2>Recent events</h2>{events.length ? events.map((event) => <article className="hostel-item" key={event.id}>
      <div><h3>{event.eventType === "HOSTEL_NAME_CHANGED" ? "Hostel name changed" : `${event.ownerName} changed their password`}</h3>{event.eventType === "HOSTEL_NAME_CHANGED" ? <p><strong>Hostel ID:</strong> {event.hostelPublicId} · {event.previousHostelName} → {event.newHostelName}</p> : <p><strong>Owner account:</strong> {event.ownerUserId} · <strong>Hostel:</strong> {event.hostelName || "Unassigned"} ({event.hostelPublicId || "no hostel ID"})</p>}<p>{event.createdAt ? new Date(event.createdAt).toLocaleString() : ""} · {event.read ? "Read" : "Unread"}</p></div>
      {!event.read && <div className="hostel-actions"><button onClick={() => markRead(event.id)}>Mark read</button></div>}
    </article>) : <p>No notifications yet.</p>}</section>
  </main>;
}
