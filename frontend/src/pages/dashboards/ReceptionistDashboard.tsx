import { useEffect, useState } from "react";
import { appointmentApi, patientApi } from "../../lib/api-endpoints";
import type { AppointmentSummary, Patient } from "../../lib/types";
import { todayDate, formatTime } from "../../lib/format";
import { StatusBadge } from "../../components/Badge";
import { useSummary, StatCards, QuickActions, ListCard, Empty } from "./shared";

export function ReceptionistDashboard() {
  const summary = useSummary();
  const [appointments, setAppointments] = useState<AppointmentSummary[]>([]);
  const [recentPatients, setRecentPatients] = useState<Patient[]>([]);

  useEffect(() => {
    appointmentApi.list({ date: todayDate(), size: 8 }).then((p) => setAppointments(p.content)).catch(() => {});
    patientApi.list({ size: 5 }).then((p) => setRecentPatients(p.content)).catch(() => {});
  }, []);

  const stats = summary
    ? [
        { label: "Appointments Today", value: String(summary.appointmentsToday), sub: "scheduled" },
        { label: "Patients Today", value: String(summary.patientsToday), sub: "new registrations" },
      ]
    : [];

  return (
    <div>
      <StatCards items={stats} />

      <QuickActions
        items={[
          { to: "/patients", label: "Register Patient", primary: true },
          { to: "/appointments", label: "Book Appointment" },
          { to: "/patients", label: "Patients" },
        ]}
      />

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 18 }}>
        <ListCard title="Today's Appointments">
          {appointments.length === 0 ? (
            <Empty />
          ) : (
            appointments.map((a) => (
              <div key={a.id} className="menu-item" style={{ borderRadius: 0 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 500 }}>
                    {a.patientName} · {a.doctorName}
                  </div>
                  <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
                    {formatTime(a.startTime)}
                    {a.purpose ? ` · ${a.purpose}` : ""}
                  </div>
                </div>
                <StatusBadge value={a.status} />
              </div>
            ))
          )}
        </ListCard>

        <ListCard title="Recently Registered Patients">
          {recentPatients.length === 0 ? (
            <Empty />
          ) : (
            recentPatients.map((p) => (
              <div key={p.id} className="menu-item" style={{ borderRadius: 0 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 500 }}>
                    {p.firstName} {p.lastName}
                  </div>
                  <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
                    {p.patientCode}
                    {p.phone ? ` · ${p.phone}` : ""}
                  </div>
                </div>
              </div>
            ))
          )}
        </ListCard>
      </div>
    </div>
  );
}