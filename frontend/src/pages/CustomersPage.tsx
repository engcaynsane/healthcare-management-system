import { useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { customerApi } from "../lib/api-endpoints";
import type { Customer } from "../lib/types";
import { formatNumber, formatMoney } from "../lib/format";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function CustomersPage() {
  const [rows, setRows] = useState<Customer[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Customer | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, q]);

  async function load() {
    setLoading(true);
    try {
      const p = await customerApi.list({ q, page, size: 20 });
      setRows(p.content);
      setTotalPages(p.totalPages);
      setTotal(p.totalElements);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load customers");
    } finally {
      setLoading(false);
    }
  }

  function reload() {
    load();
  }

  async function handleSave(body: Partial<Customer>) {
    setSaving(true);
    try {
      if (editing) {
        await customerApi.update(editing.id, body);
        toast.success("Customer updated");
      } else {
        await customerApi.create(body);
        toast.success("Customer created");
      }
      setOpen(false);
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to save customer");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <div className="toolbar">
        <div className="search">
          <input className="input" placeholder="Search name, phone…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        </div>
        <div className="spacer" style={{ flex: 1 }} />
        <button className="btn btn-primary" onClick={() => { setEditing(null); setOpen(true); }}>
          <Icon name="plus" /> New Customer
        </button>
      </div>

      <DataTable<Customer>
        rows={rows}
        loading={loading}
        emptyMessage="No customers found."
        columns={[
          { key: "name", header: "Name", render: (r) => r.name },
          { key: "phone", header: "Phone", render: (r) => r.phone || "—" },
          { key: "email", header: "Email", render: (r) => r.email || "—" },
          { key: "points", header: "Loyalty", render: (r) => formatNumber(r.loyaltyPoints) },
          { key: "credit", header: "Credit Limit", render: (r) => formatMoney(r.creditLimit) },
          { key: "balance", header: "Balance", render: (r) => formatMoney(r.balance) },
          { key: "actions", header: "", render: (r) => <button className="btn btn-sm" onClick={() => { setEditing(r); setOpen(true); }}>Edit</button> },
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />

      <CustomerModal open={open} onClose={() => setOpen(false)} editing={editing} saving={saving} onSave={handleSave} />
    </div>
  );
}

function CustomerModal({
  open,
  onClose,
  editing,
  saving,
  onSave,
}: {
  open: boolean;
  onClose: () => void;
  editing: Customer | null;
  saving: boolean;
  onSave: (b: Partial<Customer>) => void;
}) {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [address, setAddress] = useState("");
  const [creditLimit, setCreditLimit] = useState("");
  const [notes, setNotes] = useState("");

  useEffect(() => {
    if (open) {
      setName(editing?.name ?? "");
      setPhone(editing?.phone ?? "");
      setEmail(editing?.email ?? "");
      setAddress(editing?.address ?? "");
      setCreditLimit(editing?.creditLimit != null ? String(editing.creditLimit) : "");
      setNotes(editing?.notes ?? "");
    }
  }, [open, editing]);

  function submit() {
    onSave({
      name,
      phone: phone || null,
      email: email || null,
      address: address || null,
      creditLimit: creditLimit ? Number(creditLimit) : null,
      notes: notes || null,
    });
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={editing ? "Edit Customer" : "New Customer"}
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Save"}</button>
        </>
      }
    >
      <div className="form-grid">
        <div className="field">
          <label>Name <span className="req">*</span></label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label>Phone</label>
          <input className="input" value={phone} onChange={(e) => setPhone(e.target.value)} />
        </div>
        <div className="field">
          <label>Email</label>
          <input className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field">
          <label>Credit Limit</label>
          <input className="input" type="number" value={creditLimit} onChange={(e) => setCreditLimit(e.target.value)} />
        </div>
        <div className="field span-2">
          <label>Address</label>
          <input className="input" value={address} onChange={(e) => setAddress(e.target.value)} />
        </div>
        <div className="field span-2">
          <label>Notes</label>
          <textarea className="textarea" value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>
      </div>
    </Modal>
  );
}