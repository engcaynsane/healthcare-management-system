import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { dashboardApi } from "../../lib/api-endpoints";
import type { DashboardSummary } from "../../lib/types";

export function useSummary() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  useEffect(() => {
    dashboardApi.summary().then(setSummary).catch(() => {});
  }, []);
  return summary;
}

export function StatCards({ items }: { items: { label: string; value: string; sub?: string }[] }) {
  return (
    <div className="stats-grid" style={{ marginBottom: 20 }}>
      {items.map((s) => (
        <div key={s.label} className="stat-card">
          <div className="stat-label">{s.label}</div>
          <div className="stat-value">{s.value}</div>
          {s.sub ? <div className="stat-sub">{s.sub}</div> : null}
        </div>
      ))}
    </div>
  );
}

export function QuickActions({ items }: { items: { to: string; label: string; primary?: boolean }[] }) {
  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <div className="card-header">Quick Actions</div>
      <div className="card-body" style={{ display: "flex", flexWrap: "wrap", gap: 10 }}>
        {items.map((a) => (
          <Link key={a.to} to={a.to} className={`btn ${a.primary ? "btn-primary" : ""}`}>
            {a.label}
          </Link>
        ))}
      </div>
    </div>
  );
}

export function ListCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="card">
      <div className="card-header">{title}</div>
      <div className="card-body" style={{ padding: 0 }}>{children}</div>
    </div>
  );
}

export function Empty() {
  return <div className="empty">Nothing to show</div>;
}
