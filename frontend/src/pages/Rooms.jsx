
import { useEffect, useMemo, useState } from "react";
import {
  BedDouble,
  Plus,
  Search,
  RefreshCw,
  Pencil,
  X,
  DoorOpen,
  Users,
  AlertCircle,
} from "lucide-react";
import { apiRequest } from "../services/api";
import "./Rooms.css";

const emptyRoom = {
  roomNumber: "",
  roomType: "",
  blockName: "",
  hostelName: "",
  capacity: "",
  monthlyRent: "",
  description: "",
};

function getSignedInUser() {
  try {
    return JSON.parse(localStorage.getItem("user") || "null");
  } catch {
    return null;
  }
}

function getRooms(response) {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.content)) return response.content;
  if (Array.isArray(response?.data)) return response.data;
  if (Array.isArray(response?.rooms)) return response.rooms;
  return [];
}

function roomId(room) {
  return room.id ?? room.roomId;
}

export default function Rooms() {
  const currentUser = getSignedInUser();
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editingRoom, setEditingRoom] = useState(null);
  const [form, setForm] = useState(emptyRoom);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadRooms = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await apiRequest("/rooms");
      setRooms(getRooms(response));
    } catch (err) {
      console.error("Failed to load rooms:", err);
      setError(err?.message || "Could not load rooms. Check your backend and login.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void Promise.resolve().then(loadRooms);
  }, []);

  const filteredRooms = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) return rooms;

    return rooms.filter((room) =>
      [
        room.roomNumber,
        room.roomName,
        room.roomType,
        room.blockName,
        room.hostelName,
        room.description,
      ].some((value) => String(value ?? "").toLowerCase().includes(query))
    );
  }, [rooms, search]);

  const openAddForm = () => {
    setEditingRoom(null);
    setForm({ ...emptyRoom, hostelName: currentUser?.hostelName || "" });
    setError("");
    setShowForm(true);
  };

  const openEditForm = (room) => {
    setEditingRoom(room);
    setForm({
      roomNumber: room.roomNumber ?? "",
      roomType: room.roomType ?? "",
      blockName: room.blockName ?? "",
      hostelName: room.hostelName ?? "",
      capacity: room.capacity ?? "",
      monthlyRent: room.monthlyRent ?? room.rent ?? "",
      description: room.description ?? "",
    });
    setError("");
    setShowForm(true);
  };

  const closeForm = () => {
    if (saving) return;
    setShowForm(false);
    setEditingRoom(null);
    setForm(emptyRoom);
    setError("");
  };

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    const capacity = Number(form.capacity);
    const monthlyRent = Number(form.monthlyRent);

    if (!form.roomNumber.trim() || !form.roomType || !form.blockName.trim() || !form.capacity) {
      setError("Room number, room type, block name, and capacity are required.");
      return;
    }

    if (!(currentUser?.hostelName || form.hostelName.trim())) {
      setError("Enter the hostel name for this room.");
      return;
    }

    if (!Number.isInteger(capacity) || capacity < 1) {
      setError("Capacity must be a positive whole number.");
      return;
    }

    if (form.monthlyRent !== "" && (!Number.isFinite(monthlyRent) || monthlyRent < 0)) {
      setError("Rent must be a valid non-negative number.");
      return;
    }

    const payload = {
      roomNumber: form.roomNumber.trim(),
      roomType: form.roomType,
      blockName: form.blockName.trim(),
      hostelName: currentUser?.hostelName || form.hostelName.trim(),
      capacity,
      description: form.description.trim(),
    };

    if (form.monthlyRent !== "") {
      payload.monthlyRent = monthlyRent;
    }

    setSaving(true);

    try {
      if (editingRoom) {
        await apiRequest(`/rooms/${roomId(editingRoom)}`, {
          method: "PUT",
          body: payload,
        });
        setSuccess("Room updated successfully.");
      } else {
        await apiRequest("/rooms", {
          method: "POST",
          body: payload,
        });
        setSuccess("Room created successfully.");
      }

      setShowForm(false);
      setEditingRoom(null);
      setForm({ ...emptyRoom, hostelName: currentUser?.hostelName || "" });
      await loadRooms();
    } catch (err) {
      console.error("Room save failed:", err);
      setError(
        err?.message || "Could not save room. Check the room details and try again."
      );
    } finally {
      setSaving(false);
    }
  };

  const availableBeds = (room) =>
    room.availableBeds ??
    room.availableCapacity ??
    Math.max(0, Number(room.capacity || 0) - Number(room.occupiedBeds || 0));

  const occupiedBeds = (room) =>
    room.occupiedBeds ??
    room.occupiedCapacity ??
    Math.max(0, Number(room.capacity || 0) - availableBeds(room));

  return (
    <main className="rooms-page">
      <header className="rooms-header">
        <div>
          <div className="rooms-eyebrow">
            <BedDouble size={15} />
            HOSTEL MANAGEMENT SYSTEM
          </div>
          <h1>Room Management</h1>
          <p>Create, view, and manage hostel rooms.</p>
        </div>

        <button
          type="button"
          className="rooms-primary-btn"
          onClick={openAddForm}
        >
          <Plus size={18} />
          Add Room
        </button>
      </header>

      {success && (
        <div className="rooms-success">
          <span>{success}</span>
          <button type="button" onClick={() => setSuccess("")}>
            <X size={16} />
          </button>
        </div>
      )}

      {error && !showForm && (
        <div className="rooms-error" role="alert">
          <AlertCircle size={18} />
          <span>{error}</span>
          <button type="button" onClick={loadRooms}>
            Retry
          </button>
        </div>
      )}

      <section className="rooms-stat-grid">
        <article className="rooms-stat-card">
          <div className="rooms-stat-icon purple">
            <DoorOpen size={23} />
          </div>
          <div>
            <p>Total Rooms</p>
            <h2>{rooms.length}</h2>
          </div>
        </article>

        <article className="rooms-stat-card">
          <div className="rooms-stat-icon blue">
            <BedDouble size={23} />
          </div>
          <div>
            <p>Total Capacity</p>
            <h2>
              {rooms.reduce(
                (sum, room) => sum + Number(room.capacity || 0),
                0
              )}
            </h2>
          </div>
        </article>

        <article className="rooms-stat-card">
          <div className="rooms-stat-icon green">
            <Users size={23} />
          </div>
          <div>
            <p>Available Beds</p>
            <h2>
              {rooms.reduce(
                (sum, room) => sum + Number(availableBeds(room) || 0),
                0
              )}
            </h2>
          </div>
        </article>
      </section>

      <section className="rooms-table-card">
        <div className="rooms-toolbar">
          <div>
            <h2>All Rooms</h2>
            <p>Search and view your room inventory.</p>
          </div>

          <button
            type="button"
            className="rooms-refresh-btn"
            onClick={loadRooms}
            disabled={loading}
          >
            <RefreshCw
              size={16}
              className={loading ? "rooms-spinning" : ""}
            />
            Refresh
          </button>
        </div>

        <div className="rooms-search">
          <Search size={18} />
          <input
            type="search"
            placeholder="Search room number or type..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            aria-label="Search rooms"
          />
          {search && (
            <button type="button" onClick={() => setSearch("")}>
              <X size={16} />
            </button>
          )}
        </div>

        <div className="rooms-table-wrapper">
          <table className="rooms-table">
            <thead>
              <tr>
                <th>Room Number</th>
                <th>Block / Hostel</th>
                <th>Room Type</th>
                <th>Capacity</th>
                <th>Occupied</th>
                <th>Available</th>
                <th>Rent</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="rooms-empty">
                    Loading rooms...
                  </td>
                </tr>
              ) : filteredRooms.length === 0 ? (
                <tr>
                  <td colSpan={8} className="rooms-empty">
                    <BedDouble size={30} />
                    <strong>
                      {search ? "No matching rooms" : "No rooms found"}
                    </strong>
                    <span>
                      {search
                        ? "Try a different search."
                        : "Use Add Room to create your first room."}
                    </span>
                  </td>
                </tr>
              ) : (
                filteredRooms.map((room, index) => (
                  <tr key={roomId(room) ?? room.roomNumber ?? index}>
                    <td>
                      <div className="rooms-number">
                        <span className="rooms-number-icon">
                          <DoorOpen size={18} />
                        </span>
                        <strong>{room.roomNumber ?? room.roomName ?? "—"}</strong>
                      </div>
                    </td>
                    <td>
                      <span>{room.blockName || "—"}</span>
                      <small className="rooms-hostel-label">{room.hostelName || ""}</small>
                    </td>
                    <td>
                      <span className="rooms-type">
                        {room.roomType ?? "—"}
                      </span>
                    </td>
                    <td>{room.capacity ?? 0}</td>
                    <td>{occupiedBeds(room)}</td>
                    <td>
                      <span className="rooms-available">
                        {availableBeds(room)}
                      </span>
                    </td>
                    <td>
                      {room.monthlyRent != null || room.rent != null
                        ? `₹${room.monthlyRent ?? room.rent}`
                        : "—"}
                    </td>
                    <td>
                      <button
                        type="button"
                        className="rooms-action-btn"
                        onClick={() => openEditForm(room)}
                        aria-label={`Edit room ${room.roomNumber ?? ""}`}
                        title="Edit room"
                      >
                        <Pencil size={16} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <footer className="rooms-table-footer">
          Showing {filteredRooms.length} of {rooms.length} rooms
        </footer>
      </section>

      {showForm && (
        <div
          className="rooms-modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeForm();
          }}
        >
          <section
            className="rooms-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="rooms-form-title"
          >
            <header className="rooms-modal-header">
              <div>
                <div className="rooms-modal-icon">
                  {editingRoom ? <Pencil size={20} /> : <Plus size={22} />}
                </div>
                <div>
                  <h2 id="rooms-form-title">
                    {editingRoom ? "Edit Room" : "Add New Room"}
                  </h2>
                  <p>Enter the room details below.</p>
                </div>
              </div>

              <button
                type="button"
                className="rooms-close-btn"
                onClick={closeForm}
                disabled={saving}
                aria-label="Close form"
              >
                <X size={20} />
              </button>
            </header>

            <form onSubmit={handleSubmit}>
              <div className="rooms-form-body">
                {error && (
                  <div className="rooms-error" role="alert">
                    <AlertCircle size={18} />
                    <span>{error}</span>
                  </div>
                )}

                <label className="rooms-field">
                  Room Number *
                  <input
                    name="roomNumber"
                    value={form.roomNumber}
                    onChange={handleChange}
                    placeholder="e.g. A-101"
                    maxLength={20}
                    required
                  />
                </label>

                <label className="rooms-field">
                  Room Type *
                  <select
                    name="roomType"
                    value={form.roomType}
                    onChange={handleChange}
                    required
                  >
                    <option value="">Select room type</option>
                    <option value="SINGLE">Single</option>
                    <option value="DOUBLE">Double</option>
                    <option value="TRIPLE">Triple</option>
                    <option value="DORMITORY">Dormitory</option>
                  </select>
                </label>

                <div className="rooms-form-grid">
                  <label className="rooms-field">
                    Block Name *
                    <input
                      name="blockName"
                      value={form.blockName}
                      onChange={handleChange}
                      placeholder="e.g. North Wing"
                      maxLength={50}
                      required
                    />
                  </label>

                  {(!currentUser?.hostelName || editingRoom) && (
                    <label className="rooms-field">
                      Hostel Name {!editingRoom && "*"}
                      <input
                        name="hostelName"
                        value={form.hostelName}
                        onChange={handleChange}
                        placeholder="e.g. Hostel A"
                        readOnly={Boolean(editingRoom)}
                        required={!editingRoom}
                      />
                    </label>
                  )}
                </div>

                <div className="rooms-form-grid">
                  <label className="rooms-field">
                    Capacity *
                    <input
                      name="capacity"
                      type="number"
                      min="1"
                      step="1"
                      value={form.capacity}
                      onChange={handleChange}
                      placeholder="Number of beds"
                      required
                    />
                  </label>

                  <label className="rooms-field">
                    Monthly Rent (₹)
                    <input
                      name="monthlyRent"
                      type="number"
                      min="0"
                      step="0.01"
                      value={form.monthlyRent}
                      onChange={handleChange}
                      placeholder="Optional"
                    />
                  </label>
                </div>

                <label className="rooms-field">
                  Description
                  <textarea
                    name="description"
                    value={form.description}
                    onChange={handleChange}
                    placeholder="Optional room description"
                    rows={3}
                  />
                </label>

                <p className="rooms-form-note">
                  Room details are saved under the selected hostel. Hostel administrators can only manage rooms assigned to their own hostel.
                </p>
              </div>

              <footer className="rooms-modal-footer">
                <button
                  type="button"
                  className="rooms-cancel-btn"
                  onClick={closeForm}
                  disabled={saving}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="rooms-primary-btn"
                  disabled={saving}
                >
                  {saving ? "Saving..." : editingRoom ? "Save Changes" : "Create Room"}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}
