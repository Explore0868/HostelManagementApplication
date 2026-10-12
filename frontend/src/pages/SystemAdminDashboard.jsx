import { useEffect, useState } from "react";
import { Bell, Building2, Users } from "lucide-react";
import { apiRequest } from "../services/api";
import "./Hostels.css";

export default function SystemAdminDashboard() {
  const [hostels, setHostels] = useState([]);
  const [events, setEvents] = useState([]);
  const [error, setError] = useState("");
  useEffect(() => {
    Promise.all([apiRequest("/admin/hostels"), apiRequest("/admin/notifications")])
      .then(([items, notices]) => { setHostels(items); setEvents(notices); })
      .catch((e) => setError(e.message || "Unable to load system overview."));
  }, []);
  const students = hostels.reduce((sum, hostel) => sum + Number(hostel.studentCount || 0), 0);
  const unread = events.filter((event) => !event.read).length;
  return <main className="hostels-page"><header><span>SYSTEM ADMINISTRATION</span><h1>Overview</h1><p>Manage registered hostels and review changes across the system.</p></header>
    {error && <p className="hostels-message" role="alert">{error}</p>}
    <section className="system-admin-stats"><article><Building2 /><span>Registered hostels</span><strong>{hostels.length}</strong></article><article><Users /><span>Students across hostels</span><strong>{students}</strong></article><article><Bell /><span>Unread notifications</span><strong>{unread}</strong></article></section>
    <section className="hostels-card"><h2>Recent hostel updates</h2>{events.length ? events.slice(0, 5).map((event) => <article className="hostel-item" key={event.id}><div><h3>{event.eventType === "HOSTEL_NAME_CHANGED" ? "Hostel name changed" : "Owner security update"}</h3><p>{event.eventType === "HOSTEL_NAME_CHANGED" ? `${event.previousHostelName} → ${event.newHostelName} (${event.hostelPublicId})` : `${event.ownerName} updated account security · ${event.hostelName || "Unassigned"}`}</p><p>{event.createdAt ? new Date(event.createdAt).toLocaleString() : ""} · {event.read ? "Read" : "Unread"}</p></div></article>) : <p>No hostel updates yet.</p>}</section>
  </main>;
}
