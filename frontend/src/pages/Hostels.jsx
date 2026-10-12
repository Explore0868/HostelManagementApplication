import { useEffect, useState } from "react";
import { apiRequest } from "../services/api";
import "./Hostels.css";

const emptyForm = { hostelName: "", ownerName: "", ownerEmail: "", initialPassword: "", address: "", contact: "" };

export default function Hostels() {
  const [form, setForm] = useState(emptyForm);
  const [hostels, setHostels] = useState([]);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  async function load() { try { setHostels(await apiRequest("/admin/hostels")); } catch (e) { setMessage(e.message); } }
  useEffect(() => { void load(); }, []);
  function change(e) { setForm((f) => ({ ...f, [e.target.name]: e.target.value })); }
  async function create(e) {
    e.preventDefault(); setMessage(""); setBusy(true);
    try { await apiRequest("/admin/hostels", { method: "POST", body: form }); setForm(emptyForm); setMessage("Hostel and owner account created. The owner must change the temporary password at first sign in."); await load(); }
    catch (err) { setMessage(err.message); } finally { setBusy(false); }
  }
  async function rename(hostel) {
    const name = window.prompt("New hostel name", hostel.name);
    if (!name || !name.trim()) return;
    try { await apiRequest(`/admin/hostels/${hostel.id}/name`, { method: "PATCH", body: { name: name.trim() } }); await load(); }
    catch (err) { setMessage(err.message); }
  }
  async function toggleOwner(hostel) {
    try { await apiRequest(`/admin/hostels/${hostel.id}/owner-status`, { method: "PATCH", body: { enabled: !hostel.ownerEnabled } }); await load(); }
    catch (err) { setMessage(err.message); }
  }
  async function updateOwner(hostel) {
    const name = window.prompt("Owner display name", hostel.ownerName || "");
    if (name === null || !name.trim()) return;
    const email = window.prompt("Owner login email", hostel.ownerEmail || "");
    if (email === null || !email.trim()) return;
    try { await apiRequest(`/admin/hostels/${hostel.id}/owner`, { method: "PATCH", body: { name: name.trim(), email: email.trim() } }); setMessage("Owner profile updated."); await load(); }
    catch (err) { setMessage(err.message); }
  }
  async function remove(hostel) {
    if (!window.confirm(`Remove hostel ${hostel.name} (${hostel.hostelId}) and its owner account? Records: ${hostel.blocks?.length || 0} blocks, ${hostel.roomCount || 0} rooms, ${hostel.studentCount || 0} students. The backend only permits deletion when no rooms or students remain.`)) return;
    try { await apiRequest(`/admin/hostels/${hostel.id}?confirm=true`, { method: "DELETE" }); await load(); }
    catch (err) { setMessage(err.message); }
  }
  return <main className="hostels-page">
    <header><span>SYSTEM ADMINISTRATION</span><h1>Hostels</h1><p>Create hostels with their owner accounts, then manage owner access.</p></header>
    {message && <p className="hostels-message" role="status">{message}</p>}
    <section className="hostels-card"><h2>Add a hostel and owner</h2>
      <form onSubmit={create} className="hostels-form">
        <label>Hostel name<input name="hostelName" value={form.hostelName} onChange={change} maxLength={100} required /></label>
        <label>Owner name<input name="ownerName" value={form.ownerName} onChange={change} maxLength={100} required /></label>
        <label>Owner login email<input name="ownerEmail" type="email" value={form.ownerEmail} onChange={change} maxLength={150} required /></label>
        <label>Temporary password<input name="initialPassword" type="password" value={form.initialPassword} onChange={change} minLength={8} maxLength={72} autoComplete="new-password" required /><small>Shown only while entering it. The owner must replace it at first sign in.</small></label>
        <label>Address<input name="address" value={form.address} onChange={change} maxLength={300} /></label>
        <label>Contact<input name="contact" value={form.contact} onChange={change} maxLength={40} /></label>
        <button disabled={busy}>{busy ? "Creating…" : "Create hostel and owner"}</button>
      </form>
    </section>
    <section className="hostels-card"><h2>Registered hostels</h2>
      {hostels.length === 0 ? <p>No hostels registered yet.</p> : <div className="hostels-list">{hostels.map((h) => <article key={h.id} className="hostel-item">
        <div><h3>{h.name}</h3><p><strong>Hostel ID:</strong> {h.hostelId}</p><p><strong>Owner:</strong> {h.ownerName} · {h.ownerEmail}</p><p><strong>Summary:</strong> {h.blocks?.length || 0} blocks, {h.roomCount} rooms, {h.studentCount} students{h.address ? ` · ${h.address}` : ""}</p>{h.blocks?.length > 0 && <p><strong>Blocks:</strong> {h.blocks.join(", ")}</p>}<p>Owner access: {h.ownerEnabled ? "Active" : "Disabled"}</p></div>
        <div className="hostel-actions"><button type="button" onClick={() => rename(h)}>Rename hostel</button><button type="button" onClick={() => updateOwner(h)}>Edit owner</button><button type="button" onClick={() => toggleOwner(h)}>{h.ownerEnabled ? "Disable owner" : "Enable owner"}</button><button type="button" className="danger" onClick={() => remove(h)}>Remove</button></div>
      </article>)}</div>}
    </section>
  </main>;
}
