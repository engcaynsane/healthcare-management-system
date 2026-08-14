import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../stores/auth";
import { authApi, branchApi } from "../../lib/api-endpoints";
import type { Branch } from "../../lib/types";
import { toast } from "../../stores/toast";
import { ApiHttpError } from "../../lib/api";

export function ProfilePage() {
  const user = useAuthStore((s) => s.user);
  const setSession = useAuthStore((s) => s.setSession);
  const navigate = useNavigate();

  const [branches, setBranches] = useState<Branch[]>([]);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [pwError, setPwError] = useState("");
  const [pwSaving, setPwSaving] = useState(false);

  useEffect(() => {
    branchApi
      .all()
      .then(setBranches)
      .catch(() => {});
  }, []);

  async function handleChangePassword(e: React.FormEvent) {
    e.preventDefault();
    setPwError("");
    if (newPassword.length < 6) {
      setPwError("New password must be at least 6 characters.");
      return;
    }
    if (newPassword !== confirm) {
      setPwError("Passwords do not match.");
      return;
    }
    setPwSaving(true);
    try {
      await authApi.changePassword(oldPassword, newPassword);
      toast.success("Password changed successfully");
      setOldPassword("");
      setNewPassword("");
      setConfirm("");
    } catch (err) {
      setPwError(err instanceof ApiHttpError ? err.message : "Unable to change password");
    } finally {
      setPwSaving(false);
    }
  }

  async function switchBranch(branchId: number) {
    try {
      const data = await authApi.switchBranch(branchId);
      setSession(data.user, data.accessToken, data.refreshToken);
      toast.success(`Switched to ${data.user.branchName}`);
      navigate("/");
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Unable to switch branch");
    }
  }

  return (
    <div className="content" style={{ padding: 0, maxWidth: 760 }}>
      <div className="card" style={{ marginBottom: 18 }}>
        <div className="card-header">
          <span className="avatar" style={{ width: 48, height: 48, fontSize: 18 }}>
            {initials(user?.fullName)}
          </span>
          <div>
            <div style={{ fontWeight: 600, fontSize: 16 }}>{user?.fullName}</div>
            <div style={{ color: "var(--text-muted)", fontSize: 13 }}>@{user?.username}</div>
          </div>
        </div>
        <div className="card-body">
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(180px,1fr))", gap: 12 }}>
            <div>
              <div className="stat-label">Email</div>
              <div>{user?.email || "—"}</div>
            </div>
            <div>
              <div className="stat-label">Phone</div>
              <div>{user?.phone || "—"}</div>
            </div>
            <div>
              <div className="stat-label">Current Branch</div>
              <div>{user?.branchName || "—"}</div>
            </div>
            <div>
              <div className="stat-label">Roles</div>
              <div style={{ display: "flex", gap: 6, flexWrap: "wrap", marginTop: 4 }}>
                {user?.roles.map((r) => (
                  <span key={r} className="badge badge-info">
                    {r}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <div className="card-header">Switch Branch</div>
        <div className="card-body">
          {branches.length === 0 ? (
            <div className="empty">No branches available</div>
          ) : (
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill,minmax(220px,1fr))", gap: 12 }}>
              {branches.map((b) => (
                <div key={b.id} className="stat-card" style={{ padding: 14, cursor: "pointer" }} onClick={() => switchBranch(b.id)}>
                  <div style={{ fontWeight: 600 }}>{b.name}</div>
                  <div style={{ fontSize: 12, color: "var(--text-muted)" }}>{b.code}</div>
                  {b.central && <span className="badge badge-info">Central</span>}
                  {!b.active && <span className="badge badge-danger">Inactive</span>}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-header">Change Password</div>
        <div className="card-body">
          {pwError && <div className="alert alert-error">{pwError}</div>}
          <form onSubmit={handleChangePassword} className="form-grid" style={{ maxWidth: 480 }}>
            <div className="field">
              <label>Current Password</label>
              <input className="input" type="password" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} required />
            </div>
            <div className="field">
              <label>New Password</label>
              <input className="input" type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
            </div>
            <div className="field">
              <label>Confirm New Password</label>
              <input className="input" type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
            </div>
            <div className="field" style={{ justifyContent: "flex-end" }}>
              <button className="btn btn-primary" disabled={pwSaving} type="submit">
                {pwSaving ? "Saving…" : "Change Password"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

function initials(name?: string): string {
  if (!name) return "U";
  return name
    .split(" ")
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();
}