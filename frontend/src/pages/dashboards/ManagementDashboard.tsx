import { useEffect, useState } from "react";
import { inventoryApi } from "../../lib/api-endpoints";
import type { Transfer, StockRow } from "../../lib/types";
import { formatMoney } from "../../lib/format";
import { StatusBadge } from "../../components/Badge";
import { useSummary, StatCards } from "./shared";

export function ManagementDashboard() {
  const summary = useSummary();
  const [lowStock, setLowStock] = useState<StockRow[]>([]);
  const [transfers, setTransfers] = useState<Transfer[]>([]);

  useEffect(() => {
    inventoryApi.lowStock().then(setLowStock).catch(() => {});
    inventoryApi.transfers({ status: "PENDING", size: 5 }).then((p) => setTransfers(p.content)).catch(() => {});
  }, []);

  const stats = summary
    ? [
        { label: "Sales Today", value: String(summary.salesToday), sub: `${formatMoney(summary.revenueToday)} revenue` },
        { label: "Patients Today", value: String(summary.patientsToday), sub: "new registrations" },
        { label: "Appointments Today", value: String(summary.appointmentsToday), sub: "scheduled" },
        { label: "Pending Lab Orders", value: String(summary.pendingLabs), sub: "in progress" },
        { label: "Low Stock Items", value: String(summary.lowStock), sub: "need reorder" },
        { label: "Expiring Soon", value: String(summary.expiringSoon), sub: "within 30 days" },
        { label: "Pending Transfers", value: String(summary.pendingTransfers), sub: "across branches" },
      ]
    : [];

  return (
    <div>
      <StatCards items={stats} />

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 18 }}>
        <div className="card">
          <div className="card-header">Low Stock Alerts</div>
          <div className="card-body" style={{ padding: 0 }}>
            {lowStock.length === 0 ? (
              <div className="empty">All items are sufficiently stocked</div>
            ) : (
              lowStock.slice(0, 8).map((row) => (
                <div key={row.medicineId} className="menu-item" style={{ borderRadius: 0 }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 500 }}>{row.name}</div>
                    <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
                      {row.barcode} · reorder at {row.reorderLevel}
                    </div>
                  </div>
                  <span className="badge badge-warning">{row.totalQty} left</span>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header">Pending Transfers</div>
          <div className="card-body" style={{ padding: 0 }}>
            {transfers.length === 0 ? (
              <div className="empty">No pending transfers</div>
            ) : (
              transfers.map((t) => (
                <div key={t.id} className="menu-item" style={{ borderRadius: 0 }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 500 }}>
                      {t.medicineName} × {t.quantity}
                    </div>
                    <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
                      {t.transferNumber} · from branch #{t.fromBranchId} to #{t.toBranchId}
                    </div>
                  </div>
                  <StatusBadge value={t.status} />
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}