import { useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { StatusBadge } from "../components/Badge";
import { billingApi, patientApi, customerApi, PAYMENT_METHODS, PAYMENT_STATUSES } from "../lib/api-endpoints";
import type { InvoiceSummary, InvoiceDetail, Patient, Customer } from "../lib/types";
import { formatMoney, formatDateTime } from "../lib/format";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function InvoicesPage() {
  const [rows, setRows] = useState<InvoiceSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<InvoiceDetail | null>(null);
  const [payOpen, setPayOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status]);

  async function load() {
    setLoading(true);
    try {
      const p = await billingApi.list({ status: status || undefined, page, size: 20 });
      setRows(p.content);
      setTotalPages(p.totalPages);
      setTotal(p.totalElements);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load invoices");
    } finally {
      setLoading(false);
    }
  }

  function reload() {
    load();
  }

  async function openDetail(id: number) {
    try {
      setDetail(await billingApi.get(id));
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load invoice");
    }
  }

  return (
    <div>
      <div className="toolbar">
        <select className="select" style={{ width: 180 }} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
          <option value="">All statuses</option>
          {PAYMENT_STATUSES.map((s) => (<option key={s} value={s}>{s}</option>))}
        </select>
        <div className="spacer" style={{ flex: 1 }} />
        <button className="btn btn-primary" onClick={() => setCreateOpen(true)}><Icon name="plus" /> New Invoice</button>
      </div>

      <DataTable<InvoiceSummary>
        rows={rows}
        loading={loading}
        emptyMessage="No invoices found."
        columns={[
          { key: "num", header: "Invoice #", render: (r) => <span className="mono">{r.invoiceNumber}</span> },
          { key: "date", header: "Date", render: (r) => formatDateTime(r.createdAt) },
          { key: "patient", header: "Patient", render: (r) => r.patientName || "—" },
          { key: "customer", header: "Customer", render: (r) => r.customerName || "—" },
          { key: "desc", header: "Description", render: (r) => r.description || "—" },
          { key: "total", header: "Total", render: (r) => formatMoney(r.total) },
          { key: "paid", header: "Paid", render: (r) => formatMoney(r.paidAmount) },
          { key: "status", header: "Status", render: (r) => <StatusBadge value={r.status} /> },
          { key: "actions", header: "", render: (r) => <button className="btn btn-sm" onClick={() => openDetail(r.id)}>View</button> },
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />

      <Modal open={!!detail} onClose={() => setDetail(null)} title={detail ? `Invoice ${detail.invoiceNumber}` : ""}>
        {detail && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
              <span style={{ fontWeight: 600 }}>{detail.patientName || detail.customerName || "—"}</span>
              <span><StatusBadge value={detail.status} /></span>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 4, marginBottom: 14 }} className="invoice-detail">
              <Row label="Subtotal" value={formatMoney(detail.subtotal)} />
              <Row label="Discount" value={formatMoney(detail.discount)} />
              <Row label="Tax" value={formatMoney(detail.tax)} />
              <Row label="Total" value={formatMoney(detail.total)} bold />
              <Row label="Paid" value={formatMoney(detail.paidAmount)} />
              <Row label="Balance" value={formatMoney(detail.total - detail.paidAmount)} />
              <Row label="Issued By" value={detail.issuedByName} />
              <Row label="Issued" value={formatDateTime(detail.createdAt)} />
            </div>
            <div className="table-wrap" style={{ marginBottom: 14 }}>
              <table className="table">
                <thead><tr><th>Date</th><th>Method</th><th>Reference</th><th>Amount</th></tr></thead>
                <tbody>
                  {detail.payments.length === 0 ? (
                    <tr><td colSpan={4} className="empty">No payments recorded</td></tr>
                  ) : detail.payments.map((p) => (
                    <tr key={p.id}>
                      <td>{formatDateTime(p.paidAt)}</td>
                      <td>{p.method?.replaceAll("_", " ") || "—"}</td>
                      <td>{p.reference || "—"}</td>
                      <td>{formatMoney(p.amount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {detail.status !== "PAID" && (
              <button className="btn btn-primary" onClick={() => setPayOpen(true)}>Record Payment</button>
            )}
          </div>
        )}
      </Modal>

      <PaymentModal
        open={payOpen}
        onClose={() => setPayOpen(false)}
        invoiceDetail={detail}
        onDone={() => { if (detail) openDetail(detail.id); reload(); }}
      />
      <CreateInvoiceModal open={createOpen} onClose={() => setCreateOpen(false)} onDone={reload} />
    </div>
  );
}

function PaymentModal({
  open,
  onClose,
  invoiceDetail,
  onDone,
}: {
  open: boolean;
  onClose: () => void;
  invoiceDetail: InvoiceDetail | null;
  onDone: () => void;
}) {
  const [amount, setAmount] = useState("");
  const [method, setMethod] = useState("CASH");
  const [reference, setReference] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) setAmount("");
  }, [open]);

  async function submit() {
    if (!invoiceDetail || !amount) return;
    const val = Number(amount);
    const balance = invoiceDetail.total - invoiceDetail.paidAmount;
    if (val <= 0 || val > balance) {
      toast.error(`Amount must be between 0 and ${formatMoney(balance)}`);
      return;
    }
    setSaving(true);
    try {
      await billingApi.pay(invoiceDetail.id, { amount: val, method, reference: reference || undefined });
      toast.success("Payment recorded");
      onClose();
      onDone();
    } catch (e) {
      toast.error(errMsg(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Record Payment" footer={
      <>
        <button className="btn" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Record"}</button>
      </>
    }>
      {invoiceDetail && (
        <div className="form-grid">
          <div className="field span-2">
            <label>Outstanding Balance</label>
            <div style={{ fontWeight: 700, fontSize: 18 }}>{formatMoney(invoiceDetail.total - invoiceDetail.paidAmount)}</div>
          </div>
          <div className="field">
            <label>Amount</label>
            <input className="input" type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} autoFocus />
          </div>
          <div className="field">
            <label>Method</label>
            <select className="select" value={method} onChange={(e) => setMethod(e.target.value)}>
              {PAYMENT_METHODS.map((m) => (<option key={m} value={m}>{m.replaceAll("_", " ")}</option>))}
            </select>
          </div>
          <div className="field span-2">
            <label>Reference</label>
            <input className="input" value={reference} onChange={(e) => setReference(e.target.value)} />
          </div>
        </div>
      )}
    </Modal>
  );
}

function CreateInvoiceModal({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: () => void }) {
  const [patients, setPatients] = useState<Patient[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [patientId, setPatientId] = useState("");
  const [customerId, setCustomerId] = useState("");
  const [description, setDescription] = useState("");
  const [subtotal, setSubtotal] = useState("");
  const [discount, setDiscount] = useState("");
  const [tax, setTax] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      patientApi.list({ size: 200 }).then((p) => setPatients(p.content)).catch(() => {});
      customerApi.list({ size: 200 }).then((p) => setCustomers(p.content)).catch(() => {});
    }
  }, [open]);

  async function submit() {
    setSaving(true);
    try {
      await billingApi.create({
        patientId: patientId ? Number(patientId) : null,
        customerId: customerId ? Number(customerId) : null,
        description: description || null,
        subtotal: subtotal ? Number(subtotal) : null,
        discount: discount ? Number(discount) : null,
        tax: tax ? Number(tax) : null,
      });
      toast.success("Invoice created");
      onClose();
      onDone();
    } catch (e) {
      toast.error(errMsg(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New Invoice" footer={
      <>
        <button className="btn" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Create"}</button>
      </>
    }>
      <div className="form-grid">
        <div className="field">
          <label>Patient</label>
          <select className="select" value={patientId} onChange={(e) => setPatientId(e.target.value)}>
            <option value="">—</option>
            {patients.map((p) => (<option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>))}
          </select>
        </div>
        <div className="field">
          <label>Customer</label>
          <select className="select" value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
            <option value="">—</option>
            {customers.map((c) => (<option key={c.id} value={c.id}>{c.name}</option>))}
          </select>
        </div>
        <div className="field span-2">
          <label>Description</label>
          <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="field">
          <label>Subtotal</label>
          <input className="input" type="number" step="0.01" value={subtotal} onChange={(e) => setSubtotal(e.target.value)} />
        </div>
        <div className="field">
          <label>Discount</label>
          <input className="input" type="number" step="0.01" value={discount} onChange={(e) => setDiscount(e.target.value)} />
        </div>
        <div className="field span-2">
          <label>Tax</label>
          <input className="input" type="number" step="0.01" value={tax} onChange={(e) => setTax(e.target.value)} />
        </div>
      </div>
    </Modal>
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

function errMsg(e: unknown): string {
  return e instanceof ApiHttpError ? e.message : "Operation failed";
}