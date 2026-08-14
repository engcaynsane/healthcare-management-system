import { useEffect, useState } from "react";
import { inventoryApi, saleApi } from "../../lib/api-endpoints";
import type { StockRow, SaleSummary } from "../../lib/types";
import { todayDate, formatMoney } from "../../lib/format";
import { StatusBadge } from "../../components/Badge";
import { useSummary, StatCards, QuickActions, ListCard, Empty } from "./shared";

export function PharmacistDashboard() {
  const summary = useSummary();
  const [lowStock, setLowStock] = useState<StockRow[]>([]);
  const [todaySales, setTodaySales] = useState<SaleSummary[]>([]);

  useEffect(() => {
    inventoryApi.lowStock().then(setLowStock).catch(() => {});
    saleApi.list({ date: todayDate(), size: 5 }).then((p) => setTodaySales(p.content)).catch(() => {});
  }, []);

  const stats = summary
    ? [
        { label: "Sales Today", value: String(summary.salesToday), sub: `${formatMoney(summary.revenueToday)} revenue` },
        { label: "Low Stock Items", value: String(summary.lowStock), sub: "need reorder" },
        { label: "Expiring Soon", value: String(summary.expiringSoon), sub: "within 30 days" },
      ]
    : [];

  return (
    <div>
      <StatCards items={stats} />

      <QuickActions
        items={[
          { to: "/pos", label: "New Sale", primary: true },
          { to: "/inventory", label: "Inventory" },
          { to: "/pharmacy", label: "Medicines" },
          { to: "/sales", label: "Sales" },
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

        <ListCard title="Today's Sales">
          {todaySales.length === 0 ? (
            <Empty />
          ) : (
            todaySales.map((s) => (
              <div key={s.id} className="menu-item" style={{ borderRadius: 0 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 500 }}>{s.saleNumber}</div>
                  <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
                    {s.customerName || s.patientName || "Walk-in"} · {formatMoney(s.total)}
                  </div>
                </div>
                <StatusBadge value={s.status} />
              </div>
            ))
          )}
        </ListCard>
      </div>
    </div>
  );
}