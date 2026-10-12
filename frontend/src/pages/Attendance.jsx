
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  CalendarCheck,
  CheckCircle2,
  XCircle,
  Users,
  Search,
  RefreshCw,
  ClipboardCheck,
  AlertCircle,
  Save,
} from "lucide-react";
import { apiRequest } from "../services/api";
import "./Attendance.css";

const today = () => {
  const date = new Date();
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60000)
    .toISOString()
    .slice(0, 10);
};

const getList = (response) => {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.content)) return response.content;
  if (Array.isArray(response?.data)) return response.data;
  if (Array.isArray(response?.students)) return response.students;
  if (Array.isArray(response?.attendance)) return response.attendance;
  return [];
};

const getStudent = (record) =>
  record?.student ?? record?.studentDetails ?? {};

const getStudentName = (record) => {
  const student = getStudent(record);
  return (
    student?.user?.fullName ||
    student?.fullName ||
    student?.studentName ||
    record?.studentName ||
    "Student"
  );
};

const getStudentCode = (record) => {
  const student = getStudent(record);
  return (
    student?.studentCode ||
    student?.registrationNumber ||
    student?.rollNumber ||
    record?.studentCode ||
    (student?.id != null ? `STU-${student.id}` : "—")
  );
};

const getStudentId = (record) =>
  record?.studentId ?? getStudent(record)?.id;

const normalizeStatus = (status) =>
  String(status ?? "").toUpperCase();

export default function Attendance() {
  const [records, setRecords] = useState([]);
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [search, setSearch] = useState("");
  const [filterDate, setFilterDate] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL");

  const [form, setForm] = useState({
    studentId: "",
    attendanceDate: today(),
    status: "PRESENT",
  });

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const [attendanceResult, studentsResult] = await Promise.allSettled([
        apiRequest("/attendance"),
        apiRequest("/students"),
      ]);

      if (attendanceResult.status === "fulfilled") {
        setRecords(getList(attendanceResult.value));
      }
      if (studentsResult.status === "fulfilled") {
        setStudents(getList(studentsResult.value));
      }
      const failure = attendanceResult.status === "rejected"
        ? attendanceResult.reason
        : studentsResult.status === "rejected"
          ? studentsResult.reason
          : null;
      if (failure) throw failure;
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Unable to load attendance. Check your backend connection."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(loadData);
  }, [loadData]);

  const stats = useMemo(() => {
    const present = records.filter(
      (record) => normalizeStatus(record.status) === "PRESENT"
    ).length;
    const absent = records.filter(
      (record) => normalizeStatus(record.status) === "ABSENT"
    ).length;

    return {
      total: records.length,
      present,
      absent,
      rate:
        records.length > 0
          ? Math.round((present / records.length) * 100)
          : 0,
    };
  }, [records]);

  const filteredRecords = useMemo(() => {
    const query = search.trim().toLowerCase();

    return records.filter((record) => {
      const name = getStudentName(record).toLowerCase();
      const code = getStudentCode(record).toLowerCase();
      const id = String(getStudentId(record) ?? "");
      const status = normalizeStatus(record.status);
      const date = record.attendanceDate ?? record.date ?? "";

      const matchesSearch =
        !query ||
        name.includes(query) ||
        code.includes(query) ||
        id.includes(query);

      const matchesDate = !filterDate || date === filterDate;
      const matchesStatus =
        filterStatus === "ALL" || status === filterStatus;

      return matchesSearch && matchesDate && matchesStatus;
    });
  }, [records, search, filterDate, filterStatus]);

  const handleFormChange = (event) => {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
    setNotice("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setNotice("");

    if (!form.studentId || !form.attendanceDate || !form.status) {
      setError("Please select a student, date, and attendance status.");
      return;
    }

    setSaving(true);

    try {
      await apiRequest("/attendance", {
        method: "POST",
        body: {
          studentId: Number(form.studentId),
          attendanceDate: form.attendanceDate,
          status: form.status,
        },
      });

      setNotice("Attendance saved successfully.");
      await loadData();
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Unable to save attendance."
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <main className="attendance-page">
      <header className="attendance-header">
        <div>
          <span className="attendance-eyebrow">
            HOSTEL MANAGEMENT / ADMIN
          </span>
          <h1>Attendance Management</h1>
          <p>Record, review, and manage student attendance.</p>
        </div>

        <button
          type="button"
          className="attendance-refresh"
          onClick={loadData}
          disabled={loading}
        >
          <RefreshCw size={17} className={loading ? "is-spinning" : ""} />
          Refresh
        </button>
      </header>

      {error && (
        <div className="attendance-alert attendance-alert-error" role="alert">
          <AlertCircle size={19} />
          <span>{error}</span>
        </div>
      )}

      {notice && (
        <div className="attendance-alert attendance-alert-success" role="status">
          <CheckCircle2 size={19} />
          <span>{notice}</span>
        </div>
      )}

      <section className="attendance-stats">
        <article className="attendance-stat-card">
          <div className="attendance-stat-icon icon-purple">
            <ClipboardCheck size={22} />
          </div>
          <div>
            <span>Total Records</span>
            <strong>{stats.total}</strong>
            <small>All recorded entries</small>
          </div>
        </article>

        <article className="attendance-stat-card">
          <div className="attendance-stat-icon icon-green">
            <CheckCircle2 size={22} />
          </div>
          <div>
            <span>Present</span>
            <strong>{stats.present}</strong>
            <small>Marked present</small>
          </div>
        </article>

        <article className="attendance-stat-card">
          <div className="attendance-stat-icon icon-red">
            <XCircle size={22} />
          </div>
          <div>
            <span>Absent</span>
            <strong>{stats.absent}</strong>
            <small>Marked absent</small>
          </div>
        </article>

        <article className="attendance-stat-card">
          <div className="attendance-stat-icon icon-blue">
            <Users size={22} />
          </div>
          <div>
            <span>Present Rate</span>
            <strong>{stats.rate}%</strong>
            <small>Across recorded entries</small>
          </div>
        </article>
      </section>

      <section className="attendance-panel">
        <div className="attendance-panel-heading">
          <div>
            <h2>Mark Attendance</h2>
            <p>Choose a student, date, and attendance status.</p>
          </div>
          <div className="attendance-heading-icon">
            <CalendarCheck size={22} />
          </div>
        </div>

        <form className="attendance-form" onSubmit={handleSubmit}>
          <div className="attendance-field">
            <label htmlFor="attendance-student">Student</label>
            <select
              id="attendance-student"
              name="studentId"
              value={form.studentId}
              onChange={handleFormChange}
              required
            >
              <option value="">Select a student</option>
              {students.map((student) => {
                const id = student.id ?? student.studentId;
                const name =
                  student.user?.fullName ||
                  student.fullName ||
                  student.studentName ||
                  student.name ||
                  `Student ${id}`;

                if (id == null) return null;

                return (
                  <option key={id} value={id}>
                    {name}
                    {student.studentCode ? ` (${student.studentCode})` : ""}
                  </option>
                );
              })}
            </select>
            {students.length === 0 && !loading && (
              <small className="attendance-help">
                No students were returned by the API.
              </small>
            )}
          </div>

          <div className="attendance-field">
            <label htmlFor="attendance-date">Attendance Date</label>
            <input
              id="attendance-date"
              type="date"
              name="attendanceDate"
              value={form.attendanceDate}
              onChange={handleFormChange}
              required
            />
          </div>

          <div className="attendance-field">
            <label htmlFor="attendance-status">Status</label>
            <select
              id="attendance-status"
              name="status"
              value={form.status}
              onChange={handleFormChange}
              required
            >
              <option value="PRESENT">Present</option>
              <option value="ABSENT">Absent</option>
            </select>
          </div>

          <div className="attendance-form-action">
            <button type="submit" disabled={saving || loading}>
              <Save size={17} />
              {saving ? "Saving..." : "Save Attendance"}
            </button>
          </div>
        </form>
        <p className="attendance-form-note">
          Saving a student again for the same date updates that day's status.
        </p>
      </section>

      <section className="attendance-panel attendance-records-panel">
        <div className="attendance-panel-heading records-heading">
          <div>
            <h2>Attendance Records</h2>
            <p>Search and filter recorded student attendance.</p>
          </div>
          <span className="attendance-record-count">
            {filteredRecords.length} records
          </span>
        </div>

        <div className="attendance-filters">
          <div className="attendance-search">
            <Search size={18} />
            <input
              type="search"
              placeholder="Search student name or ID..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              aria-label="Search attendance"
            />
          </div>

          <div className="attendance-filter-control">
            <label htmlFor="filter-attendance-date">Date</label>
            <input
              id="filter-attendance-date"
              type="date"
              value={filterDate}
              onChange={(event) => setFilterDate(event.target.value)}
            />
          </div>

          <div className="attendance-filter-control">
            <label htmlFor="filter-attendance-status">Status</label>
            <select
              id="filter-attendance-status"
              value={filterStatus}
              onChange={(event) => setFilterStatus(event.target.value)}
            >
              <option value="ALL">All statuses</option>
              <option value="PRESENT">Present</option>
              <option value="ABSENT">Absent</option>
            </select>
          </div>

          <button
            type="button"
            className="attendance-clear-filters"
            onClick={() => {
              setSearch("");
              setFilterDate("");
              setFilterStatus("ALL");
            }}
          >
            Clear
          </button>
        </div>

        <div className="attendance-table-wrap">
          <table className="attendance-table">
            <thead>
              <tr>
                <th>Student</th>
                <th>Student ID</th>
                <th>Date</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="4" className="attendance-empty">
                    Loading attendance records...
                  </td>
                </tr>
              ) : filteredRecords.length === 0 ? (
                <tr>
                  <td colSpan="4" className="attendance-empty">
                    <CalendarCheck size={28} />
                    <strong>No attendance records found</strong>
                    <span>Mark attendance or change your filters.</span>
                  </td>
                </tr>
              ) : (
                filteredRecords.map((record) => {
                  const status = normalizeStatus(record.status);
                  const id = record.id;
                  const studentId = getStudentId(record);
                  const date =
                    record.attendanceDate ?? record.date ?? "—";

                  return (
                    <tr key={id ?? `${studentId}-${date}`}>
                      <td>
                        <div className="attendance-student-cell">
                          <span className="attendance-avatar">
                            {getStudentName(record).charAt(0).toUpperCase()}
                          </span>
                          <strong>{getStudentName(record)}</strong>
                        </div>
                      </td>
                      <td>
                        <span className="attendance-student-code">
                          {getStudentCode(record)}
                        </span>
                      </td>
                      <td>
                        {date === "—"
                          ? date
                          : new Date(`${date}T00:00:00`).toLocaleDateString(
                              "en-IN",
                              {
                                day: "2-digit",
                                month: "short",
                                year: "numeric",
                              }
                            )}
                      </td>
                      <td>
                        <span
                          className={`attendance-status-badge ${
                            status === "PRESENT"
                              ? "status-present"
                              : status === "ABSENT"
                              ? "status-absent"
                              : "status-unknown"
                          }`}
                        >
                          {status === "PRESENT" ? (
                            <CheckCircle2 size={14} />
                          ) : status === "ABSENT" ? (
                            <XCircle size={14} />
                          ) : null}
                          {status || "UNKNOWN"}
                        </span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div className="attendance-table-footer">
          Showing {filteredRecords.length} of {records.length} records
        </div>
      </section>
    </main>
  );
}
