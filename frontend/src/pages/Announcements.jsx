
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Megaphone,
  Plus,
  Search,
  Trash2,
  RefreshCw,
  CalendarDays,
  X,
  AlertCircle,
  CheckCircle2,
} from "lucide-react";
import { apiRequest } from "../services/api";
import "./Announcements.css";

const getList = (response) => {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.content)) return response.content;
  if (Array.isArray(response?.data)) return response.data;
  return [];
};

const emptyForm = { title: "", category: "General", content: "" };

export default function Announcements() {
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadAnnouncements = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const response = await apiRequest("/announcements");
      setAnnouncements(getList(response));
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Could not load announcements."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(loadAnnouncements);
  }, [loadAnnouncements]);

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return announcements;

    return announcements.filter((item) =>
      [item.title, item.content, item.category]
        .some((value) => String(value || "").toLowerCase().includes(query))
    );
  }, [announcements, search]);

  const updateForm = (event) => {
    setForm((previous) => ({
      ...previous,
      [event.target.name]: event.target.value,
    }));
  };

  const createAnnouncement = async (event) => {
    event.preventDefault();
    setError("");
    setNotice("");

    if (!form.title.trim() || !form.content.trim()) {
      setError("Enter both a title and announcement content.");
      return;
    }

    setSaving(true);
    try {
      await apiRequest("/announcements", {
        method: "POST",
        body: {
          title: form.title.trim(),
          category: form.category,
          content: form.content.trim(),
        },
      });

      setForm(emptyForm);
      setShowForm(false);
      setNotice("Announcement published successfully.");
      await loadAnnouncements();
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Could not publish the announcement."
      );
    } finally {
      setSaving(false);
    }
  };

  const deleteAnnouncement = async (item) => {
    const confirmed = window.confirm(
      `Delete the announcement "${item.title}"?`
    );
    if (!confirmed) return;

    setError("");
    setNotice("");

    try {
      await apiRequest(`/announcements/${item.id}`, {
        method: "DELETE",
      });
      setNotice("Announcement deleted successfully.");
      await loadAnnouncements();
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Could not delete the announcement."
      );
    }
  };

  return (
    <main className="announcements-page">
      <header className="announcements-header">
        <div>
          <span className="announcements-eyebrow">HOSTEL MANAGEMENT / ADMIN</span>
          <h1>Announcements</h1>
          <p>Create and manage important hostel notices.</p>
        </div>

        <button
          className="announcement-primary-btn"
          onClick={() => {
            setShowForm(true);
            setError("");
            setNotice("");
          }}
          type="button"
        >
          <Plus size={18} />
          Create Announcement
        </button>
      </header>

      {error && (
        <div className="announcement-message error-message" role="alert">
          <AlertCircle size={18} />
          {error}
        </div>
      )}

      {notice && (
        <div className="announcement-message success-message" role="status">
          <CheckCircle2 size={18} />
          {notice}
        </div>
      )}

      <section className="announcement-overview">
        <div className="announcement-overview-icon">
          <Megaphone size={27} />
        </div>
        <div>
          <span>Total Announcements</span>
          <strong>{announcements.length}</strong>
          <p>All notices saved in the system</p>
        </div>
      </section>

      {showForm && (
        <section className="announcement-panel">
          <div className="announcement-panel-heading">
            <div>
              <h2>New Announcement</h2>
              <p>Enter the details of the notice you want to publish.</p>
            </div>
            <button
              type="button"
              className="announcement-icon-btn"
              aria-label="Close form"
              onClick={() => setShowForm(false)}
            >
              <X size={20} />
            </button>
          </div>

          <form onSubmit={createAnnouncement} className="announcement-form">
            <div className="announcement-form-grid">
              <div className="announcement-field">
                <label htmlFor="announcement-title">Title *</label>
                <input
                  id="announcement-title"
                  name="title"
                  value={form.title}
                  onChange={updateForm}
                  maxLength={150}
                  placeholder="e.g. Hostel Meeting"
                  required
                />
              </div>

              <div className="announcement-field">
                <label htmlFor="announcement-category">Category</label>
                <select
                  id="announcement-category"
                  name="category"
                  value={form.category}
                  onChange={updateForm}
                >
                  <option>General</option>
                  <option>Notice</option>
                  <option>Event</option>
                  <option>Maintenance</option>
                  <option>Emergency</option>
                  <option>Rules</option>
                </select>
              </div>
            </div>

            <div className="announcement-field">
              <label htmlFor="announcement-content">Announcement *</label>
              <textarea
                id="announcement-content"
                name="content"
                value={form.content}
                onChange={updateForm}
                rows={5}
                placeholder="Write the announcement details..."
                required
              />
            </div>

            <div className="announcement-form-actions">
              <button
                type="button"
                className="announcement-secondary-btn"
                onClick={() => {
                  setShowForm(false);
                  setForm(emptyForm);
                }}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="announcement-primary-btn"
                disabled={saving}
              >
                <Megaphone size={17} />
                {saving ? "Publishing..." : "Publish Announcement"}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="announcement-panel">
        <div className="announcement-list-heading">
          <div>
            <h2>All Announcements</h2>
            <p>Search, review, or delete existing notices.</p>
          </div>
          <button
            type="button"
            className="announcement-secondary-btn"
            onClick={loadAnnouncements}
            disabled={loading}
          >
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>

        <div className="announcement-search">
          <Search size={18} />
          <input
            type="search"
            placeholder="Search by title, content, or category..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            aria-label="Search announcements"
          />
        </div>

        {loading ? (
          <div className="announcement-empty">Loading announcements...</div>
        ) : filtered.length === 0 ? (
          <div className="announcement-empty">
            <Megaphone size={32} />
            <strong>No announcements found</strong>
            <span>
              {search
                ? "Try another search term."
                : "Create your first announcement using the button above."}
            </span>
          </div>
        ) : (
          <div className="announcement-list">
            {filtered.map((item) => (
              <article className="announcement-item" key={item.id}>
                <div className="announcement-item-icon">
                  <Megaphone size={20} />
                </div>

                <div className="announcement-item-content">
                  <div className="announcement-item-meta">
                    <span className="announcement-category">
                      {item.category || "General"}
                    </span>
                    <span className="announcement-date">
                      <CalendarDays size={14} />
                      {item.createdAt
                        ? new Date(item.createdAt).toLocaleDateString("en-IN", {
                            day: "2-digit",
                            month: "short",
                            year: "numeric",
                          })
                        : "Date unavailable"}
                    </span>
                  </div>
                  <div className="announcement-reaction-summary" aria-label="Student responses">
                    <span><strong>👍</strong> {item.thumbsUpCount || 0} accept</span>
                    <span><strong>👎</strong> {item.thumbsDownCount || 0} disagree</span>
                  </div>

                  <h3>{item.title}</h3>
                  <p>{item.content}</p>
                </div>

                <button
                  type="button"
                  className="announcement-delete-btn"
                  onClick={() => deleteAnnouncement(item)}
                  aria-label={`Delete ${item.title}`}
                  title="Delete announcement"
                >
                  <Trash2 size={18} />
                </button>
              </article>
            ))}
          </div>
        )}

        <div className="announcement-footer">
          Showing {filtered.length} of {announcements.length} announcements
        </div>
      </section>
    </main>
  );
}
