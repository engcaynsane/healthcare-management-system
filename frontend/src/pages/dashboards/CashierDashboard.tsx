import { useEffect, useState } from "react";
import { saleApi, billingApi } from "../../lib/api-endpoints";
import type { SaleSummary, InvoiceSummary } from "../../lib/types";
import { todayDate, formatMoney } from "../../lib/format";
import { StatusBadge } from "../../components/Badge";
import { useSummary, StatCards, QuickActions, ListCard, Empty } from "./shared";

export function CashierDashboard() {
  const summary = useSummary();
  const [sales, setSales] = useState<SaleSummary[]>([]);
  const [unpaid, setUnpaid] = useState<InvoiceSummary[]>([]);

  useEffect(() => {
    saleApi.list({ date: todayDate(), size: 5 }).then((p) => setSales(p.content)).catch(() => {});
    billingApi.list({ status: "UNPAID", size: 5 }).then((p) => setUnpaid(p.content)).catch(() => {});
  }, []);

  const stats = summary
    ? [
        { label: "Sales Today", value: String(summary.salesToday), sub: `${formatMoney(summary.revenueToday)} revenue` },
        { label: "Patients Today", value: String(summary.patientsToday), sub: "new registrations" },
      ]
    : [];

  return (
    <div>
      <StatCards items={stats} />

      <QuickActions
        items={[
          { to: "/pos", label: "New Sale", primary: true },
          { to: "/sales", label: "Sales" },
          { to: "/invoices", label: "Invoices" },
        ]}
      />

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 18 }}>
        <ListCard title="Today's Sales">
          {sales.length === 0 ? (
            <Empty />
          ) : (
            sales.map((s) => (
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

        <ListCard title="Unpaid Invoices">
          {unpaid.length === 0 ? (
            <Empty />
          ) : (
            unpaid.map((i) => (
              <div key={i.id} className="menu-item" style={{ borderRadius: 0 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 500 }}>{i.invoiceNumber}</div>
                  <div style={{ fontSize: 12, color: "var(--text-muted)" }}>
                    {i.patientName || i.customerName || "—"} · {formatMoney(i.total)}
                  </div>
                </div>
                <StatusBadge value={i.status} />
              </div>
            ))
          )}
        </ListCard>
      </div>
    </div>
  );
}