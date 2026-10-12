import { useEffect, useState } from "react";
import { Layers3, Plus, Pencil, Trash2, RefreshCw } from "lucide-react";
import { apiRequest } from "../services/api";
import "./Hostels.css";

export default function Blocks() {
  const [blocks, setBlocks] = useState([]);
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  async function load() {
    setLoading(true);
    try { setBlocks(await apiRequest("/blocks")); setMessage(""); }
    catch (error) { setMessage(error.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, []);
  async function add(event) {
    event.preventDefault(); setSaving(true); setMessage("");
    try { await apiRequest("/blocks", { method: "POST", body: { name } }); setName(""); setMessage("Block added."); await load(); }
    catch (error) { setMessage(error.message); }
    finally { setSaving(false); }
  }
  async function rename(block) {
    const next = window.prompt("Block name", block.name);
    if (!next?.trim()) return;
    try { await apiRequest("/blocks", { method: "PATCH", body: { currentName: block.name, name: next.trim() } }); setMessage("Block and its room assignments were updated."); await load(); }
    catch (error) { setMessage(error.message); }
  }
  async function remove(block) {
    if (!window.confirm(`Remove empty block “${block.name}”? Blocks with rooms cannot be removed.`)) return;
    try { await apiRequest(`/blocks?name=${encodeURIComponent(block.name)}`, { method: "DELETE" }); setMessage("Block removed."); await load(); }
    catch (error) { setMessage(error.message); }
  }
  return <main className="hostels-page">
    <header><span>HOSTEL OWNER WORKSPACE</span><h1>Blocks</h1><p>Manage blocks for your assigned hostel. Existing room blocks are shown automatically.</p></header>
    {message && <p className="hostels-message" role="status">{message}</p>}
    <section className="hostels-card"><h2>Add a block</h2><form className="hostels-form" onSubmit={add}>
      <label>Block name<input value={name} onChange={e => setName(e.target.value)} maxLength={50} required /></label>
      <button disabled={saving}><Plus size={16}/>{saving ? "Adding…" : "Add block"}</button>
    </form></section>
    <section className="hostels-card"><h2>Your hostel blocks</h2>
      <button type="button" className="dashboard-refresh" onClick={load} disabled={loading}><RefreshCw size={16}/> Refresh</button>
      {loading ? <p>Loading blocks…</p> : blocks.length ? <div className="hostels-list">{blocks.map(block => <article className="hostel-item" key={block.name}>
        <div><h3><Layers3 size={17}/> {block.name}</h3><p>{block.roomCount} {block.roomCount === 1 ? "room" : "rooms"}</p></div>
        <div className="hostel-actions"><button type="button" onClick={() => rename(block)}><Pencil size={15}/> Rename</button>{block.roomCount === 0 && <button type="button" className="danger" onClick={() => remove(block)}><Trash2 size={15}/> Remove</button>}</div>
      </article>)}</div> : <p>No blocks yet.</p>}
    </section>
  </main>;
}
