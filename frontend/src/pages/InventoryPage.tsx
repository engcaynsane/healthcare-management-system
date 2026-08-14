import { useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { StatusBadge } from "../components/Badge";
import { inventoryApi, medicineApi } from "../lib/api-endpoints";
import type { StockRow, Movement, Medicine, Supplier, Transfer } from "../lib/types";
import { formatMoney, formatDateTime } from "../lib/format";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";
import { supplierApi, branchApi } from "../lib/api-endpoints";
import type { Branch } from "../lib/types";

type Tab = "stock" | "movements" | "transfers";

export function InventoryPage() {
  const [tab, setTab] = useState<Tab>("stock");
  const [stock, setStock] = useState<StockRow[]>([]);
  const [movements, setMovements] = useState<Movement[]>([]);
  const [transfers, setTransfers] = useState<Transfer[]>([]);
  const [movPage, setMovPage] = useState(0);
  const [movTotal, setMovTotal] = useState(0);
  const [movPages, setMovPages] = useState(0);
  const [trPage, setTrPage] = useState(0);
  const [trTotal, setTrTotal] = useState(0);
  const [trPages, setTrPages] = useState(0);
  const [trStatus, setTrStatus] = useState("");
  const [loadingStock, setLoadingStock] = useState(false);
  const [receiveOpen, setReceiveOpen] = useState(false);
  const [adjustOpen, setAdjustOpen] = useState(false);
  const [transferOpen, setTransferOpen] = useState(false);

  function loadStock() {
    setLoadingStock(true);
    inventoryApi.stock().then(setStock).catch((e) => toast.error(errMsg(e))).finally(() => setLoadingStock(false));
  }

  function loadMovements() {
    inventoryApi.movements(movPage, 20).then((p) => { setMovements(p.content); setMovTotal(p.totalElements); setMovPages(p.totalPages); }).catch(() => {});
  }

  function loadTransfers() {
    inventoryApi.transfers({ status: trStatus || undefined, page: trPage, size: 20 }).then((p) => { setTransfers(p.content); setTrTotal(p.totalElements); setTrPages(p.totalPages); }).catch(() => {});
  }

  useEffect(() => {
    loadStock();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (tab === "movements") loadMovements();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, movPage]);

  useEffect(() => {
    if (tab === "transfers") loadTransfers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, trPage, trStatus]);

  async function handleTransferAction(id: number, action: "approve" | "reject" | "ship" | "receive", reason?: string) {
    try {
      await inventoryApi[`${action}Transfer`](id, reason);
      toast.success(action === "approve" ? "Transfer approved" : action === "reject" ? "Transfer rejected" : action === "ship" ? "Transfer shipped" : "Transfer received");
      loadTransfers();
    } catch (e) {
      toast.error(errMsg(e));
    }
  }

  return (
    <div>
      <div className="toolbar">
        <div style={{ display: "flex", gap: 6 }}>
          <button className={`btn ${tab === "stock" ? "btn-primary" : ""}`} onClick={() => setTab("stock")}>Stock</button>
          <button className={`btn ${tab === "movements" ? "btn-primary" : ""}`} onClick={() => setTab("movements")}>Movements</button>
          <button className={`btn ${tab === "transfers" ? "btn-primary" : ""}`} onClick={() => setTab("transfers")}>Transfers</button>
        </div>
        <div className="spacer" style={{ flex: 1 }} />
        {tab === "stock" && (
          <>
            <button className="btn" onClick={() => setAdjustOpen(true)}><Icon name="refresh" /> Adjust</button>
            <button className="btn btn-primary" onClick={() => setReceiveOpen(true)}><Icon name="plus" /> Receive Stock</button>
          </>
        )}
        {tab === "transfers" && (
          <button className="btn btn-primary" onClick={() => setTransferOpen(true)}><Icon name="plus" /> New Transfer</button>
        )}
      </div>

      {tab === "stock" && (
        <StockTable rows={stock} loading={loadingStock} />
      )}

      {tab === "movements" && (
        <>
          <DataTable<Movement>
            rows={movements}
            emptyMessage="No movements."
            columns={[
              { key: "date", header: "Date", render: (r) => formatDateTime(r.createdAt) },
              { key: "type", header: "Type", render: (r) => <StatusBadge value={r.type} /> },
              { key: "medicine", header: "Medicine", render: (r) => r.medicineName || "—" },
              { key: "batch", header: "Batch", render: (r) => r.batchNo || "—" },
              { key: "change", header: "Change", render: (r) => <span className={r.quantityChange < 0 ? "badge badge-danger" : "badge badge-success"}>{r.quantityChange > 0 ? "+" : ""}{r.quantityChange}</span> },
              { key: "after", header: "After", render: (r) => r.afterQty },
              { key: "reference", header: "Reference", render: (r) => r.reference || "—" },
            ]}
          />
          <Pagination page={movPage} totalPages={movPages} totalElements={movTotal} onPage={setMovPage} />
        </>
      )}

      {tab === "transfers" && (
        <>
          <div className="toolbar">
            <select className="select" style={{ width: 180 }} value={trStatus} onChange={(e) => { setTrStatus(e.target.value); setTrPage(0); }}>
              <option value="">All statuses</option>
              {["PENDING", "APPROVED", "REJECTED", "IN_TRANSIT", "RECEIVED"].map((s) => (
                <option key={s} value={s}>{s.replaceAll("_", " ")}</option>
              ))}
            </select>
          </div>
          <DataTable<Transfer>
            rows={transfers}
            emptyMessage="No transfers."
            columns={[
              { key: "num", header: "Number", render: (r) => <span className="mono">{r.transferNumber}</span> },
              { key: "medicine", header: "Medicine", render: (r) => r.medicineName },
              { key: "qty", header: "Qty", render: (r) => r.quantity },
              { key: "from", header: "From", render: (r) => `#${r.fromBranchId}` },
              { key: "to", header: "To", render: (r) => `#${r.toBranchId}` },
              { key: "status", header: "Status", render: (r) => <StatusBadge value={r.status} /> },
              { key: "date", header: "Created", render: (r) => formatDateTime(r.createdAt) },
              { key: "actions", header: "Actions", render: (r) => <TransferActions status={r.status} id={r.id} onAction={handleTransferAction} /> },
            ]}
          />
          <Pagination page={trPage} totalPages={trPages} totalElements={trTotal} onPage={setTrPage} />
        </>
      )}

      <ReceiveModal open={receiveOpen} onClose={() => setReceiveOpen(false)} onDone={() => { loadStock(); loadTransfers(); }} />
      <AdjustModal open={adjustOpen} onClose={() => setAdjustOpen(false)} onDone={loadStock} stock={stock} />
      <TransferModal open={transferOpen} onClose={() => setTransferOpen(false)} onDone={loadTransfers} stock={stock} />
    </div>
  );
}

function StockTable({ rows, loading }: { rows: StockRow[]; loading: boolean }) {
  return (
    <DataTable<StockRow>
      rows={rows}
      loading={loading}
      emptyMessage="No stock records yet. Receive stock to get started."
      rowKey={(r) => r.medicineId}
      columns={[
        { key: "name", header: "Medicine", render: (r) => (
          <div>
            <div style={{ fontWeight: 500 }}>{r.name}</div>
            <div style={{ fontSize: 11, color: "var(--text-muted)" }}>{r.barcode}</div>
          </div>
        )},
        { key: "qty", header: "On Hand", render: (r) => (
          <span className={`badge ${r.lowStock ? "badge-danger" : r.totalQty > 0 ? "badge-success" : "badge-muted"}`}>{r.totalQty} {r.unit || "units"}</span>
        )},
        { key: "batches", header: "Batches", render: (r) => r.batchList.length },
        { key: "expiring", header: "Expiring", render: (r) => (r.expiringSoon ? <span className="badge badge-warning">Soon</span> : "—") },
        { key: "reorder", header: "Reorder Lvl", render: (r) => r.reorderLevel }, 
        { key: "price", header: "Selling", render: (r) => formatMoney(r.sellingPrice) },
        { key: "cost", header: "Cost", render: (r) => formatMoney(r.costPrice) },
        { key: "details", header: "Batches Detail", render: (r) => (
          <span title={r.batchList.map((b) => `${b.batchNo}: ${b.quantity} (${b.expiryDate || "no exp"})`).join(", ")} style={{ cursor: "help", color: "var(--text-muted)" }}>view</span>
        )},
      ]}
    />
  );
}

function TransferActions({
  status,
  id,
  onAction,
}: {
  status: string;
  id: number;
  onAction: (id: number, action: "approve" | "reject" | "ship" | "receive", reason?: string) => void;
}) {
  const [reason, setReason] = useState("");
  const [rejOpen, setRejOpen] = useState(false);
  return (
    <div className="actions">
      {status === "PENDING" && (
        <>
          <button className="btn btn-sm btn-success" onClick={() => onAction(id, "approve")}>Approve</button>
          <button className="btn btn-sm btn-danger" onClick={() => setRejOpen(true)}>Reject</button>
        </>
      )}
      {status === "APPROVED" && <button className="btn btn-sm btn-primary" onClick={() => onAction(id, "ship")}>Ship</button>}
      {status === "IN_TRANSIT" && <button className="btn btn-sm btn-success" onClick={() => onAction(id, "receive")}>Receive</button>}
      <Modal open={rejOpen} onClose={() => setRejOpen(false)} title="Reject Transfer" footer={
        <>
          <button className="btn" onClick={() => setRejOpen(false)}>Cancel</button>
          <button className="btn btn-danger" onClick={() => { onAction(id, "reject", reason); setRejOpen(false); }}>Reject</button>
        </>
      }>
        <div className="field">
          <label>Reason</label>
          <textarea className="textarea" value={reason} onChange={(e) => setReason(e.target.value)} />
        </div>
      </Modal>
    </div>
  );
}

function ReceiveModal({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: () => void }) {
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [medicineId, setMedicineId] = useState("");
  const [batchNo, setBatchNo] = useState("");
  const [expiryDate, setExpiryDate] = useState("");
  const [costPrice, setCostPrice] = useState("");
  const [quantity, setQuantity] = useState("");
  const [supplierId, setSupplierId] = useState("");
  const [location, setLocation] = useState("");
  const [reference, setReference] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      medicineApi.list({ size: 200 }).then((p) => setMedicines(p.content)).catch(() => {});
      supplierApi.list({ size: 200 }).then((p) => setSuppliers(p.content)).catch(() => {});
    }
  }, [open]);

  async function submit() {
    if (!medicineId || !batchNo || !quantity) {
      toast.error("Medicine, batch and quantity are required");
      return;
    }
    setSaving(true);
    try {
      await inventoryApi.receive({
        medicineId: Number(medicineId),
        batchNo,
        expiryDate: expiryDate || null,
        costPrice: costPrice ? Number(costPrice) : null,
        quantity: Number(quantity),
        supplierId: supplierId ? Number(supplierId) : null,
        location: location || null,
        reference: reference || null,
      });
      toast.success("Stock received");
      onClose();
      onDone();
    } catch (e) {
      toast.error(errMsg(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Receive Stock" footer={
      <>
        <button className="btn" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Receive"}</button>
      </>
    }>
      <div className="form-grid">
        <div className="field span-2">
          <label>Medicine <span className="req">*</span></label>
          <select className="select" value={medicineId} onChange={(e) => setMedicineId(e.target.value)}>
            <option value="">Select medicine…</option>
            {medicines.map((m) => (<option key={m.id} value={m.id}>{m.name} ({m.barcode})</option>))}
          </select>
        </div>
        <div className="field">
          <label>Batch No <span className="req">*</span></label>
          <input className="input" value={batchNo} onChange={(e) => setBatchNo(e.target.value)} />
        </div>
        <div className="field">
          <label>Quantity <span className="req">*</span></label>
          <input className="input" type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
        </div>
        <div className="field">
          <label>Expiry Date</label>
          <input className="input" type="date" value={expiryDate} onChange={(e) => setExpiryDate(e.target.value)} />
        </div>
        <div className="field">
          <label>Cost Price</label>
          <input className="input" type="number" step="0.01" value={costPrice} onChange={(e) => setCostPrice(e.target.value)} />
        </div>
        <div className="field">
          <label>Supplier</label>
          <select className="select" value={supplierId} onChange={(e) => setSupplierId(e.target.value)}>
            <option value="">—</option>
            {suppliers.map((s) => (<option key={s.id} value={s.id}>{s.name}</option>))}
          </select>
        </div>
        <div className="field">
          <label>Location</label>
          <input className="input" value={location} onChange={(e) => setLocation(e.target.value)} />
        </div>
        <div className="field">
          <label>Reference</label>
          <input className="input" value={reference} onChange={(e) => setReference(e.target.value)} />
        </div>
      </div>
    </Modal>
  );
}

function AdjustModal({ open, onClose, onDone, stock }: { open: boolean; onClose: () => void; onDone: () => void; stock: StockRow[] }) {
  const [medicineId, setMedicineId] = useState("");
  const [batchId, setBatchId] = useState("");
  const [quantityChange, setQuantityChange] = useState("");
  const [type, setType] = useState("ADJUST");
  const [reason, setReason] = useState("");
  const [batches, setBatches] = useState<{ batchId: number; batchNo: string }[]>([]);
  const [saving, setSaving] = useState(false);

  function selectMedicine(id: string) {
    setMedicineId(id);
    const row = stock.find((s) => s.medicineId === Number(id));
    setBatches(row ? row.batchList.map((b) => ({ batchId: b.batchId, batchNo: b.batchNo })) : []);
    setBatchId("");
  }

  async function submit() {
    if (!medicineId || !quantityChange) {
      toast.error("Medicine and quantity change are required");
      return;
    }
    setSaving(true);
    try {
      await inventoryApi.adjust({
        medicineId: Number(medicineId),
        batchId: batchId ? Number(batchId) : null,
        quantityChange: Number(quantityChange),
        type,
        reason: reason || null,
      });
      toast.success("Stock adjusted");
      onClose();
      onDone();
    } catch (e) {
      toast.error(errMsg(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Adjust Stock" footer={
      <>
        <button className="btn" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Adjust"}</button>
      </>
    }>
      <div className="form-grid">
        <div className="field span-2">
          <label>Medicine</label>
          <select className="select" value={medicineId} onChange={(e) => selectMedicine(e.target.value)}>
            <option value="">Select…</option>
            {stock.map((s) => (<option key={s.medicineId} value={s.medicineId}>{s.name} (on hand: {s.totalQty})</option>))}
          </select>
        </div>
        <div className="field">
          <label>Batch</label>
          <select className="select" value={batchId} onChange={(e) => setBatchId(e.target.value)}>
            <option value="">—</option>
            {batches.map((b) => (<option key={b.batchId} value={b.batchId}>{b.batchNo}</option>))}
          </select>
        </div>
        <div className="field">
          <label>Qty Change (+/-)</label>
          <input className="input" type="number" value={quantityChange} onChange={(e) => setQuantityChange(e.target.value)} />
        </div>
        <div className="field">
          <label>Type</label>
          <select className="select" value={type} onChange={(e) => setType(e.target.value)}>
            {["ADJUST", "DAMAGED", "EXPIRED"].map((t) => (<option key={t} value={t}>{t}</option>))}
          </select>
        </div>
        <div className="field span-2">
          <label>Reason</label>
          <input className="input" value={reason} onChange={(e) => setReason(e.target.value)} />
        </div>
      </div>
    </Modal>
  );
}

function TransferModal({ open, onClose, onDone, stock }: { open: boolean; onClose: () => void; onDone: () => void; stock: StockRow[] }) {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [medicineId, setMedicineId] = useState("");
  const [toBranchId, setToBranchId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      branchApi.all().then(setBranches).catch(() => {});
    }
  }, [open]);

  async function submit() {
    if (!medicineId || !toBranchId || !quantity) {
      toast.error("Medicine, destination branch and quantity are required");
      return;
    }
    setSaving(true);
    try {
      await inventoryApi.requestTransfer({
        medicineId: Number(medicineId),
        toBranchId: Number(toBranchId),
        quantity: Number(quantity),
        reason: reason || undefined,
      });
      toast.success("Transfer requested");
      onClose();
      onDone();
    } catch (e) {
      toast.error(errMsg(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Request Transfer" footer={
      <>
        <button className="btn" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Request"}</button>
      </>
    }>
      <div className="form-grid">
        <div className="field span-2">
          <label>Medicine</label>
          <select className="select" value={medicineId} onChange={(e) => setMedicineId(e.target.value)}>
            <option value="">Select…</option>
            {stock.map((s) => (<option key={s.medicineId} value={s.medicineId}>{s.name} (on hand: {s.totalQty})</option>))}
          </select>
        </div>
        <div className="field">
          <label>To Branch</label>
          <select className="select" value={toBranchId} onChange={(e) => setToBranchId(e.target.value)}>
            <option value="">Select…</option>
            {branches.map((b) => (<option key={b.id} value={b.id}>{b.name}</option>))}
          </select>
        </div>
        <div className="field">
          <label>Quantity</label>
          <input className="input" type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
        </div>
        <div className="field span-2">
          <label>Reason</label>
          <input className="input" value={reason} onChange={(e) => setReason(e.target.value)} />
        </div>
      </div>
    </Modal>
  );
}

function errMsg(e: unknown): string {
  return e instanceof ApiHttpError ? e.message : "Operation failed";
}