import { useCallback, useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { StatusBadge } from "../components/Badge";
import { appointmentApi, doctorApi, patientApi, APPOINTMENT_STATUSES } from "../lib/api-endpoints";
import type { AppointmentSummary, Doctor, Patient } from "../lib/types";
import { formatDateTime, fromLocalDateTimeInput } from "../lib/format";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function AppointmentsPage() {
  const [rows, setRows] = useState<AppointmentSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status]);

  async function load() {
    setLoading(true);
    try {
      const p = await appointmentApi.list({ status: status || undefined, page, size: 20 });
      setRows(p.content);
      setTotalPages(p.totalPages);
      setTotal(p.totalElements);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load appointments");
    } finally {
      setLoading(false);
    }
  }

  const reload = useCallback(() => load(), [page, status]);

  async function updateStatus(id: number, newStatus: string) {
    try {
      await appointmentApi.updateStatus(id, newStatus);
      toast.success(`Appointment marked ${newStatus.replaceAll("_", " ")}`);
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to update status");
    }
  }

  return (
    <div>
      <div className="toolbar">
        <select className="select" style={{ width: 180 }} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
          <option value="">All statuses</option>
          {APPOINTMENT_STATUSES.map((s) => (
            <option key={s} value={s}>{s.replaceAll("_", " ")}</option>
          ))}
        </select>
        <div className="spacer" style={{ flex: 1 }} />
        <button className="btn btn-primary" onClick={() => setOpen(true)}>
          <Icon name="plus" /> New Appointment
        </button>
      </div>

      <DataTable<AppointmentSummary>
        rows={rows}
        loading={loading}
        emptyMessage="No appointments found."
        columns={[
          { key: "start", header: "Scheduled", render: (r) => formatDateTime(r.startTime) },
          { key: "patient", header: "Patient", render: (r) => r.patientName },
          { key: "doctor", header: "Doctor", render: (r) => r.doctorName },
          { key: "purpose", header: "Purpose", render: (r) => r.purpose || "—" },
          { key: "status", header: "Status", render: (r) => <StatusBadge value={r.status} /> },
          {
            key: "actions",
            header: "Actions",
            render: (r) => (
              <div className="actions">
                <select className="select" style={{ width: 140 }} value={r.status} onChange={(e) => updateStatus(r.id, e.target.value)}>
                  {APPOINTMENT_STATUSES.map((s) => (
                    <option key={s} value={s}>{s.replaceAll("_", " ")}</option>
                  ))}
                </select>
              </div>
            ),
          },
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />

      <AppointmentModal open={open} onClose={() => setOpen(false)} onSaved={reload} />
    </div>
  );
}

function AppointmentModal({
  open,
  onClose,
  onSaved,
}: {
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [patientId, setPatientId] = useState("");
  const [doctorId, setDoctorId] = useState("");
  const [purpose, setPurpose] = useState("");
  const [notes, setNotes] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      doctorApi.list().then(setDoctors).catch(() => {});
      patientApi.list({ size: 200 }).then((p) => setPatients(p.content)).catch(() => {});
    }
  }, [open]);

  async function submit() {
    if (!patientId || !doctorId || !startTime) {
      toast.error("Patient, doctor and start time are required");
      return;
    }
    setSaving(true);
    try {
      await appointmentApi.create({
        patientId: Number(patientId),
        doctorId: Number(doctorId),
        startTime: fromLocalDateTimeInput(startTime),
        endTime: fromLocalDateTimeInput(endTime) ,
        purpose: purpose || null,
        notes: notes || null,
      });
      toast.success("Appointment scheduled");
      onClose();
      onSaved();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to schedule appointment");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="New Appointment"
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Schedule"}</button>
        </>
      }
    >
      <div className="form-grid">
        <div className="field">
          <label>Patient <span className="req">*</span></label>
          <select className="select" value={patientId} onChange={(e) => setPatientId(e.target.value)}>
            <option value="">Select patient…</option>
            {patients.map((p) => (
              <option key={p.id} value={p.id}>{p.firstName} {p.lastName} ({p.patientCode})</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Doctor <span className="req">*</span></label>
          <select className="select" value={doctorId} onChange={(e) => setDoctorId(e.target.value)}>
            <option value="">Select doctor…</option>
            {doctors.map((d) => (
              <option key={d.id} value={d.id}>{d.fullName} · {d.specialty || "General"}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Start Time <span className="req">*</span></label>
          <input className="input" type="datetime-local" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
        </div>
        <div className="field">
          <label>End Time</label>
          <input className="input" type="datetime-local" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
        </div>
        <div className="field span-2">
          <label>Purpose</label>
          <input className="input" value={purpose} onChange={(e) => setPurpose(e.target.value)} />
        </div>
        <div className="field span-2">
          <label>Notes</label>
          <textarea className="textarea" value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>
      </div>
    </Modal>
  );
}