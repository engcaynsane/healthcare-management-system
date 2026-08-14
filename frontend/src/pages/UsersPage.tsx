import { useCallback, useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { Badge } from "../components/Badge";
import { userApi, roleApi, branchApi } from "../lib/api-endpoints";
import type { User, Role, Branch } from "../lib/types";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";
import { hasRole } from "../lib/permissions";

export function UsersPage() {
  const [rows, setRows] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<User | null>(null);
  const [saving, setSaving] = useState(false);
  const isSuperAdmin = hasRole("SUPER_ADMIN");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, q]);

  useEffect(() => {
    roleApi.list().then(setRoles).catch(() => {});
    branchApi.all().then(setBranches).catch(() => {});
  }, []);

  async function load() {
    setLoading(true);
    try {
      const p = await userApi.list({ q, page, size: 20 });
      setRows(p.content);
      setTotalPages(p.totalPages);
      setTotal(p.totalElements);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load users");
    } finally {
      setLoading(false);
    }
  }

  const reload = useCallback(() => load(), [page, q]);

  async function handleSave(body: Record<string, unknown>) {
    setSaving(true);
    try {
      if (editing) {
        await userApi.update(editing.id, body as never);
        toast.success("User updated");
      } else {
        await userApi.create(body as never);
        toast.success("User created");
      }
      setOpen(false);
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to save user");
    } finally {
      setSaving(false);
    }
  }

  async function toggleActive(u: User) {
    try {
      if (u.active) {
        await userApi.deactivate(u.id);
        toast.success("User deactivated");
      } else {
        await userApi.activate(u.id);
        toast.success("User activated");
      }
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to update user");
    }
  }

  return (
    <div>
      <div className="toolbar">
        <div className="search">
          <input className="input" placeholder="Search users…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        </div>
        <div className="spacer" style={{ flex: 1 }} />
        <button className="btn btn-primary" onClick={() => { setEditing(null); setOpen(true); }}>
          <Icon name="plus" /> New User
        </button>
      </div>

      <DataTable<User>
        rows={rows}
        loading={loading}
        emptyMessage="No users found."
        columns={[
          { key: "username", header: "Username", render: (r) => <span className="mono">@{r.username}</span> },
          { key: "fullName", header: "Full Name", render: (r) => r.fullName },
          { key: "email", header: "Email", render: (r) => r.email || "—" },
          { key: "branch", header: "Branch", render: (r) => r.branchName || "—" },
          { key: "roles", header: "Roles", render: (r) => (
            <div style={{ display: "flex", gap: 4, flexWrap: "wrap" }}>
              {r.roles.length === 0 ? "—" : r.roles.map((role) => <span key={role} className="badge badge-info">{role}</span>)}
            </div>
          )},
          { key: "active", header: "Status", render: (r) => (r.active ? <Badge value="Active" tone="success" /> : <Badge value="Inactive" tone="muted" />) },
          { key: "actions", header: "", render: (r) => (
            <div className="actions">
              <button className="btn btn-sm" onClick={() => { setEditing(r); setOpen(true); }}>Edit</button>
              <button className={`btn btn-sm ${r.active ? "btn-danger" : "btn-success"}`} onClick={() => toggleActive(r)}>
                {r.active ? "Deactivate" : "Activate"}
              </button>
            </div>
          )},
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />

      <UserModal open={open} onClose={() => setOpen(false)} editing={editing} roles={roles} branches={branches} saving={saving} onSave={handleSave} showBranch={isSuperAdmin} />
    </div>
  );
}

function UserModal({
  open,
  onClose,
  editing,
  roles,
  branches,
  saving,
  onSave,
  showBranch,
}: {
  open: boolean;
  onClose: () => void;
  editing: User | null;
  roles: Role[];
  branches: Branch[];
  saving: boolean;
  onSave: (b: Record<string, unknown>) => void;
  showBranch: boolean;
}) {
  const [username, setUsername] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [roleCodes, setRoleCodes] = useState<string[]>([]);
  const [branchId, setBranchId] = useState("");
  const [active, setActive] = useState(true);

  useEffect(() => {
    if (open) {
      setUsername(editing?.username ?? "");
      setFullName(editing?.fullName ?? "");
      setEmail(editing?.email ?? "");
      setPhone(editing?.phone ?? "");
      setPassword("");
      setRoleCodes(editing?.roles ?? []);
      setBranchId(editing?.branchId != null ? String(editing.branchId) : "");
      setActive(editing?.active ?? true);
    }
  }, [open, editing]);

  function toggleRole(code: string) {
    setRoleCodes((r) => (r.includes(code) ? r.filter((x) => x !== code) : [...r, code]));
  }

  function submit() {
    if (!username || !fullName) {
      toast.error("Username and full name are required");
      return;
    }
    if (!editing && password.length < 6) {
      toast.error("Password must be at least 6 characters");
      return;
    }
    const base: Record<string, unknown> = {
      fullName,
      email: email || undefined,
      phone: phone || undefined,
      roleCodes,
      branchId: branchId ? Number(branchId) : undefined,
    };
    if (editing) {
      onSave({ ...base, active });
    } else {
      onSave({ ...base, username, password });
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      wide
      title={editing ? `Edit User ${editing.username}` : "New User"}
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Save"}</button>
        </>
      }
    >
      <div className="form-grid">
        {!editing && (
          <div className="field">
            <label>Username <span className="req">*</span></label>
            <input className="input" value={username} onChange={(e) => setUsername(e.target.value)} />
          </div>
        )}
        <div className="field">
          <label>Full Name <span className="req">*</span></label>
          <input className="input" value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </div>
        <div className="field">
          <label>Email</label>
          <input className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field">
          <label>Phone</label>
          <input className="input" value={phone} onChange={(e) => setPhone(e.target.value)} />
        </div>
        {!editing && (
          <div className="field">
            <label>Password <span className="req">*</span></label>
            <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
        )}
        {showBranch && (
          <div className="field">
            <label>Branch</label>
            <select className="select" value={branchId} onChange={(e) => setBranchId(e.target.value)}>
              <option value="">—</option>
              {branches.map((b) => (<option key={b.id} value={b.id}>{b.name}</option>))}
            </select>
          </div>
        )}
        <div className="field span-2">
          <label>Roles</label>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            {roles.map((r) => (
              <button key={r.id} type="button" className={`btn btn-sm ${roleCodes.includes(r.code) ? "btn-primary" : ""}`} onClick={() => toggleRole(r.code)}>
                {r.code}
              </button>
            ))}
          </div>
        </div>
        {editing && (
          <div className="field span-2">
            <div className="checkbox-row">
              <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} id="active" />
              <label htmlFor="active">Active</label>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}