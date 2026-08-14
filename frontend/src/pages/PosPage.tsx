import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { inventoryApi, saleApi, customerApi, patientApi, PAYMENT_METHODS } from "../lib/api-endpoints";
import type { StockRow, Customer, Patient } from "../lib/types";
import { formatMoney } from "../lib/format";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";
import { useAuthStore } from "../stores/auth";

type CartLine = {
  medicineId: number;
  name: string;
  unitPrice: number;
  quantity: number;
};

export function PosPage() {
  const user = useAuthStore((s) => s.user);
  const hasSale = user?.permissions?.includes("sale.create") ?? false;
  const navigate = useNavigate();

  const [stock, setStock] = useState<StockRow[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [search, setSearch] = useState("");
  const [customerId, setCustomerId] = useState("");
  const [patientId, setPatientId] = useState("");
  const [discount, setDiscount] = useState("");
  const [tax, setTax] = useState("");
  const [paid, setPaid] = useState("");
  const [method, setMethod] = useState("CASH");
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    inventoryApi.stock().then(setStock).catch(() => {});
    customerApi.list({ size: 200 }).then((p) => setCustomers(p.content)).catch(() => {});
    patientApi.list({ size: 200 }).then((p) => setPatients(p.content)).catch(() => {});
  }, []);

  const items = useMemo(
    () => stock.filter((s) => `${s.name} ${s.barcode} ${s.genericName || ""}`.toLowerCase().includes(search.toLowerCase())),
    [stock, search],
  );

  const subtotal = cart.reduce((sum, l) => sum + l.unitPrice * l.quantity, 0);
  const discountVal = Number(discount) || 0;
  const taxVal = Number(tax) || 0;
  const total = Math.max(0, subtotal - discountVal + taxVal);
  const change = Number(paid || 0) - total;

  function inCart(medicineId: number) {
    return cart.find((l) => l.medicineId === medicineId);
  }

  function addToCart(row: StockRow) {
    if (row.totalQty <= 0) {
      toast.error("Out of stock");
      return;
    }
    if (row.sellingPrice == null) {
      toast.error("No selling price set for this medicine");
      return;
    }
    const existing = inCart(row.medicineId);
    if (existing) {
      if (existing.quantity >= row.totalQty) {
        toast.error("Only " + row.totalQty + " available");
        return;
      }
      setCart((c) => c.map((l) => (l.medicineId === row.medicineId ? { ...l, quantity: l.quantity + 1 } : l)));
    } else {
      setCart((c) => [...c, { medicineId: row.medicineId, name: row.name, unitPrice: row.sellingPrice!, quantity: 1 }]);
    }
  }

  function setQty(medicineId: number, quantity: number) {
    if (quantity <= 0) {
      setCart((c) => c.filter((l) => l.medicineId !== medicineId));
    } else {
      setCart((c) => c.map((l) => (l.medicineId === medicineId ? { ...l, quantity } : l)));
    }
  }

  function removeLine(medicineId: number) {
    setCart((c) => c.filter((l) => l.medicineId !== medicineId));
  }

  async function completeSale() {
    if (cart.length === 0) {
      toast.error("Cart is empty");
      return;
    }
    if (paid && Number(paid) < total) {
      toast.error("Paid amount is less than total");
      return;
    }
    setSubmitting(true);
    try {
      const res = await saleApi.create({
        items: cart.map((l) => ({ medicineId: l.medicineId, quantity: l.quantity, unitPrice: l.unitPrice })),
        customerId: customerId ? Number(customerId) : null,
        patientId: patientId ? Number(patientId) : null,
        discount: discountVal || null,
        tax: taxVal || null,
        paidAmount: Number(paid) || total,
        paymentMethod: method,
        note: note || null,
      });
      toast.success(`Sale ${res.saleNumber} completed`);
      setCart([]);
      setPaid("");
      setNote("");
      navigate("/sales");
    } catch (e) {
      toast.error(e instanceof ApiHttpError ? e.message : "Failed to complete sale");
    } finally {
      setSubmitting(false);
    }
  }

  if (!hasSale) {
    return <div className="empty">You do not have permission to create sales.</div>;
  }

  return (
    <div style={{ display: "grid", gridTemplateColumns: "1.6fr 1fr", gap: 18, alignItems: "start" }}>
      <div className="card">
        <div className="card-header">Products</div>
        <div className="card-body">
          <div className="search" style={{ marginBottom: 14 }}>
            <input className="input" placeholder="Search products…" value={search} onChange={(e) => setSearch(e.target.value)} autoFocus />
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill,minmax(150px,1fr))", gap: 10, maxHeight: 520, overflowY: "auto" }}>
            {items.map((s) => (
              <button key={s.medicineId} className="btn" style={{ flexDirection: "column", alignItems: "flex-start", height: "auto", padding: 10 }} onClick={() => addToCart(s)} disabled={s.totalQty <= 0}>
                <span style={{ fontWeight: 600, textAlign: "left" }}>{s.name}</span>
                <span style={{ fontSize: 11, color: "var(--text-muted)" }}>{formatMoney(s.sellingPrice)} · {s.totalQty} left</span>
              </button>
            ))}
            {items.length === 0 && <div className="empty" style={{ gridColumn: "1 / -1" }}>No products match</div>}
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">Current Sale</div>
        <div className="card-body" style={{ paddingTop: 14 }}>
          <div className="form-grid" style={{ marginBottom: 4 }}>
            <div className="field">
              <label>Customer</label>
              <select className="select" value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
                <option value="">Walk-in</option>
                {customers.map((c) => (<option key={c.id} value={c.id}>{c.name}</option>))}
              </select>
            </div>
            <div className="field">
              <label>Patient</label>
              <select className="select" value={patientId} onChange={(e) => setPatientId(e.target.value)}>
                <option value="">None</option>
                {patients.map((p) => (<option key={p.id} value={p.id}>{p.firstName} {p.lastName}</option>))}
              </select>
            </div>
          </div>

          <div style={{ maxHeight: 220, overflowY: "auto", marginBottom: 6 }}>
            {cart.length === 0 ? (
              <div className="empty">Click products to add</div>
            ) : (
              cart.map((l) => (
                <div key={l.medicineId} className="menu-item" style={{ borderRadius: 0, borderBottom: "1px solid var(--border)" }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 500 }}>{l.name}</div>
                    <div style={{ fontSize: 12, color: "var(--text-muted)" }}>{formatMoney(l.unitPrice)} each</div>
                  </div>
                  <input className="input" type="number" min={1} style={{ width: 58 }} value={l.quantity} onChange={(e) => setQty(l.medicineId, Number(e.target.value))} />
                  <div style={{ width: 74, textAlign: "right", fontWeight: 600 }}>{formatMoney(l.unitPrice * l.quantity)}</div>
                  <button className="btn btn-ghost btn-icon" onClick={() => removeLine(l.medicineId)}>✕</button>
                </div>
              ))
            )}
          </div>

          <div className="form-grid">
            <div className="field">
              <label>Discount</label>
              <input className="input" type="number" step="0.01" value={discount} onChange={(e) => setDiscount(e.target.value)} />
            </div>
            <div className="field">
              <label>Tax</label>
              <input className="input" type="number" step="0.01" value={tax} onChange={(e) => setTax(e.target.value)} />
            </div>
            <div className="field">
              <label>Payment Method</label>
              <select className="select" value={method} onChange={(e) => setMethod(e.target.value)}>
                {PAYMENT_METHODS.map((m) => (<option key={m} value={m}>{m.replaceAll("_", " ")}</option>))}
              </select>
            </div>
            <div className="field">
              <label>Amount Paid</label>
              <input className="input" type="number" step="0.01" value={paid} onChange={(e) => setPaid(e.target.value)} placeholder={String(total)} />
            </div>
            <div className="field span-2">
              <label>Note</label>
              <input className="input" value={note} onChange={(e) => setNote(e.target.value)} />
            </div>
          </div>

          <div style={{ borderTop: "1px solid var(--border)", paddingTop: 12, marginTop: 4 }}>
            <div style={{ display: "flex", justifyContent: "space-between" }}><span className="stat-label">Subtotal</span><span>{formatMoney(subtotal)}</span></div>
            <div style={{ display: "flex", justifyContent: "space-between" }}><span className="stat-label">Total</span><span style={{ fontSize: 18, fontWeight: 700 }}>{formatMoney(total)}</span></div>
            <div style={{ display: "flex", justifyContent: "space-between" }}><span className="stat-label">Change</span><span className={change < 0 ? "badge badge-danger" : ""}>{formatMoney(change >= 0 ? change : 0)}</span></div>
          </div>

          <button className="btn btn-primary" style={{ width: "100%", marginTop: 12, padding: 10 }} disabled={submitting || cart.length === 0} onClick={completeSale}>
            {submitting ? "Processing…" : `Complete Sale · ${formatMoney(total)}`}
          </button>
        </div>
      </div>
    </div>
  );
}