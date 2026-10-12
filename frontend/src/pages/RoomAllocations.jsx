
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  BedDouble,
  RefreshCw,
  Search,
  UserPlus,
  UserMinus,
  Users,
  CheckCircle2,
  AlertCircle,
  X,
} from "lucide-react";
import { apiRequest } from "../services/api";
import "./RoomAllocations.css";

function getList(data, keys = []) {
  if (Array.isArray(data)) return data;

  for (const key of keys) {
    if (Array.isArray(data?.[key])) return data[key];
  }

  return [];
}

function getStudentId(student) {
  return student?.id ?? student?.studentId;
}

function getStudentName(student) {
  return (
    student?.fullName ??
    student?.studentName ??
    student?.user?.fullName ??
    student?.name ??
    "Unnamed student"
  );
}

function getRoomId(room) {
  return room?.id ?? room?.roomId;
}

function getRoomCapacity(room) {
  return Number(room?.capacity ?? 0);
}

function getErrorMessage(error) {
  return (
    error?.response?.data?.message ??
    error?.response?.data?.error ??
    error?.message ??
    "Something went wrong. Please try again."
  );
}

export default function RoomAllocations() {
  const [allocations, setAllocations] = useState([]);
  const [students, setStudents] = useState([]);
  const [rooms, setRooms] = useState([]);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [vacatingId, setVacatingId] = useState(null);

  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [studentId, setStudentId] = useState("");
  const [roomId, setRoomId] = useState("");
  const [bedNumber, setBedNumber] = useState("");

  const [notice, setNotice] = useState({
    type: "",
    message: "",
  });

  const loadData = useCallback(async () => {
    setLoading(true);
    setNotice({ type: "", message: "" });

    try {
      const [allocationData, studentData, roomData] =
        await Promise.all([
          apiRequest("/room-allocations"),
          apiRequest("/students"),
          apiRequest("/rooms"),
        ]);

      setAllocations(
        getList(allocationData, ["content", "data", "allocations"])
      );

      setStudents(
        getList(studentData, ["content", "data", "students"])
      );

      setRooms(getList(roomData, ["content", "data", "rooms"]));
    } catch (error) {
      setNotice({
        type: "error",
        message: getErrorMessage(error),
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(loadData);
  }, [loadData]);

  const activeAllocations = useMemo(
    () => allocations.filter((allocation) => allocation.active === true),
    [allocations]
  );

  const occupiedBedsByRoom = useMemo(() => {
    const result = {};

    activeAllocations.forEach((allocation) => {
      const id = allocation.roomId;
      if (id == null) return;

      if (!result[id]) result[id] = new Set();

      if (allocation.bedNumber != null) {
        result[id].add(Number(allocation.bedNumber));
      }
    });

    return result;
  }, [activeAllocations]);

  const selectedRoom = rooms.find(
    (room) => String(getRoomId(room)) === String(roomId)
  );

  const availableBeds = useMemo(() => {
    if (!selectedRoom) return [];

    const capacity = getRoomCapacity(selectedRoom);
    const occupied = occupiedBedsByRoom[getRoomId(selectedRoom)] ??
      new Set();

    return Array.from({ length: capacity }, (_, index) => index + 1)
      .filter((bed) => !occupied.has(bed));
  }, [selectedRoom, occupiedBedsByRoom]);

  const availableRooms = useMemo(
    () =>
      rooms.filter((room) => {
        const id = getRoomId(room);
        const capacity = getRoomCapacity(room);
        const occupied = occupiedBedsByRoom[id]?.size ?? 0;
        const active = room.active ?? room.isActive ?? true;

        return active && capacity > occupied;
      }),
    [rooms, occupiedBedsByRoom]
  );

  const studentsWithoutRoom = useMemo(
    () =>
      students.filter((student) => {
        const id = getStudentId(student);
        const active = student.active ?? student.isActive ?? true;

        return (
          active &&
          !activeAllocations.some(
            (allocation) =>
              String(allocation.studentId) === String(id)
          )
        );
      }),
    [students, activeAllocations]
  );

  const filteredAllocations = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) return activeAllocations;

    return activeAllocations.filter((allocation) =>
      [
        allocation.studentName,
        allocation.studentCode,
        allocation.roomNumber,
        allocation.bedNumber,
      ].some((value) =>
        String(value ?? "").toLowerCase().includes(query)
      )
    );
  }, [activeAllocations, search]);

  function openForm() {
    setStudentId("");
    setRoomId("");
    setBedNumber("");
    setNotice({ type: "", message: "" });
    setShowForm(true);
  }

  function closeForm() {
    if (saving) return;

    setShowForm(false);
    setStudentId("");
    setRoomId("");
    setBedNumber("");
  }

  async function handleAllocate(event) {
    event.preventDefault();

    if (!studentId || !roomId || !bedNumber) {
      setNotice({
        type: "error",
        message: "Please select a student, room, and available bed.",
      });
      return;
    }

    setSaving(true);
    setNotice({ type: "", message: "" });

    try {
      await apiRequest("/room-allocations", {
        method: "POST",
        body: {
          studentId: Number(studentId),
          roomId: Number(roomId),
          bedNumber: Number(bedNumber),
        },
      });

      setShowForm(false);
      setStudentId("");
      setRoomId("");
      setBedNumber("");

      setNotice({
        type: "success",
        message: "Room allocated successfully.",
      });

      await loadData();
    } catch (error) {
      setNotice({
        type: "error",
        message: getErrorMessage(error),
      });
    } finally {
      setSaving(false);
    }
  }

  async function handleVacate(allocation) {
    const confirmed = window.confirm(
      `Vacate bed ${allocation.bedNumber} in room ${allocation.roomNumber} for ${allocation.studentName}?`
    );

    if (!confirmed) return;

    setVacatingId(allocation.id);
    setNotice({ type: "", message: "" });

    try {
      await apiRequest(
        `/room-allocations/${allocation.id}/vacate`,
        { method: "PATCH" }
      );

      setNotice({
        type: "success",
        message: "Room vacated successfully.",
      });

      await loadData();
    } catch (error) {
      setNotice({
        type: "error",
        message: getErrorMessage(error),
      });
    } finally {
      setVacatingId(null);
    }
  }

  function handleRoomChange(value) {
    setRoomId(value);
    setBedNumber("");
  }

  return (
    <div className="allocations-page">
      <div className="allocations-header">
        <div>
          <span className="allocations-eyebrow">
            HOSTEL MANAGEMENT
          </span>
          <h1>Room Allocations</h1>
          <p>Assign students to rooms and manage occupied beds.</p>
        </div>

        <div className="allocations-header-actions">
          <button
            className="allocation-btn allocation-btn-secondary"
            onClick={loadData}
            disabled={loading}
            type="button"
          >
            <RefreshCw size={17} />
            Refresh
          </button>

          <button
            className="allocation-btn allocation-btn-primary"
            onClick={openForm}
            type="button"
          >
            <UserPlus size={18} />
            Allocate Room
          </button>
        </div>
      </div>

      {notice.message && (
        <div
          className={`allocation-notice ${notice.type}`}
          role="status"
        >
          {notice.type === "success" ? (
            <CheckCircle2 size={19} />
          ) : (
            <AlertCircle size={19} />
          )}

          <span>{notice.message}</span>

          <button
            type="button"
            aria-label="Dismiss message"
            onClick={() => setNotice({ type: "", message: "" })}
          >
            <X size={17} />
          </button>
        </div>
      )}

      <div className="allocation-stats">
        <div className="allocation-stat-card">
          <div className="allocation-stat-icon blue">
            <Users size={22} />
          </div>
          <div>
            <span>Active Allocations</span>
            <strong>{activeAllocations.length}</strong>
          </div>
        </div>

        <div className="allocation-stat-card">
          <div className="allocation-stat-icon green">
            <BedDouble size={22} />
          </div>
          <div>
            <span>Available Rooms</span>
            <strong>{availableRooms.length}</strong>
          </div>
        </div>

        <div className="allocation-stat-card">
          <div className="allocation-stat-icon purple">
            <UserPlus size={22} />
          </div>
          <div>
            <span>Students Without Rooms</span>
            <strong>{studentsWithoutRoom.length}</strong>
          </div>
        </div>
      </div>

      {showForm && (
        <div className="allocation-form-card">
          <div className="allocation-form-heading">
            <div>
              <h2>Allocate a Room</h2>
              <p>Select a student and assign an available bed.</p>
            </div>

            <button
              className="allocation-close"
              type="button"
              onClick={closeForm}
              aria-label="Close allocation form"
            >
              <X size={20} />
            </button>
          </div>

          <form onSubmit={handleAllocate}>
            <div className="allocation-form-grid">
              <label>
                Student <span>*</span>
                <select
                  value={studentId}
                  onChange={(event) => setStudentId(event.target.value)}
                  required
                >
                  <option value="">Select student</option>
                  {studentsWithoutRoom.map((student) => (
                    <option
                      key={getStudentId(student)}
                      value={getStudentId(student)}
                    >
                      {getStudentName(student)}
                      {student.studentCode
                        ? ` (${student.studentCode})`
                        : ""}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Room <span>*</span>
                <select
                  value={roomId}
                  onChange={(event) =>
                    handleRoomChange(event.target.value)
                  }
                  required
                >
                  <option value="">Select room</option>
                  {availableRooms.map((room) => (
                    <option
                      key={getRoomId(room)}
                      value={getRoomId(room)}
                    >
                      {room.roomNumber} —{" "}
                      {Math.max(
                        0,
                        getRoomCapacity(room) -
                          (occupiedBedsByRoom[getRoomId(room)]?.size ?? 0)
                      )}{" "}
                      bed(s) available
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Bed Number <span>*</span>
                <select
                  value={bedNumber}
                  onChange={(event) => setBedNumber(event.target.value)}
                  required
                  disabled={!roomId || availableBeds.length === 0}
                >
                  <option value="">Select bed</option>
                  {availableBeds.map((bed) => (
                    <option key={bed} value={bed}>
                      Bed {bed}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            {roomId && availableBeds.length === 0 && (
              <p className="allocation-inline-warning">
                No available beds were found for this room.
              </p>
            )}

            {studentsWithoutRoom.length === 0 && (
              <p className="allocation-inline-warning">
                No eligible students are available for allocation.
              </p>
            )}

            <div className="allocation-form-actions">
              <button
                className="allocation-btn allocation-btn-secondary"
                type="button"
                onClick={closeForm}
                disabled={saving}
              >
                Cancel
              </button>

              <button
                className="allocation-btn allocation-btn-primary"
                type="submit"
                disabled={
                  saving ||
                  !studentId ||
                  !roomId ||
                  !bedNumber
                }
              >
                {saving ? "Allocating..." : "Confirm Allocation"}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="allocation-list-card">
        <div className="allocation-list-header">
          <div>
            <h2>Current Allocations</h2>
            <p>{activeAllocations.length} active student allocation(s)</p>
          </div>

          <div className="allocation-search">
            <Search size={18} />
            <input
              type="search"
              placeholder="Search student, code, room..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
        </div>

        <div className="allocation-table-wrap">
          <table className="allocation-table">
            <thead>
              <tr>
                <th>Student</th>
                <th>Student Code</th>
                <th>Room</th>
                <th>Bed</th>
                <th>Allocation Date</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" className="allocation-empty">
                    Loading allocations...
                  </td>
                </tr>
              ) : filteredAllocations.length === 0 ? (
                <tr>
                  <td colSpan="7" className="allocation-empty">
                    {search
                      ? "No matching allocations found."
                      : "No active room allocations yet. Select Allocate Room to begin."}
                  </td>
                </tr>
              ) : (
                filteredAllocations.map((allocation) => (
                  <tr key={allocation.id}>
                    <td>
                      <div className="allocation-student-cell">
                        <div className="allocation-avatar">
                          {String(allocation.studentName ?? "S")
                            .trim()
                            .charAt(0)
                            .toUpperCase()}
                        </div>
                        <strong>
                          {allocation.studentName ?? "Unknown student"}
                        </strong>
                      </div>
                    </td>

                    <td>{allocation.studentCode ?? "—"}</td>

                    <td>
                      <span className="allocation-room-number">
                        {allocation.roomNumber ?? "—"}
                      </span>
                    </td>

                    <td>Bed {allocation.bedNumber ?? "—"}</td>

                    <td>
                      {allocation.allocationDate
                        ? String(allocation.allocationDate).slice(0, 10)
                        : "—"}
                    </td>

                    <td>
                      <span className="allocation-status">
                        <span />
                        Active
                      </span>
                    </td>

                    <td>
                      <button
                        className="allocation-vacate-btn"
                        type="button"
                        onClick={() => handleVacate(allocation)}
                        disabled={vacatingId === allocation.id}
                      >
                        <UserMinus size={15} />
                        {vacatingId === allocation.id
                          ? "Vacating..."
                          : "Vacate"}
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
