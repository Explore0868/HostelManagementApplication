
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Settings as SettingsIcon,
  ShieldCheck,
  ShieldAlert,
  UserPlus,
  KeyRound,
} from "lucide-react";
import { apiRequest } from "../services/api";
import "./Settings.css";

function getSignedInUser() {
  try {
    return JSON.parse(localStorage.getItem("user") || "null");
  } catch {
    return null;
  }
}

export default function Settings() {
  const navigate = useNavigate();
  const currentUser = getSignedInUser();
  const [activeTab, setActiveTab] = useState("password");
  const [saving, setSaving] = useState(false);

  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  const [adminForm, setAdminForm] = useState({
    fullName: "",
    email: "",
    password: "",
    hostelName: "",
  });

  const [message, setMessage] = useState("");

  const updatePassword = (event) => {
    setPasswordForm({
      ...passwordForm,
      [event.target.name]: event.target.value,
    });
  };

  const updateAdmin = (event) => {
    setAdminForm({
      ...adminForm,
      [event.target.name]: event.target.value,
    });
  };

  const submitPassword = async (event) => {
    event.preventDefault();
    setMessage("");

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setMessage("New passwords do not match.");
      return;
    }

    if (passwordForm.newPassword.length < 8) {
      setMessage("Password must contain at least 8 characters.");
      return;
    }

    const newPwd = passwordForm.newPassword;
    setSaving(true);

    try {
      await apiRequest("/account/password", {
        method: "PUT",
        body: {
          currentPassword: passwordForm.currentPassword,
          newPassword: newPwd,
        },
      });

      setPasswordForm({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      });

      // Automatically sign in with the new password to obtain a fresh JWT token
      // with mustChangePassword = false and full ROLE_ADMIN authorities
      try {
        const loginResult = await apiRequest("/auth/login", {
          method: "POST",
          body: {
            email: currentUser?.email,
            password: newPwd,
          },
        });

        if (loginResult?.accessToken) {
          localStorage.setItem("token", loginResult.accessToken);
          const role = String(loginResult.role || "").replace(/^ROLE_/, "").toUpperCase();
          const updatedUser = {
            userId: loginResult.userId,
            fullName: loginResult.fullName,
            email: loginResult.email || currentUser?.email,
            role,
            hostelName: loginResult.hostelName || currentUser?.hostelName || "",
            systemAdmin: Boolean(loginResult.systemAdmin),
            mustChangePassword: false,
          };
          localStorage.setItem("user", JSON.stringify(updatedUser));

          setMessage("Password updated successfully! Unlocking your dashboard...");
          setTimeout(() => {
            navigate(updatedUser.systemAdmin ? "/system-admin" : "/dashboard", { replace: true });
          }, 1000);
          return;
        }
      } catch {
        // If automatic re-login fails, redirect to the login screen with guidance
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        navigate("/login", {
          replace: true,
          state: { info: "Password changed successfully! Please log in with your new password." },
        });
        return;
      }

      setMessage("Password changed successfully. Please log in with your new password.");
    } catch (err) {
      setMessage(
        err?.message ||
        "Could not change password. Check your current password and try again."
      );
    } finally {
      setSaving(false);
    }
  };

const submitAdmin = async (event) => {
  event.preventDefault();
  setMessage("");

  if (adminForm.password.length < 8) {
    setMessage("Admin password must contain at least 8 characters.");
    return;
  }

  const hostelName = currentUser?.hostelName || adminForm.hostelName.trim();
  if (!hostelName) {
    setMessage("Enter the hostel this administrator will manage.");
    return;
  }

  setSaving(true);

  try {
    await apiRequest("/admin/accounts", {
      method: "POST",
      body: {
        fullName: adminForm.fullName.trim(),
        email: adminForm.email.trim(),
        password: adminForm.password,
        hostelName,
      },
    });

    setAdminForm({
      fullName: "",
      email: "",
      password: "",
      hostelName: "",
    });

    setMessage("Administrator account created successfully.");
  } catch (err) {
    setMessage(
      err?.message ||
      "Could not create administrator. Check your access and account details."
    );
  } finally {
    setSaving(false);
  }
};


  return (
    <main className="settings-page">
      <header className="settings-header">
        <div>
          <span className="settings-eyebrow">
            HOSTEL MANAGEMENT / ADMIN
          </span>
          <h1>Settings</h1>
          <p>Manage your account and administrator access.</p>
        </div>

        <div className="settings-header-icon">
          <SettingsIcon size={28} />
        </div>
      </header>

      {currentUser?.mustChangePassword ? (
        <section
          style={{
            margin: "0 0 1.5rem",
            padding: "1rem 1.25rem",
            backgroundColor: "#fffbeb",
            border: "1px solid #fde68a",
            borderRadius: "10px",
            display: "flex",
            alignItems: "center",
            gap: "0.85rem",
            color: "#92400e",
          }}
        >
          <ShieldAlert size={24} style={{ flexShrink: 0, color: "#d97706" }} />
          <div>
            <strong style={{ display: "block", fontSize: "0.95rem", color: "#78350f" }}>
              Action Required: Initial Account Setup
            </strong>
            <span style={{ fontSize: "0.875rem", color: "#92400e" }}>
              Please set your new permanent password below. Once saved, all hostel management features (Dashboard, Students, Rooms, Allocations, Attendance, Fees) will unlock automatically.
            </span>
          </div>
        </section>
      ) : (
        <section className="settings-security-note">
          <ShieldCheck size={22} />
          <div>
            <strong>Administrator security</strong>
            <p>
              Change your own password or create another
              administrator account.
            </p>
          </div>
        </section>
      )}

      {!currentUser?.mustChangePassword && (
        <nav className="settings-tabs">
          <button
            type="button"
            className={activeTab === "password" ? "active" : ""}
            onClick={() => {
              setActiveTab("password");
              setMessage("");
            }}
          >
            <KeyRound size={17} />
            Change Password
          </button>

          <button
            type="button"
            className={activeTab === "admins" ? "active" : ""}
            onClick={() => {
              setActiveTab("admins");
              setMessage("");
            }}
          >
            <UserPlus size={17} />
            Add Administrator
          </button>
        </nav>
      )}

      {message && (
        <div className="settings-message" role="status">
          {message}
        </div>
      )}

      {activeTab === "password" && (
        <section className="settings-card">
          <h2>Change Your Password</h2>
          <p className="settings-description">
            {currentUser?.mustChangePassword
              ? "Enter your temporary initial password and choose a secure permanent password (minimum 8 characters)."
              : "Update the password for your own administrator account."}
          </p>

          <form onSubmit={submitPassword}>
            <label htmlFor="currentPassword">
              Current Password
            </label>
            <input
              id="currentPassword"
              type="password"
              name="currentPassword"
              value={passwordForm.currentPassword}
              onChange={updatePassword}
              autoComplete="current-password"
              required
            />

            <label htmlFor="newPassword">New Password</label>
            <input
              id="newPassword"
              type="password"
              name="newPassword"
              value={passwordForm.newPassword}
              onChange={updatePassword}
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              required
            />

            <label htmlFor="confirmPassword">
              Confirm New Password
            </label>
            <input
              id="confirmPassword"
              type="password"
              name="confirmPassword"
              value={passwordForm.confirmPassword}
              onChange={updatePassword}
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              required
            />

            <button
                    className="settings-primary-button"
                    type="submit"
                    disabled={saving}
                    >
                    {saving ? "Updating..." : "Update My Password"}
            </button>
          </form>
        </section>
      )}

      {activeTab === "admins" && (
        <section className="settings-card">
          <h2>Create Administrator Account</h2>
          <p className="settings-description">
            The new administrator will only be able to manage the assigned hostel.
          </p>

          <form onSubmit={submitAdmin}>
            <label htmlFor="adminName">Full Name</label>
            <input
              id="adminName"
              name="fullName"
              value={adminForm.fullName}
              onChange={updateAdmin}
              autoComplete="name"
              required
            />

            <label htmlFor="adminEmail">Email Address</label>
            <input
              id="adminEmail"
              type="email"
              name="email"
              value={adminForm.email}
              onChange={updateAdmin}
              autoComplete="email"
              required
            />

            {currentUser?.hostelName ? (
              <p className="settings-description">
                Assigned hostel: <strong>{currentUser.hostelName}</strong>
              </p>
            ) : (
              <>
                <label htmlFor="adminHostel">Hostel Name</label>
                <input
                  id="adminHostel"
                  name="hostelName"
                  value={adminForm.hostelName}
                  onChange={updateAdmin}
                  placeholder="e.g. Hostel A"
                  maxLength={100}
                  required
                />
              </>
            )}

            <label htmlFor="adminPassword">Initial Password</label>
            <input
              id="adminPassword"
              type="password"
              name="password"
              value={adminForm.password}
              onChange={updateAdmin}
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              required
            />

            <button
                className="settings-primary-button"
                type="submit"
                disabled={saving}
                >
                <UserPlus size={17} />
                {saving ? "Creating..." : "Create Administrator"}
            </button>
          </form>
        </section>
      )}
    </main>
  );
}
