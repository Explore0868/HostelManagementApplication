
import { useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { LockKeyhole, Mail, Eye, EyeOff, GraduationCap } from "lucide-react";
import { apiRequest } from "../services/api";
import "./Login.css";

function normalizeRole(role) {
  return String(role || "")
    .replace(/^ROLE_/, "")
    .toUpperCase();
}

export default function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const result = await apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email: email.trim(), password }),
      });

      if (!result?.accessToken) {
        throw new Error("The server did not return an access token.");
      }

      const role = normalizeRole(result.role);

      if (!["ADMIN", "STUDENT"].includes(role)) {
        throw new Error("The server returned an unsupported user role.");
      }

      // Save only the fields needed by the frontend.
      localStorage.setItem("token", result.accessToken);
      localStorage.setItem(
        "user",
        JSON.stringify({
          userId: result.userId,
          fullName: result.fullName,
          email: result.email || email.trim(),
          role,
          hostelName: result.hostelName || "",
          systemAdmin: Boolean(result.systemAdmin),
        })
      );

      navigate(
        role === "ADMIN" ? (result.systemAdmin ? "/system-admin" : "/dashboard") : "/student-dashboard",
        { replace: true }
      );
    } catch (err) {
      setError(err.message || "Login failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  const savedToken = localStorage.getItem("token");

  let savedUser = null;
  try {
    savedUser = JSON.parse(localStorage.getItem("user") || "null");
  } catch {
    savedUser = null;
  }

  if (savedToken && savedUser) {
    const role = normalizeRole(savedUser.role);

    if (role === "ADMIN") {
      return <Navigate to={savedUser.systemAdmin ? "/system-admin" : "/dashboard"} replace />;
    }

    if (role === "STUDENT") {
      return <Navigate to="/student-dashboard" replace />;
    }
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <div className="login-brand-icon">
          <GraduationCap size={34} />
        </div>

        <p className="login-eyebrow">HOSTEL MANAGEMENT SYSTEM</p>
        <h1>Welcome back</h1>
        <p className="login-subtitle">
          Sign in to access your hostel portal.
        </p>

        <form className="login-form" onSubmit={handleSubmit}>
          <label htmlFor="email">Email address</label>

          <div className="login-input-wrap">
            <Mail size={19} aria-hidden="true" />
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              autoComplete="username"
              required
              disabled={loading}
            />
          </div>

          <label htmlFor="password">Password</label>

          <div className="login-input-wrap">
            <LockKeyhole size={19} aria-hidden="true" />
            <input
              id="password"
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter your password"
              autoComplete="current-password"
              required
              disabled={loading}
            />

            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword((visible) => !visible)}
              aria-label={showPassword ? "Hide password" : "Show password"}
            >
              {showPassword ? <EyeOff size={19} /> : <Eye size={19} />}
            </button>
          </div>

          {error && (
            <div className="login-error" role="alert">
              {error}
            </div>
          )}

          <button className="login-submit" type="submit" disabled={loading}>
            {loading ? "Signing in..." : "Sign in to your account"}
          </button>
        </form>

        <p className="login-footer">
          Secure access for hostel administrators and students.
        </p>
      </section>
    </main>
  );
}
