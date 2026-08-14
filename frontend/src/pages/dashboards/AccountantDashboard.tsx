import { useEffect, useState } from "react";
import { billingApi } from "../../lib/api-endpoints";
import type { InvoiceSummary } from "../../lib/types";
import { formatMoney } from "../../lib/format";
import { StatusBadge } from "../../components/Badge";
import { useSummary, StatCards, QuickActions, ListCard, Empty } from "./shared";

export function AccountantDashboard() {
  const summary = useSummary();
  const [invoices, setInvoices] = useState<InvoiceSummary[]>([]);

  useEffect(() => {
    billingApi.list({ size: 8 }).then((p) => setInvoices(p.content)).catch(() => {});
  }, []);

  const stats = summary
    ? [
        { label: "Sales Today", value: String(summary.salesToday), sub: `${formatMoney(summary.revenueToday)} revenue` },
      ]
    : [];

  return (
    <div>
      <StatCards items={stats} />

      <QuickActions
        items={[
          { to: "/invoices", label: "Invoices", primary: true },
          { to: "/audit", label: "Audit Logs" },
        ]}
      />

      <ListCard title="Recent Invoices">
        {invoices.length === 0 ? (
          <Empty />
        ) : (
          invoices.map((i) => (
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
  );
}