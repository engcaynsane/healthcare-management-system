import { useCallback, useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { patientApi } from "../lib/api-endpoints";
import type { Patient } from "../lib/types";
import { formatDate } from "../lib/format";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";
import { can } from "../lib/permissions";

export function PatientsPage() {
  const [rows, setRows] = useState<Patient[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Patient | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, q]);

  async function load() {
    setLoading(true);
    try {
      const p = await patientApi.list({ q, page, size: 20 });
      setRows(p.content);
      setTotalPages(p.totalPages);
      setTotal(p.totalElements);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load patients");
    } finally {
      setLoading(false);
    }
  }

  const reload = useCallback(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, q]);

  function openCreate() {
    setEditing(null);
    setOpen(true);
  }
  function openEdit(p: Patient) {
    setEditing(p);
    setOpen(true);
  }

  async function handleSave(body: PatientForm) {
    setSaving(true);
    try {
      if (editing) {
        await patientApi.update(editing.id, body);
        toast.success("Patient updated");
      } else {
        await patientApi.create(body);
        toast.success("Patient registered");
      }
      setOpen(false);
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to save patient");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(p: Patient) {
    if (!window.confirm(`Delete patient ${p.firstName} ${p.lastName}?`)) return;
    try {
      await patientApi.delete(p.id);
      toast.success("Patient deleted");
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to delete patient");
    }
  }

  return (
    <div>
      <div className="toolbar">
        <div className="search">
          <input className="input" placeholder="Search name, code, phone…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        </div>
        <div className="spacer" style={{ flex: 1 }} />
        <button className="btn btn-primary" onClick={openCreate}>
          <Icon name="plus" /> Register Patient
        </button>
      </div>

      <DataTable<Patient>
        rows={rows}
        loading={loading}
        emptyMessage="No patients found."
        columns={[
          { key: "patientCode", header: "Code", render: (r) => <span className="mono">{r.patientCode}</span> },
          { key: "name", header: "Name", render: (r) => `${r.firstName} ${r.lastName}` },
          { key: "gender", header: "Gender", render: (r) => r.gender || "—" },
          { key: "dob", header: "Date of Birth", render: (r) => formatDate(r.dateOfBirth) },
          { key: "phone", header: "Phone", render: (r) => r.phone || "—" },
          { key: "bloodGroup", header: "Blood", render: (r) => r.bloodGroup || "—" },
          { key: "email", header: "Email", render: (r) => r.email || "—" },
          { key: "actions", header: "", render: (r) => (
            <div style={{ display: "flex", gap: 6, justifyContent: "flex-end" }}>
              <button className="btn btn-sm" onClick={() => openEdit(r)}>Edit</button>
              {can("patient.delete") && (
                <button className="btn btn-sm btn-danger" onClick={() => handleDelete(r)}>Delete</button>
              )}
            </div>
          ) },
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />

      <PatientModal
        open={open}
        onClose={() => setOpen(false)}
        editing={editing}
        saving={saving}
        onSave={handleSave}
      />
    </div>
  );
}

export type PatientForm = {
  firstName: string;
  lastName: string;
  gender: string | null;
  dateOfBirth: string | null;
  phone: string | null;
  email: string | null;
  bloodGroup: string | null;
  nationalId: string | null;
  allergies: string | null;
  medicalHistory: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
};

function PatientModal({
  open,
  onClose,
  editing,
  saving,
  onSave,
}: {
  open: boolean;
  onClose: () => void;
  editing: Patient | null;
  saving: boolean;
  onSave: (b: PatientForm) => void;
}) {
  const [form, setForm] = useState<PatientForm>({
    firstName: "",
    lastName: "",
    gender: null,
    dateOfBirth: null,
    phone: null,
    email: null,
    bloodGroup: null,
    nationalId: null,
    allergies: null,
    medicalHistory: null,
    emergencyContactName: null,
    emergencyContactPhone: null,
  });

  useEffect(() => {
    if (open) {
      if (editing) {
        setForm({
          firstName: editing.firstName,
          lastName: editing.lastName,
          gender: editing.gender,
          dateOfBirth: editing.dateOfBirth,
          phone: editing.phone,
          email: editing.email,
          bloodGroup: editing.bloodGroup,
          nationalId: editing.nationalId,
          allergies: editing.allergies,
          medicalHistory: editing.medicalHistory,
          emergencyContactName: editing.emergencyContactName,
          emergencyContactPhone: editing.emergencyContactPhone,
        });
      } else {
        setForm({ firstName: "", lastName: "", gender: null, dateOfBirth: null, phone: null, email: null, bloodGroup: null, nationalId: null, allergies: null, medicalHistory: null, emergencyContactName: null, emergencyContactPhone: null });
      }
    }
  }, [open, editing]);

  const set = <K extends keyof PatientForm>(key: K, value: PatientForm[K]) =>
    setForm((f) => ({ ...f, [key]: value }));

  return (
    <Modal
      open={open}
      onClose={onClose}
      wide
      title={editing ? `Edit Patient ${editing.firstName} ${editing.lastName}` : "Register Patient"}
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving} onClick={() => onSave(form)}>
            {saving ? "Saving…" : "Save Patient"}
          </button>
        </>
      }
    >
      <div className="form-grid">
        <div className="field">
          <label>First Name <span className="req">*</span></label>
          <input className="input" value={form.firstName} onChange={(e) => set("firstName", e.target.value)} required />
        </div>
        <div className="field">
          <label>Last Name <span className="req">*</span></label>
          <input className="input" value={form.lastName} onChange={(e) => set("lastName", e.target.value)} required />
        </div>
        <div className="field">
          <label>Gender</label>
          <select className="select" value={form.gender || ""} onChange={(e) => set("gender", e.target.value || null)}>
            <option value="">—</option>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
          </select>
        </div>
        <div className="field">
          <label>Date of Birth</label>
          <input className="input" type="date" value={form.dateOfBirth || ""} onChange={(e) => set("dateOfBirth", e.target.value || null)} />
        </div>
        <div className="field">
          <label>Phone</label>
          <input className="input" value={form.phone || ""} onChange={(e) => set("phone", e.target.value || null)} />
        </div>
        <div className="field">
          <label>Email</label>
          <input className="input" value={form.email || ""} onChange={(e) => set("email", e.target.value || null)} />
        </div>
        <div className="field">
          <label>Blood Group</label>
          <select className="select" value={form.bloodGroup || ""} onChange={(e) => set("bloodGroup", e.target.value || null)}>
            <option value="">—</option>
            {["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"].map((b) => (
              <option key={b} value={b}>{b}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>National ID</label>
          <input className="input" value={form.nationalId || ""} onChange={(e) => set("nationalId", e.target.value || null)} />
        </div>
        <div className="field span-2">
          <label>Allergies</label>
          <input className="input" value={form.allergies || ""} onChange={(e) => set("allergies", e.target.value || null)} />
        </div>
        <div className="field span-2">
          <label>Medical History</label>
          <textarea className="textarea" value={form.medicalHistory || ""} onChange={(e) => set("medicalHistory", e.target.value || null)} />
        </div>
        <div className="field">
          <label>Emergency Contact Name</label>
          <input className="input" value={form.emergencyContactName || ""} onChange={(e) => set("emergencyContactName", e.target.value || null)} />
        </div>
        <div className="field">
          <label>Emergency Contact Phone</label>
          <input className="input" value={form.emergencyContactPhone || ""} onChange={(e) => set("emergencyContactPhone", e.target.value || null)} />
        </div>
      </div>
    </Modal>
  );
}