import { useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { supplierApi } from "../lib/api-endpoints";
import type { Supplier } from "../lib/types";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function SuppliersPage() {
  const [rows, setRows] = useState<Supplier[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Supplier | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, q]);

  async function load() {
    setLoading(true);
    try {
      const p = await supplierApi.list({ q, page, size: 20 });
      setRows(p.content);
      setTotalPages(p.totalPages);
      setTotal(p.totalElements);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load suppliers");
    } finally {
      setLoading(false);
    }
  }

  function reload() {
    load();
  }

  async function handleSave(body: Partial<Supplier>) {
    setSaving(true);
    try {
      if (editing) {
        await supplierApi.update(editing.id, body);
        toast.success("Supplier updated");
      } else {
        await supplierApi.create(body);
        toast.success("Supplier created");
      }
      setOpen(false);
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to save supplier");
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
          <Icon name="plus" /> New Supplier
        </button>
      </div>

      <DataTable<Supplier>
        rows={rows}
        loading={loading}
        emptyMessage="No suppliers found."
        columns={[
          { key: "name", header: "Name", render: (r) => r.name },
          { key: "person", header: "Contact Person", render: (r) => r.contactPerson || "—" },
          { key: "phone", header: "Phone", render: (r) => r.phone || "—" },
          { key: "email", header: "Email", render: (r) => r.email || "—" },
          { key: "address", header: "Address", render: (r) => r.address || "—" },
          { key: "actions", header: "", render: (r) => <button className="btn btn-sm" onClick={() => { setEditing(r); setOpen(true); }}>Edit</button> },
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />

      <SupplierModal open={open} onClose={() => setOpen(false)} editing={editing} saving={saving} onSave={handleSave} />
    </div>
  );
}

function SupplierModal({
  open,
  onClose,
  editing,
  saving,
  onSave,
}: {
  open: boolean;
  onClose: () => void;
  editing: Supplier | null;
  saving: boolean;
  onSave: (b: Partial<Supplier>) => void;
}) {
  const [name, setName] = useState("");
  const [contactPerson, setContactPerson] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [address, setAddress] = useState("");

  useEffect(() => {
    if (open) {
      setName(editing?.name ?? "");
      setContactPerson(editing?.contactPerson ?? "");
      setPhone(editing?.phone ?? "");
      setEmail(editing?.email ?? "");
      setAddress(editing?.address ?? "");
    }
  }, [open, editing]);

  function submit() {
    onSave({
      name,
      contactPerson: contactPerson || null,
      phone: phone || null,
      email: email || null,
      address: address || null,
    });
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={editing ? "Edit Supplier" : "New Supplier"}
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={saving} onClick={submit}>{saving ? "Saving…" : "Save"}</button>
        </>
      }
    >
      <div className="form-grid">
        <div className="field span-2">
          <label>Name <span className="req">*</span></label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label>Contact Person</label>
          <input className="input" value={contactPerson} onChange={(e) => setContactPerson(e.target.value)} />
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
          <label>Address</label>
          <input className="input" value={address} onChange={(e) => setAddress(e.target.value)} />
        </div>
      </div>
    </Modal>
  );
}