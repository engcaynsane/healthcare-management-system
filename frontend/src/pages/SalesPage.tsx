import { useCallback, useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { StatusBadge } from "../components/Badge";
import { saleApi, SALE_STATUSES } from "../lib/api-endpoints";
import type { SaleSummary, SaleDetail } from "../lib/types";
import { formatMoney, formatDateTime } from "../lib/format";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function SalesPage() {
  const [rows, setRows] = useState<SaleSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState("");
  const [date, setDate] = useState("");
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<SaleDetail | null>(null);
  const [refundReason, setRefundReason] = useState("");
  const [refunding, setRefunding] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status, date, q]);

  async function load() {
    setLoading(true);
    try {
      const p = await saleApi.list({ date: date || undefined, status: status || undefined, q: q || undefined, page, size: 20 });
      setRows(p.content);
      setTotalPages(p.totalPages);
      setTotal(p.totalElements);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load sales");
    } finally {
      setLoading(false);
    }
  }

  const reload = useCallback(() => load(), [page, status, date, q]);

  async function openDetail(id: number) {
    try {
      const d = await saleApi.detail(id);
      setDetail(d);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load sale");
    }
  }

  async function doRefund() {
    if (!detail) return;
    setRefunding(true);
    try {
      await saleApi.refund(detail.id, refundReason || undefined);
      toast.success("Sale refunded");
      setDetail(null);
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Refund failed");
    } finally {
      setRefunding(false);
    }
  }

  return (
    <div>
      <div className="toolbar">
        <input className="input" type="date" style={{ width: 160 }} value={date} onChange={(e) => { setDate(e.target.value); setPage(0); }} />
        <select className="select" style={{ width: 180 }} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
          <option value="">All statuses</option>
          {SALE_STATUSES.map((s) => (<option key={s} value={s}>{s.replaceAll("_", " ")}</option>))}
        </select>
        <input className="input" style={{ width: 160 }} placeholder="Search…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
      </div>

      <DataTable<SaleSummary>
        rows={rows}
        loading={loading}
        emptyMessage="No sales found."
        columns={[
          { key: "num", header: "Sale #", render: (r) => <span className="mono">{r.saleNumber}</span> },
          { key: "date", header: "Date", render: (r) => formatDateTime(r.createdAt) },
          { key: "customer", header: "Customer", render: (r) => r.customerName || "Walk-in" },
          { key: "patient", header: "Patient", render: (r) => r.patientName || "—" },
          { key: "total", header: "Total", render: (r) => formatMoney(r.total) },
          { key: "paid", header: "Paid", render: (r) => formatMoney(r.paidAmount) },
          { key: "method", header: "Method", render: (r) => (r.paymentMethod ? r.paymentMethod.replaceAll("_", " ") : "—") },
          { key: "status", header: "Status", render: (r) => <StatusBadge value={r.status} /> },
          { key: "actions", header: "", render: (r) => <button className="btn btn-sm" onClick={() => openDetail(r.id)}>View</button> },
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />

      <Modal open={!!detail} onClose={() => setDetail(null)} wide title={detail ? `Sale ${detail.saleNumber}` : ""}>
        {detail && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
              <span><StatusBadge value={detail.status} /></span>
              <span style={{ color: "var(--text-muted)", fontSize: 12 }}>{formatDateTime(detail.createdAt)} · {detail.cashierName}</span>
            </div>
            <div className="table-wrap" style={{ marginBottom: 14 }}>
              <table className="table">
                <thead><tr><th>Item</th><th>Batch</th><th>Qty</th><th>Unit</th><th>Line Total</th></tr></thead>
                <tbody>
                  {detail.items.map((i) => (
                    <tr key={i.id}>
                      <td>{i.medicineName}</td>
                      <td className="mono">{i.batchNo || "—"}</td>
                      <td>{i.quantity}</td>
                      <td>{formatMoney(i.unitPrice)}</td>
                      <td>{formatMoney(i.lineTotal)}{i.refunded ? <span className="badge badge-danger" style={{ marginLeft: 6 }}>refunded</span> : null}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 4, marginBottom: 14, fontSize: 14 }}>
              <Row label="Subtotal" value={formatMoney(detail.subtotal)} />
              <Row label="Discount" value={formatMoney(detail.discount)} />
              <Row label="Tax" value={formatMoney(detail.tax)} />
              <Row label="Total" value={formatMoney(detail.total)} bold />
              <Row label="Paid" value={formatMoney(detail.paidAmount)} />
              <Row label="Change" value={formatMoney(detail.changeAmount)} />
              <Row label="Method" value={detail.paymentMethod?.replaceAll("_", " ") || "—"} />
              <Row label="Note" value={detail.note || "—"} />
            </div>
            {detail.status?.toUpperCase() === "COMPLETED" && (
              <>
                <div className="field">
                  <label>Refund Reason</label>
                  <input className="input" value={refundReason} onChange={(e) => setRefundReason(e.target.value)} />
                </div>
                <button className="btn btn-danger" disabled={refunding} onClick={doRefund}>
                  {refunding ? "Refunding…" : "Refund Sale"}
                </button>
              </>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

function Row({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between" }}>
      <span className="stat-label" style={{ marginBottom: 0 }}>{label}</span>
      <span style={{ fontWeight: bold ? 700 : 400 }}>{value}</span>
    </div>
  );
}