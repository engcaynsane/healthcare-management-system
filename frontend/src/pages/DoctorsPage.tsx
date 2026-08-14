import { useCallback, useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { doctorApi, userApi } from "../lib/api-endpoints";
import type { Doctor, User } from "../lib/types";
import { formatMoney } from "../lib/format";
import { Badge } from "../components/Badge";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";
import { can } from "../lib/permissions";

export function DoctorsPage() {
  const [rows, setRows] = useState<Doctor[]>([]);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Doctor | null>(null);
  const [saving, setSaving] = useState(false);
  const [users, setUsers] = useState<User[]>([]);

  useEffect(() => {
    load();
  }, [q]);

  async function load() {
    setLoading(true);
    try {
      const data = await doctorApi.list(q);
      setRows(data);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load doctors");
    } finally {
      setLoading(false);
    }
  }

  const reload = useCallback(() => load(), [q]);

  const canAssign = can("doctor.assign");

  useEffect(() => {
    if (can("user.view")) {
      userApi.list({ size: 200 }).then((p) => setUsers(p.content)).catch(() => {});
    }
  }, []);

  function openCreate() {
    setEditing(null);
    setOpen(true);
  }
  function openEdit(d: Doctor) {
    setEditing(d);
    setOpen(true);
  }

  async function handleSave(body: Record<string, unknown>) {
    setSaving(true);
    try {
      if (editing) {
        await doctorApi.update(editing.id, body);
        toast.success("Doctor updated");
      } else {
        await doctorApi.create(body);
        toast.success("Doctor added");
      }
      setOpen(false);
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to save doctor");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <div className="toolbar">
        <div className="search">
          <input className="input" placeholder="Search name, specialty…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <div className="spacer" style={{ flex: 1 }} />
        {canAssign && (
          <button className="btn btn-primary" onClick={openCreate}>
            <Icon name="plus" /> Add Doctor
          </button>
        )}
      </div>

      <DataTable<Doctor>
        rows={rows}
        loading={loading}
        emptyMessage="No doctors found."
        columns={[
          { key: "name", header: "Name", render: (r) => r.fullName },
          { key: "specialty", header: "Specialty", render: (r) => r.specialty || "—" },
          { key: "license", header: "License", render: (r) => <span className="mono">{r.licenseNumber || "—"}</span> },
          { key: "fee", header: "Consultation Fee", render: (r) => formatMoney(r.consultationFee) },
          { key: "user", header: "Linked User", render: (r) => r.userName || "—" },
          { key: "active", header: "Status", render: (r) => (r.active ? <Badge value="Active" tone="success" /> : <Badge value="Inactive" tone="muted" />) },
          { key: "actions", header: "", render: (r) => canAssign ? <button className="btn btn-sm" onClick={() => openEdit(r)}>Edit</button> : null },
        ]}
      />

      <DoctorModal
        open={open}
        onClose={() => setOpen(false)}
        editing={editing}
        users={users}
        saving={saving}
        onSave={handleSave}
      />
    </div>
  );
}

function DoctorModal({
  open,
  onClose,
  editing,
  users,
  saving,
  onSave,
}: {
  open: boolean;
  onClose: () => void;
  editing: Doctor | null;
  users: User[];
  saving: boolean;
  onSave: (b: Record<string, unknown>) => void;
}) {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [specialty, setSpecialty] = useState("");
  const [licenseNumber, setLicenseNumber] = useState("");
  const [consultationFee, setConsultationFee] = useState("");
  const [userId, setUserId] = useState("");

  useEffect(() => {
    if (open) {
      const parts = (editing?.fullName ?? "").split(" ");
      setFirstName(parts.shift() ?? "");
      setLastName(parts.join(" "));
      setSpecialty(editing?.specialty ?? "");
      setLicenseNumber(editing?.licenseNumber ?? "");
      setConsultationFee(editing?.consultationFee != null ? String(editing.consultationFee) : "");
      setUserId(editing?.userId != null ? String(editing.userId) : "");
    }
  }, [open, editing]);

  function submit() {
    onSave({
      userId: userId ? Number(userId) : null,
      firstName,
      lastName,
      specialty: specialty || null,
      licenseNumber: licenseNumber || null,
      consultationFee: consultationFee ? Number(consultationFee) : null,
    });
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={editing ? "Edit Doctor" : "Add Doctor"}
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Save"}</button>
        </>
      }
    >
      <div className="form-grid">
        <div className="field">
          <label>First Name <span className="req">*</span></label>
          <input className="input" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
        </div>
        <div className="field">
          <label>Last Name <span className="req">*</span></label>
          <input className="input" value={lastName} onChange={(e) => setLastName(e.target.value)} />
        </div>
        <div className="field">
          <label>Specialty</label>
          <input className="input" value={specialty} onChange={(e) => setSpecialty(e.target.value)} placeholder="Cardiology" />
        </div>
        <div className="field">
          <label>License Number</label>
          <input className="input" value={licenseNumber} onChange={(e) => setLicenseNumber(e.target.value)} />
        </div>
        <div className="field">
          <label>Consultation Fee</label>
          <input className="input" type="number" value={consultationFee} onChange={(e) => setConsultationFee(e.target.value)} />
        </div>
        <div className="field">
          <label>Linked User</label>
          <select className="select" value={userId} onChange={(e) => setUserId(e.target.value)}>
            <option value="">—</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>{u.fullName} (@{u.username})</option>
            ))}
          </select>
        </div>
      </div>
    </Modal>
  );
}