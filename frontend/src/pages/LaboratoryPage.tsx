import { useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { StatusBadge } from "../components/Badge";
import { labApi, patientApi, LAB_STATUSES } from "../lib/api-endpoints";
import type { LabOrderSummary, LabOrderDetail, LabTest, Patient } from "../lib/types";
import { formatMoney, formatDateTime } from "../lib/format";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function LaboratoryPage() {
  const [tab, setTab] = useState<"orders" | "tests">("orders");
  const [orders, setOrders] = useState<LabOrderSummary[]>([]);
  const [tests, setTests] = useState<LabTest[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<LabOrderDetail | null>(null);
  const [resultLine, setResultLine] = useState<{ itemId: number; result: string; notes: string } | null>(null);
  const [orderOpen, setOrderOpen] = useState(false);
  const [testOpen, setTestOpen] = useState(false);

  function loadOrders() {
    setLoading(true);
    labApi.orders({ status: status || undefined, page, size: 20 })
      .then((p) => { setOrders(p.content); setTotalPages(p.totalPages); setTotal(p.totalElements); })
      .catch((e) => toast.error(errMsg(e)))
      .finally(() => setLoading(false));
  }

  function loadTests() {
    labApi.tests().then(setTests).catch(() => {});
  }

  useEffect(() => {
    if (tab === "orders") loadOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, page, status]);

  async function openDetail(id: number) {
    try {
      setDetail(await labApi.orderDetail(id));
    } catch (e) {
      toast.error(errMsg(e));
    }
  }

  async function saveResult() {
    if (!detail || !resultLine) return;
    try {
      await labApi.enterResult(detail.id, resultLine);
      toast.success("Result saved");
      setResultLine(null);
      setDetail(await labApi.orderDetail(detail.id));
    } catch (e) {
      toast.error(errMsg(e));
    }
  }

  async function complete(orderId: number) {
    try {
      await labApi.complete(orderId);
      toast.success("Order completed");
      setDetail(null);
      loadOrders();
    } catch (e) {
      toast.error(errMsg(e));
    }
  }

  return (
    <div>
      <div className="toolbar">
        <div style={{ display: "flex", gap: 6 }}>
          <button className={`btn ${tab === "orders" ? "btn-primary" : ""}`} onClick={() => setTab("orders")}>Orders</button>
          <button className={`btn ${tab === "tests" ? "btn-primary" : ""}`} onClick={() => { setTab("tests"); loadTests(); }}>Test Catalog</button>
        </div>
        <div className="spacer" style={{ flex: 1 }} />
        {tab === "orders" && (
          <>
            <select className="select" style={{ width: 180 }} value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
              <option value="">All statuses</option>
              {LAB_STATUSES.map((s) => (<option key={s} value={s}>{s.replaceAll("_", " ")}</option>))}
            </select>
            <button className="btn btn-primary" onClick={() => setOrderOpen(true)}><Icon name="plus" /> New Order</button>
          </>
        )}
        {tab === "tests" && <button className="btn btn-primary" onClick={() => setTestOpen(true)}><Icon name="plus" /> Add Test</button>}
      </div>

      {tab === "orders" && (
        <>
          <DataTable<LabOrderSummary>
            rows={orders}
            loading={loading}
            emptyMessage="No lab orders."
            columns={[
              { key: "num", header: "Order #", render: (r) => <span className="mono">{r.orderNumber}</span> },
              { key: "patient", header: "Patient", render: (r) => r.patientName },
              { key: "tests", header: "Tests", render: (r) => r.itemCount },
              { key: "status", header: "Status", render: (r) => <StatusBadge value={r.status} /> },
              { key: "date", header: "Created", render: (r) => formatDateTime(r.createdAt) },
              { key: "by", header: "Requested By", render: (r) => r.requestedByName },
              { key: "actions", header: "", render: (r) => <button className="btn btn-sm" onClick={() => openDetail(r.id)}>View</button> },
            ]}
          />
          <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />
        </>
      )}

      {tab === "tests" && (
        <DataTable<LabTest>
          rows={tests}
          emptyMessage="No tests in catalog."
          columns={[
            { key: "code", header: "Code", render: (r) => <span className="mono">{r.code}</span> },
            { key: "name", header: "Name", render: (r) => r.name },
            { key: "category", header: "Category", render: (r) => r.category || "—" },
            { key: "price", header: "Price", render: (r) => formatMoney(r.price) },
            { key: "desc", header: "Description", render: (r) => r.description || "—" },
          ]}
        />
      )}

      <Modal open={!!detail} onClose={() => setDetail(null)} wide title={detail ? `Order ${detail.orderNumber}` : ""}>
        {detail && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
              <span style={{ fontWeight: 600 }}>{detail.patientName}</span>
              <span><StatusBadge value={detail.status} /></span>
            </div>
            <div className="table-wrap" style={{ marginBottom: 14 }}>
              <table className="table">
                <thead><tr><th>Test</th><th>Price</th><th>Result</th><th>Status</th><th></th></tr></thead>
                <tbody>
                  {detail.items.map((i) => (
                    <tr key={i.id}>
                      <td>{i.name} <span className="mono">({i.code})</span></td>
                      <td>{formatMoney(i.price)}</td>
                      <td>{i.result || "—"}</td>
                      <td><StatusBadge value={i.status} /></td>
                      <td>
                        {detail.status !== "COMPLETED" && detail.status !== "CANCELLED" && (
                          <button className="btn btn-sm" onClick={() => { setResultLine({ itemId: i.id, result: i.result || "", notes: i.resultNotes || "" }); }}>Enter Result</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {detail.status === "IN_PROGRESS" && detail.items.every((i) => i.status === "COMPLETED") && (
              <button className="btn btn-success" onClick={() => complete(detail.id)}>Complete Order</button>
            )}
          </div>
        )}
      </Modal>

      <Modal open={!!resultLine} onClose={() => setResultLine(null)} title="Enter Result">
        {resultLine && (
          <div>
            <div className="field">
              <label>Result</label>
              <textarea className="textarea" value={resultLine.result} onChange={(e) => setResultLine({ ...resultLine, result: e.target.value })} />
            </div>
            <div className="field">
              <label>Notes</label>
              <textarea className="textarea" value={resultLine.notes} onChange={(e) => setResultLine({ ...resultLine, notes: e.target.value })} />
            </div>
            <button className="btn btn-primary" onClick={saveResult}>Save Result</button>
          </div>
        )}
      </Modal>

      <OrderModal open={orderOpen} onClose={() => setOrderOpen(false)} onDone={() => loadOrders()} />
      <TestModal open={testOpen} onClose={() => setTestOpen(false)} onDone={loadTests} />
    </div>
  );
}

function OrderModal({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: () => void }) {
  const [patients, setPatients] = useState<Patient[]>([]);
  const [tests, setTests] = useState<LabTest[]>([]);
  const [patientId, setPatientId] = useState("");
  const [testIds, setTestIds] = useState<number[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      patientApi.list({ size: 200 }).then((p) => setPatients(p.content)).catch(() => {});
      labApi.tests().then(setTests).catch(() => {});
      setPatientId("");
      setTestIds([]);
    }
  }, [open]);

  function toggleTest(id: number) {
    setTestIds((t) => (t.includes(id) ? t.filter((x) => x !== id) : [...t, id]));
  }

  async function submit() {
    if (!patientId || testIds.length === 0) {
      toast.error("Select a patient and at least one test");
      return;
    }
    setSaving(true);
    try {
      await labApi.createOrder({ patientId: Number(patientId), testIds });
      toast.success("Lab order created");
      onClose();
      onDone();
    } catch (e) {
      toast.error(errMsg(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New Lab Order" footer={
      <>
        <button className="btn" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Create"}</button>
      </>
    }>
      <div className="field">
        <label>Patient</label>
        <select className="select" value={patientId} onChange={(e) => setPatientId(e.target.value)}>
          <option value="">Select patient…</option>
          {patients.map((p) => (<option key={p.id} value={p.id}>{p.firstName} {p.lastName} ({p.patientCode})</option>))}
        </select>
      </div>
      <div className="field">
        <label>Tests</label>
        <div style={{ maxHeight: 220, overflowY: "auto", border: "1px solid var(--border)", borderRadius: 6 }}>
          {tests.length === 0 ? <div className="empty">No tests in catalog</div> : tests.map((t) => (
            <label key={t.id} className="menu-item" style={{ display: "flex", gap: 8 }}>
              <input type="checkbox" checked={testIds.includes(t.id)} onChange={() => toggleTest(t.id)} />
              <span>{t.name} ({t.code})</span>
              <span style={{ marginLeft: "auto", color: "var(--text-muted)" }}>{formatMoney(t.price)}</span>
            </label>
          ))}
        </div>
      </div>
    </Modal>
  );
}

function TestModal({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: () => void }) {
  const [form, setForm] = useState({ code: "", name: "", category: "", price: "", description: "" });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) setForm({ code: "", name: "", category: "", price: "", description: "" });
  }, [open]);

  async function submit() {
    if (!form.code || !form.name) {
      toast.error("Code and name are required");
      return;
    }
    setSaving(true);
    try {
      await labApi.createTest({
        code: form.code,
        name: form.name,
        category: form.category || undefined,
        price: form.price ? Number(form.price) : undefined,
        description: form.description || undefined,
      });
      toast.success("Test added");
      onClose();
      onDone();
    } catch (e) {
      toast.error(errMsg(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Add Lab Test" footer={
      <>
        <button className="btn" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Add"}</button>
      </>
    }>
      <div className="form-grid">
        <div className="field">
          <label>Code <span className="req">*</span></label>
          <input className="input" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="CBC" />
        </div>
        <div className="field">
          <label>Name <span className="req">*</span></label>
          <input className="input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </div>
        <div className="field">
          <label>Category</label>
          <input className="input" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
        </div>
        <div className="field">
          <label>Price</label>
          <input className="input" type="number" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
        </div>
        <div className="field span-2">
          <label>Description</label>
          <textarea className="textarea" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </div>
      </div>
    </Modal>
  );
}

function errMsg(e: unknown): string {
  return e instanceof ApiHttpError ? e.message : "Operation failed";
}