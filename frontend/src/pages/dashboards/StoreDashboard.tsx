import { useEffect, useState } from "react";
import { inventoryApi } from "../../lib/api-endpoints";
import type { StockRow, Transfer } from "../../lib/types";
import { StatusBadge } from "../../components/Badge";
import { useSummary, StatCards, QuickActions, ListCard, Empty } from "./shared";

export function StoreDashboard() {
  const summary = useSummary();
  const [lowStock, setLowStock] = useState<StockRow[]>([]);
  const [transfers, setTransfers] = useState<Transfer[]>([]);

  useEffect(() => {
    inventoryApi.lowStock().then(setLowStock).catch(() => {});
    inventoryApi.transfers({ status: "PENDING", size: 5 }).then((p) => setTransfers(p.content)).catch(() => {});
  }, []);

  const stats = summary
    ? [
        { label: "Low Stock Items", value: String(summary.lowStock), sub: "need reorder" },
        { label: "Expiring Soon", value: String(summary.expiringSoon), sub: "within 30 days" },
        { label: "Pending Transfers", value: String(summary.pendingTransfers), sub: "across branches" },
      ]
    : [];

  return (
    <div>
      <StatCards items={stats} />

      <QuickActions
        items={[
          { to: "/inventory", label: "Inventory", primary: true },
          { to: "/pharmacy", label: "Medicines" },
          { to: "/suppliers", label: "Suppliers" },
        ]}
      />

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 18 }}>
        <ListCard title="Low Stock Alerts">
          {lowStock.length === 0 ? (
            <Empty />
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
        </ListCard>

        <ListCard title="Pending Transfers">
          {transfers.length === 0 ? (
            <Empty />
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
        </ListCard>
      </div>
    </div>
  );
}