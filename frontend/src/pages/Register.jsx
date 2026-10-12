
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Building2, UserRound, Mail, LockKeyhole, Eye, EyeOff, ArrowRight
} from "lucide-react";

function Register() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [form, setForm] = useState({
    name: "", email: "", password: "", confirmPassword: ""
  });
  const [error, setError] = useState("");

  function updateField(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  function handleSubmit(e) {
    e.preventDefault();
    setError("");

    if (form.password.length < 6) {
      setError("Password must contain at least 6 characters.");
      return;
    }

    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    // Demo only: localStorage is not a secure account database.
    localStorage.setItem(
      "hostelUser",
      JSON.stringify({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password
      })
    );

    localStorage.setItem(
      "hostelSession",
      JSON.stringify({ email: form.email.trim() })
    );

    navigate("/dashboard");
  }

  return (
    <main className="auth-page">
      <section className="auth-visual">
        <div className="brand">
          <div className="brand-icon"><Building2 size={25} /></div>
          <span>Hostel<span className="brand-light">Ease</span></span>
        </div>

        <div className="visual-content">
          <span className="eyebrow">YOUR NEW BEGINNING</span>
          <h1>Make yourself<br /><span>at home.</span></h1>
          <p>
            Join your hostel community and make everyday student life
            easier with one simple platform.
          </p>
          <div className="visual-highlight">
            <div className="highlight-icon"><UserRound size={23} /></div>
            <div>
              <strong>Your space. Your community.</strong>
              <p>Start your hostel journey here.</p>
            </div>
          </div>
        </div>

        <div className="visual-footer">
          <span>●</span> Your hostel, all in one place
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-form-wrap">
          <div className="mobile-brand">
            <div className="brand-icon"><Building2 size={22} /></div>
            <strong>HostelEase</strong>
          </div>

          <div className="form-heading">
            <span className="eyebrow">GET STARTED</span>
            <h2>Create your account</h2>
            <p>Fill in your details to get started.</p>
          </div>

          <form onSubmit={handleSubmit} className="auth-form">
            <label htmlFor="name">Full name</label>
            <div className="input-wrap">
              <UserRound size={19} />
              <input
                id="name"
                name="name"
                placeholder="Enter your full name"
                autoComplete="name"
                value={form.name}
                onChange={updateField}
                required
              />
            </div>

            <label htmlFor="email">Email address</label>
            <div className="input-wrap">
              <Mail size={19} />
              <input
                id="email"
                name="email"
                type="email"
                placeholder="you@example.com"
                autoComplete="email"
                value={form.email}
                onChange={updateField}
                required
              />
            </div>

            <label htmlFor="password">Password</label>
            <div className="input-wrap">
              <LockKeyhole size={19} />
              <input
                id="password"
                name="password"
                type={showPassword ? "text" : "password"}
                placeholder="At least 6 characters"
                autoComplete="new-password"
                minLength={6}
                value={form.password}
                onChange={updateField}
                required
              />
              <button
                type="button"
                className="icon-button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={19} /> : <Eye size={19} />}
              </button>
            </div>

            <label htmlFor="confirmPassword">Confirm password</label>
            <div className="input-wrap">
              <LockKeyhole size={19} />
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                placeholder="Re-enter your password"
                autoComplete="new-password"
                value={form.confirmPassword}
                onChange={updateField}
                required
              />
            </div>

            {error && <p className="form-error">{error}</p>}

            <button type="submit" className="primary-button">
              Create Account <ArrowRight size={18} />
            </button>
          </form>

          <p className="auth-switch">
            Already registered? <Link to="/login">Sign in</Link>
          </p>
          <p className="form-note">
            Demo registration · Real account creation comes with backend integration
          </p>
        </div>
      </section>
    </main>
  );
}

export default Register;
