import { useCallback, useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { Badge } from "../components/Badge";
import { branchApi } from "../lib/api-endpoints";
import type { Branch } from "../lib/types";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function BranchesPage() {
  const [rows, setRows] = useState<Branch[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Branch | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, q]);

  async function load() {
    setLoading(true);
    try {
      const p = await branchApi.list({ q, page, size: 20 });
      setRows(p.content);
      setTotalPages(p.totalPages);
      setTotal(p.totalElements);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load branches");
    } finally {
      setLoading(false);
    }
  }

  const reload = useCallback(() => load(), [page, q]);

  async function handleSave(body: Partial<Branch>) {
    setSaving(true);
    try {
      if (editing) {
        await branchApi.update(editing.id, body);
        toast.success("Branch updated");
      } else {
        await branchApi.create(body);
        toast.success("Branch created");
      }
      setOpen(false);
      reload();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to save branch");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <div className="toolbar">
        <div className="search">
          <input className="input" placeholder="Search branches…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        </div>
        <div className="spacer" style={{ flex: 1 }} />
        <button className="btn btn-primary" onClick={() => { setEditing(null); setOpen(true); }}>
          <Icon name="plus" /> New Branch
        </button>
      </div>

      <DataTable<Branch>
        rows={rows}
        loading={loading}
        emptyMessage="No branches found."
        columns={[
          { key: "name", header: "Name", render: (r) => r.name },
          { key: "code", header: "Code", render: (r) => <span className="mono">{r.code}</span> },
          { key: "address", header: "Address", render: (r) => r.address || "—" },
          { key: "phone", header: "Phone", render: (r) => r.phone || "—" },
          { key: "type", header: "Type", render: (r) => (r.central ? <Badge value="Central" tone="info" /> : <Badge value="Branch" tone="muted" />) },
          { key: "active", header: "Status", render: (r) => (r.active ? <Badge value="Active" tone="success" /> : <Badge value="Inactive" tone="muted" />) },
          { key: "actions", header: "", render: (r) => <button className="btn btn-sm" onClick={() => { setEditing(r); setOpen(true); }}>Edit</button> },
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />

      <BranchModal open={open} onClose={() => setOpen(false)} editing={editing} saving={saving} onSave={handleSave} />
    </div>
  );
}

function BranchModal({
  open,
  onClose,
  editing,
  saving,
  onSave,
}: {
  open: boolean;
  onClose: () => void;
  editing: Branch | null;
  saving: boolean;
  onSave: (b: Partial<Branch>) => void;
}) {
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [address, setAddress] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [active, setActive] = useState(true);
  const [central, setCentral] = useState(false);

  useEffect(() => {
    if (open) {
      setName(editing?.name ?? "");
      setCode(editing?.code ?? "");
      setAddress(editing?.address ?? "");
      setPhone(editing?.phone ?? "");
      setEmail(editing?.email ?? "");
      setActive(editing?.active ?? true);
      setCentral(editing?.central ?? false);
    }
  }, [open, editing]);

  function submit() {
    if (!name || !code) {
      toast.error("Name and code are required");
      return;
    }
    onSave({
      name,
      code: code.toUpperCase(),
      address: address || null,
      phone: phone || null,
      email: email || null,
      active,
      central,
    });
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={editing ? `Edit Branch ${editing.name}` : "New Branch"}
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
          <label>Code <span className="req">*</span></label>
          <input className="input" value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="MAIN" />
        </div>
        <div className="field span-2">
          <label>Address</label>
          <input className="input" value={address} onChange={(e) => setAddress(e.target.value)} />
        </div>
        <div className="field">
          <label>Phone</label>
          <input className="input" value={phone} onChange={(e) => setPhone(e.target.value)} />
        </div>
        <div className="field">
          <label>Email</label>
          <input className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field span-2" style={{ display: "flex", gap: 20 }}>
          <div className="checkbox-row">
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} id="b-active" />
            <label htmlFor="b-active">Active</label>
          </div>
          <div className="checkbox-row">
            <input type="checkbox" checked={central} onChange={(e) => setCentral(e.target.checked)} id="b-central" />
            <label htmlFor="b-central">Central Branch</label>
          </div>
        </div>
      </div>
    </Modal>
  );
}