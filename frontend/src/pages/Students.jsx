import { useEffect, useMemo, useState } from "react";
import {
  Users,
  UserPlus,
  Search,
  RefreshCw,
  Pencil,
  X,
  Mail,
  Phone,
  GraduationCap,
  CalendarDays,
  AlertCircle,
  Trash2,
} from "lucide-react";
import { apiRequest } from "../services/api";
import "./Students.css";

const emptyStudent = {
  fullName: "",
  email: "",
  password: "",
  studentCode: "",
  hostelName: "",
  phone: "",
  course: "",
  studyYear: "",
  guardianName: "",
  guardianPhone: "",
  admissionDate: "",
};

function getSignedInUser() {
  try {
    return JSON.parse(localStorage.getItem("user") || "null");
  } catch {
    return null;
  }
}

function getStudentList(response) {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.content)) return response.content;
  if (Array.isArray(response?.data)) return response.data;
  if (Array.isArray(response?.students)) return response.students;
  return [];
}

function getStudentId(student) {
  return student.id ?? student.studentId;
}

export default function Students() {
  const currentUser = getSignedInUser();
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [form, setForm] = useState(emptyStudent);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [hostels, setHostels] = useState([]);
  const [deletingId, setDeletingId] = useState(null);

  const loadStudents = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await apiRequest("/students");
      setStudents(getStudentList(response));
    } catch (err) {
      console.error("Could not load students:", err);
      setError(
        err?.message || "Unable to load students. Check your login and backend connection."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void Promise.resolve().then(loadStudents);
  }, []);

  const filteredStudents = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) return students;

    return students.filter((student) =>
      [
        student.fullName,
        student.name,
        student.email,
        student.studentCode,
        student.phone,
        student.course,
      ].some((value) => String(value ?? "").toLowerCase().includes(query))
    );
  }, [students, search]);

  const openAddForm = async () => {
    setSelectedStudent(null);
    setForm(emptyStudent);
    setError("");
    setSuccess("");
    setShowForm(true);
    if (!currentUser?.hostelName) {
      try {
        const response = await apiRequest("/admin/hostels");
        const availableHostels = getStudentList(response);
        setHostels(availableHostels);
        setForm((previous) => ({
          ...previous,
          hostelName: availableHostels[0]?.name || "",
        }));
      } catch (err) {
        setError(err?.message || "Could not load registered hostels.");
      }
    }
  };

  const deleteStudent = async (student) => {
    const id = getStudentId(student);
    if (id == null || deletingId != null) return;
    const name = student.fullName ?? student.name ?? "this student";
    if (!window.confirm(`Permanently delete ${name}? Their room allocations, attendance history, and settled fee records will also be removed. Students with pending or unpaid fee balance cannot be deleted.`)) return;
    setDeletingId(id);
    setError("");
    setSuccess("");
    try {
      await apiRequest(`/students/${id}`, { method: "DELETE" });
      setSuccess(`${name} and their records were deleted.`);
      await loadStudents();
    } catch (err) {
      setError(err?.message || "Could not delete this student.");
    } finally {
      setDeletingId(null);
    }
  };

  const openEditForm = (student) => {
    setSelectedStudent(student);
    setForm({
      fullName: student.fullName ?? student.name ?? "",
      email: student.email ?? "",
      password: "",
      studentCode: student.studentCode ?? "",
      hostelName: student.hostelName ?? "",
      phone: student.phone ?? "",
      course: student.course ?? "",
      studyYear: student.studyYear ?? "",
      guardianName: student.guardianName ?? "",
      guardianPhone: student.guardianPhone ?? "",
      admissionDate: student.admissionDate
        ? String(student.admissionDate).slice(0, 10)
        : "",
    });
    setError("");
    setSuccess("");
    setShowForm(true);
  };

  const closeForm = () => {
    if (saving) return;
    setShowForm(false);
    setSelectedStudent(null);
    setForm(emptyStudent);
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

    const payload = {
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      studentCode: form.studentCode.trim(),
      phone: form.phone.trim(),
      course: form.course.trim(),
      studyYear: Number(form.studyYear),
      guardianName: form.guardianName.trim(),
      guardianPhone: form.guardianPhone.trim(),
      admissionDate: form.admissionDate || null,
    };

    if (!payload.fullName || !payload.email || !payload.studentCode) {
      setError("Full name, email, and student code are required.");
      return;
    }

    if (!selectedStudent && !form.password.trim()) {
      setError("Enter a password to register the student.");
      return;
    }

    if (!selectedStudent) {
      if (form.password.length < 8) {
        setError("Student password must contain at least 8 characters.");
        return;
      }
      const hostelName = currentUser?.hostelName || form.hostelName.trim();
      if (!hostelName) {
        setError("Enter the hostel that this student belongs to.");
        return;
      }
      payload.hostelName = hostelName;
      payload.password = form.password;
    }

    setSaving(true);

    try {
      if (selectedStudent) {
        // The backend must implement PUT /api/students/{id}.
        await apiRequest(`/students/${getStudentId(selectedStudent)}`, {
          method: "PUT",
          body: payload,
        });
        setSuccess("Student updated successfully.");
      } else {
        await apiRequest("/students", {
          method: "POST",
          body: payload,
        });
        setSuccess("Student registered successfully.");
      }

      setShowForm(false);
      setSelectedStudent(null);
      setForm(emptyStudent);
      await loadStudents();
    } catch (err) {
      console.error("Student save failed:", err);
      setError(
        err?.message ||
          (selectedStudent
            ? "Could not update student. Verify that the update endpoint exists."
            : "Could not register student. Check the form and backend.")
      );
    } finally {
      setSaving(false);
    }
  };

  const formatDate = (date) => {
    if (!date) return "—";

    const parsedDate = new Date(`${String(date).slice(0, 10)}T00:00:00`);

    if (Number.isNaN(parsedDate.getTime())) return String(date);

    return parsedDate.toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  };

  return (
    <main className="students-page">
      <header className="students-header">
        <div>
          <div className="students-eyebrow">
            <Users size={15} />
            HOSTEL MANAGEMENT SYSTEM
          </div>
          <h1>Student Management</h1>
          <p>Register students and manage student information.</p>
        </div>

        <button
          className="students-primary-btn"
          type="button"
          onClick={openAddForm}
        >
          <UserPlus size={18} />
          Add Student
        </button>
      </header>

      {success && (
        <div className="students-success">
          <span>{success}</span>
          <button
            type="button"
            aria-label="Dismiss success message"
            onClick={() => setSuccess("")}
          >
            <X size={17} />
          </button>
        </div>
      )}

      {error && !showForm && (
        <div className="students-error" role="alert">
          <AlertCircle size={18} />
          <span>{error}</span>
          <button type="button" onClick={loadStudents}>
            Retry
          </button>
        </div>
      )}

      <section className="students-overview">
        <div className="students-overview-icon">
          <Users size={24} />
        </div>
        <div>
          <p>Total registered students</p>
          <h2>{students.length}</h2>
        </div>
        <div className="students-overview-note">
          <GraduationCap size={18} />
          Student records
        </div>
      </section>

      <section className="students-table-card">
        <div className="students-table-toolbar">
          <div>
            <h2>All Students</h2>
            <p>View and search registered student records.</p>
          </div>

          <button
            type="button"
            className="students-refresh-btn"
            onClick={loadStudents}
            disabled={loading}
            aria-label="Refresh students"
          >
            <RefreshCw
              size={17}
              className={loading ? "students-spinning" : ""}
            />
            Refresh
          </button>
        </div>

        <div className="students-search">
          <Search size={18} />
          <input
            type="search"
            placeholder="Search name, email, student code, phone..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            aria-label="Search students"
          />
          {search && (
            <button
              type="button"
              onClick={() => setSearch("")}
              aria-label="Clear search"
            >
              <X size={17} />
            </button>
          )}
        </div>

        <div className="students-table-wrapper">
          <table className="students-table">
            <thead>
              <tr>
                <th>Student</th>
                <th>Student Code</th>
                <th>Contact</th>
                <th>Course</th>
                <th>Study Year</th>
                <th>Admission Date</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="students-table-message">
                    <span className="students-inline-spinner" />
                    Loading students...
                  </td>
                </tr>
              ) : filteredStudents.length === 0 ? (
                <tr>
                  <td colSpan={7} className="students-table-message">
                    <Users size={30} />
                    <strong>
                      {search ? "No matching students" : "No students found"}
                    </strong>
                    <span>
                      {search
                        ? "Try a different search term."
                        : "Click Add Student to register the first student."}
                    </span>
                  </td>
                </tr>
              ) : (
                filteredStudents.map((student, index) => (
                  <tr key={getStudentId(student) ?? student.email ?? index}>
                    <td>
                      <div className="students-person">
                        <div className="students-avatar">
                          {(student.fullName ?? student.name ?? "S")
                            .trim()
                            .charAt(0)
                            .toUpperCase()}
                        </div>
                        <div className="students-person-info">
                          <strong>
                            {student.fullName ?? student.name ?? "Unnamed"}
                          </strong>
                          <span>
                            <Mail size={13} />
                            {student.email ?? "—"}
                          </span>
                        </div>
                      </div>
                    </td>

                    <td>
                      <span className="students-code">
                        {student.studentCode ?? "—"}
                      </span>
                    </td>

                    <td>
                      <span className="students-contact">
                        <Phone size={14} />
                        {student.phone ?? "—"}
                      </span>
                    </td>

                    <td>{student.course ?? "—"}</td>

                    <td>
                      {student.studyYear
                        ? `Year ${student.studyYear}`
                        : "—"}
                    </td>

                    <td>
                      <span className="students-date">
                        <CalendarDays size={14} />
                        {formatDate(student.admissionDate)}
                      </span>
                    </td>

                    <td>
                      <div className="students-actions">
                        <button
                          type="button"
                          className="students-action-btn"
                          onClick={() => openEditForm(student)}
                          title="Edit student"
                          aria-label={`Edit ${student.fullName ?? student.name ?? "student"}`}
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          type="button"
                          className="students-action-btn students-delete-btn"
                          onClick={() => deleteStudent(student)}
                          disabled={deletingId === getStudentId(student)}
                          title="Delete student and their room allocations"
                          aria-label={`Delete ${student.fullName ?? student.name ?? "student"}`}
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="students-table-footer">
          <span>
            Showing {filteredStudents.length} of {students.length} students
          </span>
        </div>
      </section>

      {showForm && (
        <div
          className="students-modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeForm();
          }}
        >
          <section
            className="students-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="student-form-title"
          >
            <header className="students-modal-header">
              <div>
                <div className="students-modal-icon">
                  {selectedStudent ? (
                    <Pencil size={21} />
                  ) : (
                    <UserPlus size={21} />
                  )}
                </div>
                <div>
                  <h2 id="student-form-title">
                    {selectedStudent ? "Edit Student" : "Register Student"}
                  </h2>
                  <p>
                    {selectedStudent
                      ? "Update the student information."
                      : "Enter the details to create a student record."}
                  </p>
                </div>
              </div>

              <button
                type="button"
                className="students-modal-close"
                onClick={closeForm}
                disabled={saving}
                aria-label="Close form"
              >
                <X size={21} />
              </button>
            </header>

            <form onSubmit={handleSubmit}>
              <div className="students-form-body">
                {error && (
                  <div className="students-error" role="alert">
                    <AlertCircle size={18} />
                    <span>{error}</span>
                  </div>
                )}

                <div className="students-form-section-title">
                  Personal Information
                </div>

                <div className="students-form-grid">
                  <label className="students-field">
                    Full Name *
                    <input
                      name="fullName"
                      value={form.fullName}
                      onChange={handleChange}
                      placeholder="Enter full name"
                      maxLength={100}
                      required
                    />
                  </label>

                  <label className="students-field">
                    Email Address *
                    <input
                      name="email"
                      type="email"
                      value={form.email}
                      onChange={handleChange}
                      placeholder="student@example.com"
                      maxLength={150}
                      required
                    />
                  </label>

                  {!selectedStudent && (
                    <label className="students-field">
                      Password *
                      <input
                        name="password"
                        type="password"
                        value={form.password}
                        onChange={handleChange}
                        placeholder="Create password"
                        minLength={8}
                        maxLength={72}
                        autoComplete="new-password"
                        required
                      />
                    </label>
                  )}

                  {!selectedStudent && !currentUser?.hostelName && (
                    <label className="students-field">
                      Hostel Name *
                      <select
                        name="hostelName"
                        value={form.hostelName}
                        onChange={handleChange}
                        required
                      >
                        <option value="">Select a registered hostel</option>
                        {hostels.map((hostel) => (
                          <option key={hostel.id} value={hostel.name}>{hostel.name}</option>
                        ))}
                      </select>
                      {hostels.length === 0 && (
                        <small className="students-field-help">
                          Create a hostel from Hostels before registering students.
                        </small>
                      )}
                    </label>
                  )}

                  <label className="students-field">
                    Student Code *
                    <input
                      name="studentCode"
                      value={form.studentCode}
                      onChange={handleChange}
                      placeholder="e.g. STU1001"
                      maxLength={30}
                      required
                    />
                  </label>

                  <label className="students-field">
                    Phone Number *
                    <input
                      name="phone"
                      type="tel"
                      value={form.phone}
                      onChange={handleChange}
                      placeholder="Enter phone number"
                      maxLength={20}
                      required
                    />
                  </label>

                  <label className="students-field">
                    Course *
                    <input
                      name="course"
                      value={form.course}
                      onChange={handleChange}
                      placeholder="e.g. B.Tech CSE"
                      maxLength={100}
                      required
                    />
                  </label>

                  <label className="students-field">
                    Study Year
                    <select
                      name="studyYear"
                      value={form.studyYear}
                      onChange={handleChange}
                      required
                    >
                      <option value="">Select year</option>
                      <option value="1">1st Year</option>
                      <option value="2">2nd Year</option>
                      <option value="3">3rd Year</option>
                      <option value="4">4th Year</option>
                      <option value="5">5th Year</option>
                    </select>
                  </label>

                  <label className="students-field">
                    Admission Date
                    <input
                      name="admissionDate"
                      type="date"
                      value={form.admissionDate}
                      onChange={handleChange}
                    />
                  </label>
                </div>

                <div className="students-form-section-title">
                  Guardian Information
                </div>

                <div className="students-form-grid">
                  <label className="students-field">
                    Guardian Name *
                    <input
                      name="guardianName"
                      value={form.guardianName}
                      onChange={handleChange}
                      placeholder="Enter guardian name"
                      maxLength={100}
                      required
                    />
                  </label>

                  <label className="students-field">
                    Guardian Phone *
                    <input
                      name="guardianPhone"
                      type="tel"
                      value={form.guardianPhone}
                      onChange={handleChange}
                      placeholder="Enter guardian phone"
                      maxLength={20}
                      required
                    />
                  </label>
                </div>

                <p className="students-required-note">
                  * Required fields
                </p>
              </div>

              <footer className="students-modal-footer">
                <button
                  type="button"
                  className="students-cancel-btn"
                  onClick={closeForm}
                  disabled={saving}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="students-primary-btn"
                  disabled={saving}
                >
                  {saving ? (
                    <>
                      <span className="students-button-spinner" />
                      Saving...
                    </>
                  ) : (
                    <>
                      {selectedStudent ? (
                        <Pencil size={17} />
                      ) : (
                        <UserPlus size={17} />
                      )}
                      {selectedStudent ? "Save Changes" : "Register Student"}
                    </>
                  )}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}
