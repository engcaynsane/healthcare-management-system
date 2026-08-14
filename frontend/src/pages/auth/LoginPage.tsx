import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../../lib/api-endpoints";
import { useAuthStore } from "../../stores/auth";
import { ApiHttpError } from "../../lib/api";
import { toast } from "../../stores/toast";

export function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const data = await authApi.login(username.trim(), password);
      setSession(data.user, data.accessToken, data.refreshToken);
      toast.success(`Welcome back, ${data.user.fullName}`);
      navigate("/", { replace: true });
    } catch (err) {
      const message = err instanceof ApiHttpError ? err.message : "Unable to sign in";
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-screen">
      <div className="login-card">
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 14 }}>
          <div className="logo" style={{ width: 40, height: 40, borderRadius: 10 }}>
            H
          </div>
          <h1>Healthcare Management</h1>
        </div>
        <p>Sign in with your credentials to continue.</p>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              className="input"
              autoFocus
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="admin"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              className="input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>
          <button className="btn btn-primary" style={{ width: "100%", marginTop: 6 }} disabled={loading}>
            {loading ? "Signing in…" : "Sign in"}
          </button>
        </form>
      </div>
    </div>
  );
}