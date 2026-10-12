
import { useCallback, useEffect, useState } from "react";
import {
  Plus,
  Search,
  RefreshCw,
  CreditCard,
  Receipt,
  Wallet,
  Clock,
  X,
  History,
  IndianRupee,
} from "lucide-react";
import { apiRequest } from "../services/api";
import "./Fees.css";

const emptyFeeForm = {
  studentId: "",
  feeType: "HOSTEL",
  totalAmount: "",
  dueDate: "",
};

const emptyPaymentForm = {
  feeRecordId: "",
  amount: "",
  paymentMethod: "UPI",
  transactionReference: "",
};

function getList(response, keys = []) {
  if (Array.isArray(response)) return response;

  for (const key of keys) {
    if (Array.isArray(response?.[key])) return response[key];
  }

  if (Array.isArray(response?.content)) return response.content;
  if (Array.isArray(response?.data)) return response.data;

  return [];
}

function formatAmount(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value ?? 0));
}

function getErrorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    "Something went wrong. Please try again."
  );
}

function getStatusClass(status) {
  return String(status || "PENDING").toLowerCase();
}

export default function Fees() {
  const [fees, setFees] = useState([]);
  const [students, setStudents] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [feeModalOpen, setFeeModalOpen] = useState(false);
  const [paymentModalOpen, setPaymentModalOpen] = useState(false);
  const [feeForm, setFeeForm] = useState(emptyFeeForm);
  const [paymentForm, setPaymentForm] = useState(emptyPaymentForm);

  const [historyFee, setHistoryFee] = useState(null);
  const [payments, setPayments] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const [feeResponse, studentResponse] = await Promise.all([
        apiRequest("/fees"),
        apiRequest("/students"),
      ]);

      setFees(getList(feeResponse, ["fees", "feeRecords"]));
      setStudents(getList(studentResponse, ["students"]));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(loadData);
  }, [loadData]);

  const filteredFees = fees.filter((fee) => {
    const searchText = search.trim().toLowerCase();

    if (!searchText) return true;

    return [
      fee.studentName,
      fee.studentCode,
      fee.feeType,
      fee.status,
      fee.id,
    ].some((value) =>
      String(value ?? "").toLowerCase().includes(searchText)
    );
  });

  const totalFees = fees.reduce(
    (sum, fee) => sum + Number(fee.totalAmount || 0),
    0
  );

  const totalPaid = fees.reduce(
    (sum, fee) => sum + Number(fee.paidAmount || 0),
    0
  );

  const totalBalance = fees.reduce(
    (sum, fee) => sum + Number(fee.balanceAmount || 0),
    0
  );

  const pendingCount = fees.filter(
    (fee) => Number(fee.balanceAmount || 0) > 0
  ).length;

  function openFeeModal() {
    setError("");
    setSuccess("");
    setFeeForm(emptyFeeForm);
    setFeeModalOpen(true);
  }

  function openPaymentModal(fee) {
    setError("");
    setSuccess("");
    setPaymentForm({
      ...emptyPaymentForm,
      feeRecordId: String(fee.id),
      amount: String(fee.balanceAmount ?? ""),
    });
    setPaymentModalOpen(true);
  }

  async function handleCreateFee(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!feeForm.studentId) {
      setError("Please select a student.");
      return;
    }

    if (Number(feeForm.totalAmount) <= 0) {
      setError("Fee amount must be greater than zero.");
      return;
    }

    setSubmitting(true);

    try {
      await apiRequest("/fees", {
        method: "POST",
        body: {
          studentId: Number(feeForm.studentId),
          feeType: feeForm.feeType,
          totalAmount: Number(feeForm.totalAmount),
          dueDate: feeForm.dueDate,
        },
      });

      setFeeModalOpen(false);
      setFeeForm(emptyFeeForm);
      setSuccess("Fee record created successfully.");
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRecordPayment(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (Number(paymentForm.amount) <= 0) {
      setError("Payment amount must be greater than zero.");
      return;
    }

    const selectedFee = fees.find(
      (fee) => String(fee.id) === String(paymentForm.feeRecordId)
    );

    if (
      selectedFee &&
      Number(paymentForm.amount) > Number(selectedFee.balanceAmount)
    ) {
      setError(
        `Payment cannot exceed the outstanding balance of ${formatAmount(
          selectedFee.balanceAmount
        )}.`
      );
      return;
    }

    setSubmitting(true);

    try {
      await apiRequest("/fees/payments", {
        method: "POST",
        body: {
          feeRecordId: Number(paymentForm.feeRecordId),
          amount: Number(paymentForm.amount),
          paymentMethod: paymentForm.paymentMethod,
          transactionReference:
            paymentForm.transactionReference.trim() || null,
        },
      });

      setPaymentModalOpen(false);
      setPaymentForm(emptyPaymentForm);
      setSuccess("Payment recorded successfully.");
      await loadData();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function openPaymentHistory(fee) {
    setHistoryFee(fee);
    setPayments([]);
    setHistoryLoading(true);
    setError("");

    try {
      const response = await apiRequest(`/fees/${fee.id}/payments`);
      setPayments(getList(response, ["payments", "paymentHistory"]));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setHistoryLoading(false);
    }
  }

  function closeModals() {
    setFeeModalOpen(false);
    setPaymentModalOpen(false);
    setHistoryFee(null);
    setPayments([]);
  }

  return (
    <div className="fees-page">
      <div className="fees-page-header">
        <div>
          <p className="fees-eyebrow">FINANCE MANAGEMENT</p>
          <h1>Fees & Payments</h1>
          <p className="fees-subtitle">
            Manage hostel fees, record payments, and track outstanding balances.
          </p>
        </div>

        <div className="fees-header-actions">
          <button
            className="fees-button fees-button-secondary"
            onClick={loadData}
            disabled={loading}
            type="button"
          >
            <RefreshCw size={17} />
            Refresh
          </button>

          <button
            className="fees-button fees-button-primary"
            onClick={openFeeModal}
            type="button"
          >
            <Plus size={18} />
            Create Fee
          </button>
        </div>
      </div>

      {error && (
        <div className="fees-alert fees-alert-error" role="alert">
          <span>{error}</span>
          <button
            type="button"
            aria-label="Dismiss error"
            onClick={() => setError("")}
          >
            <X size={17} />
          </button>
        </div>
      )}

      {success && (
        <div className="fees-alert fees-alert-success" role="status">
          <span>{success}</span>
          <button
            type="button"
            aria-label="Dismiss message"
            onClick={() => setSuccess("")}
          >
            <X size={17} />
          </button>
        </div>
      )}

      <div className="fees-stat-grid">
        <div className="fees-stat-card">
          <div className="fees-stat-icon fees-icon-purple">
            <Receipt size={22} />
          </div>
          <div>
            <p>Total Fees</p>
            <h2>{formatAmount(totalFees)}</h2>
            <span>{fees.length} fee records</span>
          </div>
        </div>

        <div className="fees-stat-card">
          <div className="fees-stat-icon fees-icon-green">
            <Wallet size={22} />
          </div>
          <div>
            <p>Total Collected</p>
            <h2>{formatAmount(totalPaid)}</h2>
            <span>Payments recorded</span>
          </div>
        </div>

        <div className="fees-stat-card">
          <div className="fees-stat-icon fees-icon-orange">
            <IndianRupee size={22} />
          </div>
          <div>
            <p>Outstanding</p>
            <h2>{formatAmount(totalBalance)}</h2>
            <span>{pendingCount} fee records with balance</span>
          </div>
        </div>

        <div className="fees-stat-card">
          <div className="fees-stat-icon fees-icon-blue">
            <CreditCard size={22} />
          </div>
          <div>
            <p>Collection Rate</p>
            <h2>
              {totalFees > 0
                ? `${Math.min(100, (totalPaid / totalFees) * 100).toFixed(1)}%`
                : "0%"}
            </h2>
            <span>Paid against total fees</span>
          </div>
        </div>
      </div>

      <section className="fees-table-card">
        <div className="fees-table-header">
          <div>
            <h2>Fee Records</h2>
            <p>Review student fee details and payment status.</p>
          </div>

          <div className="fees-search">
            <Search size={18} />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search student, code, fee..."
              aria-label="Search fee records"
            />
          </div>
        </div>

        <div className="fees-table-wrap">
          <table className="fees-table">
            <thead>
              <tr>
                <th>Student</th>
                <th>Fee Type</th>
                <th>Total Amount</th>
                <th>Paid</th>
                <th>Balance</th>
                <th>Due Date</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="8" className="fees-table-message">
                    Loading fee records...
                  </td>
                </tr>
              ) : filteredFees.length === 0 ? (
                <tr>
                  <td colSpan="8" className="fees-table-message">
                    No fee records found. Create a fee record to get started.
                  </td>
                </tr>
              ) : (
                filteredFees.map((fee) => (
                  <tr key={fee.id}>
                    <td>
                      <div className="fees-student-cell">
                        <div className="fees-avatar">
                          {String(fee.studentName || "S")
                            .trim()
                            .charAt(0)
                            .toUpperCase()}
                        </div>
                        <div>
                          <strong>{fee.studentName || "Unknown Student"}</strong>
                          <span>{fee.studentCode || `ID: ${fee.studentId}`}</span>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className="fees-type">{fee.feeType}</span>
                    </td>
                    <td>{formatAmount(fee.totalAmount)}</td>
                    <td className="fees-paid-amount">
                      {formatAmount(fee.paidAmount)}
                    </td>
                    <td className="fees-balance-amount">
                      {formatAmount(fee.balanceAmount)}
                    </td>
                    <td>{fee.dueDate || "—"}</td>
                    <td>
                      <span
                        className={`fees-status ${getStatusClass(fee.status)}`}
                      >
                        {fee.status || "PENDING"}
                      </span>
                    </td>
                    <td>
                      <div className="fees-row-actions">
                        {Number(fee.balanceAmount) > 0 && (
                          <button
                            type="button"
                            className="fees-action-button fees-pay-action"
                            onClick={() => openPaymentModal(fee)}
                            title="Record payment"
                          >
                            <CreditCard size={16} />
                            Pay
                          </button>
                        )}
                        <button
                          type="button"
                          className="fees-action-button"
                          onClick={() => openPaymentHistory(fee)}
                          title="View payment history"
                        >
                          <History size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="fees-table-footer">
          Showing {filteredFees.length} of {fees.length} fee records
        </div>
      </section>

      {feeModalOpen && (
        <div className="fees-modal-overlay" onMouseDown={closeModals}>
          <section
            className="fees-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="create-fee-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="fees-modal-header">
              <div>
                <h2 id="create-fee-title">Create Fee Record</h2>
                <p>Assign a fee to a student.</p>
              </div>
              <button
                className="fees-close-button"
                type="button"
                onClick={() => setFeeModalOpen(false)}
                aria-label="Close"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateFee}>
              <label>
                Student
                <select
                  required
                  value={feeForm.studentId}
                  onChange={(event) =>
                    setFeeForm({ ...feeForm, studentId: event.target.value })
                  }
                >
                  <option value="">Select a student</option>
                  {students.map((student) => (
                    <option
                      key={student.id ?? student.studentId}
                      value={student.id ?? student.studentId}
                    >
                      {student.fullName ||
                        student.studentName ||
                        student.name ||
                        "Student"}{" "}
                      — {student.studentCode || student.email || student.id}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Fee Type
                <select
                  required
                  value={feeForm.feeType}
                  onChange={(event) =>
                    setFeeForm({ ...feeForm, feeType: event.target.value })
                  }
                >
                  <option value="HOSTEL">Hostel Fee</option>
                  <option value="MESS">Mess Fee</option>
                  <option value="SECURITY_DEPOSIT">Security Deposit</option>
                  <option value="MAINTENANCE">Maintenance Fee</option>
                  <option value="OTHER">Other</option>
                </select>
              </label>

              <label>
                Total Amount (₹)
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  required
                  value={feeForm.totalAmount}
                  onChange={(event) =>
                    setFeeForm({
                      ...feeForm,
                      totalAmount: event.target.value,
                    })
                  }
                  placeholder="Enter fee amount"
                />
              </label>

              <label>
                Due Date
                <input
                  type="date"
                  required
                  value={feeForm.dueDate}
                  onChange={(event) =>
                    setFeeForm({ ...feeForm, dueDate: event.target.value })
                  }
                />
              </label>

              <div className="fees-modal-actions">
                <button
                  type="button"
                  className="fees-button fees-button-secondary"
                  onClick={() => setFeeModalOpen(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="fees-button fees-button-primary"
                  disabled={submitting}
                >
                  {submitting ? "Saving..." : "Create Fee"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}

      {paymentModalOpen && (
        <div className="fees-modal-overlay" onMouseDown={closeModals}>
          <section
            className="fees-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="record-payment-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="fees-modal-header">
              <div>
                <h2 id="record-payment-title">Record Payment</h2>
                <p>
                  {fees.find(
                    (fee) =>
                      String(fee.id) === String(paymentForm.feeRecordId)
                  )?.studentName || "Student fee payment"}
                </p>
              </div>
              <button
                className="fees-close-button"
                type="button"
                onClick={() => setPaymentModalOpen(false)}
                aria-label="Close"
              >
                <X size={20} />
              </button>
            </div>

            {(() => {
              const selectedFee = fees.find(
                (fee) =>
                  String(fee.id) === String(paymentForm.feeRecordId)
              );

              return (
                <div className="fees-payment-summary">
                  <div>
                    <span>Total Fee</span>
                    <strong>{formatAmount(selectedFee?.totalAmount)}</strong>
                  </div>
                  <div>
                    <span>Already Paid</span>
                    <strong>{formatAmount(selectedFee?.paidAmount)}</strong>
                  </div>
                  <div>
                    <span>Balance Due</span>
                    <strong>
                      {formatAmount(selectedFee?.balanceAmount)}
                    </strong>
                  </div>
                </div>
              );
            })()}

            <form onSubmit={handleRecordPayment}>
              <label>
                Payment Amount (₹)
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  required
                  value={paymentForm.amount}
                  onChange={(event) =>
                    setPaymentForm({
                      ...paymentForm,
                      amount: event.target.value,
                    })
                  }
                  placeholder="Enter payment amount"
                />
              </label>

              <label>
                Payment Method
                <select
                  required
                  value={paymentForm.paymentMethod}
                  onChange={(event) =>
                    setPaymentForm({
                      ...paymentForm,
                      paymentMethod: event.target.value,
                    })
                  }
                >
                  <option value="UPI">UPI</option>
                  <option value="CASH">Cash</option>
                  <option value="CARD">Card</option>
                  <option value="BANK_TRANSFER">Bank Transfer</option>
                  <option value="OTHER">Other</option>
                </select>
              </label>

              <label>
                Transaction Reference (optional)
                <input
                  type="text"
                  maxLength="100"
                  value={paymentForm.transactionReference}
                  onChange={(event) =>
                    setPaymentForm({
                      ...paymentForm,
                      transactionReference: event.target.value,
                    })
                  }
                  placeholder="Enter transaction ID or receipt number"
                />
              </label>

              <div className="fees-payment-note">
                <Clock size={16} />
                This records a payment entry; it does not process money through
                a payment gateway.
              </div>

              <div className="fees-modal-actions">
                <button
                  type="button"
                  className="fees-button fees-button-secondary"
                  onClick={() => setPaymentModalOpen(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="fees-button fees-button-primary"
                  disabled={submitting}
                >
                  {submitting ? "Saving..." : "Record Payment"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}

      {historyFee && (
        <div className="fees-modal-overlay" onMouseDown={closeModals}>
          <section
            className="fees-modal fees-history-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="payment-history-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="fees-modal-header">
              <div>
                <h2 id="payment-history-title">Payment History</h2>
                <p>
                  {historyFee.studentName || "Student"} ·{" "}
                  {historyFee.feeType} · Fee #{historyFee.id}
                </p>
              </div>
              <button
                className="fees-close-button"
                type="button"
                onClick={() => setHistoryFee(null)}
                aria-label="Close"
              >
                <X size={20} />
              </button>
            </div>

            {historyLoading ? (
              <p className="fees-history-message">Loading payment history...</p>
            ) : payments.length === 0 ? (
              <p className="fees-history-message">
                No payment entries found for this fee.
              </p>
            ) : (
              <div className="fees-history-list">
                {payments.map((payment) => (
                  <div className="fees-history-item" key={payment.id}>
                    <div className="fees-history-icon">
                      <CreditCard size={18} />
                    </div>
                    <div className="fees-history-details">
                      <strong>{formatAmount(payment.amount)}</strong>
                      <span>
                        {payment.paymentMethod || "Method not specified"} ·{" "}
                        {payment.transactionReference || "No reference"}
                      </span>
                      <small>
                        {payment.paymentDate
                          ? new Date(payment.paymentDate).toLocaleString(
                              "en-IN"
                            )
                          : "Date unavailable"}
                      </small>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="fees-modal-actions">
              <button
                type="button"
                className="fees-button fees-button-secondary"
                onClick={() => setHistoryFee(null)}
              >
                Close
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
