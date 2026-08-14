import { useEffect, useState } from "react";
import { labApi } from "../../lib/api-endpoints";
import type { LabOrderSummary } from "../../lib/types";
import { formatDateTime } from "../../lib/format";
import { StatusBadge } from "../../components/Badge";
import { useSummary, StatCards, QuickActions, ListCard, Empty } from "./shared";

export function LabDashboard() {
  const summary = useSummary();
  const [orders, setOrders] = useState<LabOrderSummary[]>([]);

  useEffect(() => {
    labApi.orders({ status: "REQUESTED", size: 8 }).then((p) => setOrders(p.content)).catch(() => {});
  }, []);

  const stats = summary
    ? [
        { label: "Pending Lab Orders", value: String(summary.pendingLabs), sub: "awaiting processing" },
      ]
    : [];

  return (
    <div>
      <StatCards items={stats} />

      <QuickActions
        items={[
          { to: "/lab", label: "Lab Orders", primary: true },
        ]}
      />

      <ListCard title="Pending Lab Orders">
        {orders.length === 0 ? (
          <Empty />
        ) : (
          orders.map((o) => (
            <div key={o.id} className="menu-item" style={{ borderRadius: 0 }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 500 }}>{o.orderNumber}</div>
                <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
                  {o.patientName} · {formatDateTime(o.createdAt)}
                </div>
              </div>
              <StatusBadge value={o.status} />
            </div>
          ))
        )}
      </ListCard>
    </div>
  );
}