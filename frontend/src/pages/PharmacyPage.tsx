import { useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { Modal } from "../components/Modal";
import { Icon } from "../components/icons";
import { medicineApi } from "../lib/api-endpoints";
import type { Medicine, MedicineCategory } from "../lib/types";
import { formatMoney } from "../lib/format";
import { Badge } from "../components/Badge";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function PharmacyPage() {
  const [rows, setRows] = useState<Medicine[]>([]);
  const [categories, setCategories] = useState<MedicineCategory[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [q, setQ] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [categoryOpen, setCategoryOpen] = useState(false);
  const [editing, setEditing] = useState<Medicine | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, q, categoryId]);

  useEffect(() => {
    medicineApi.categories().then(setCategories).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  async function load() {
    setLoading(true);
    try {
      const p = await medicineApi.list({ q, categoryId: categoryId ? Number(categoryId) : undefined, page, size: 20 });
      setRows(p.content);
      setTotalPages(p.totalPages);
      setTotal(p.totalElements);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load medicines");
    } finally {
      setLoading(false);
    }
  }

  async function handleSave(body: Record<string, unknown>) {
    setSaving(true);
    try {
      if (editing) {
        await medicineApi.update(editing.id, body);
        toast.success("Medicine updated");
      } else {
        await medicineApi.create(body);
        toast.success("Medicine created");
      }
      setOpen(false);
      load();
      medicineApi.categories().then(setCategories);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to save medicine");
    } finally {
      setSaving(false);
    }
  }

  async function handleCategory(body: { name: string; description?: string }) {
    try {
      await medicineApi.createCategory(body);
      toast.success("Category created");
      setCategoryOpen(false);
      medicineApi.categories().then(setCategories);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to create category");
    }
  }

  return (
    <div>
      <div className="toolbar">
        <div className="search">
          <input className="input" placeholder="Search name, barcode…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        </div>
        <select className="select" style={{ width: 180 }} value={categoryId} onChange={(e) => { setCategoryId(e.target.value); setPage(0); }}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
        <div className="spacer" style={{ flex: 1 }} />
        <button className="btn" onClick={() => setCategoryOpen(true)}><Icon name="plus" /> Category</button>
        <button className="btn btn-primary" onClick={() => { setEditing(null); setOpen(true); }}><Icon name="plus" /> Add Medicine</button>
      </div>

      <DataTable<Medicine>
        rows={rows}
        loading={loading}
        emptyMessage="No medicines found."
        columns={[
          { key: "name", header: "Name", render: (r) => r.name },
          { key: "generic", header: "Generic", render: (r) => r.genericName || "—" },
          { key: "category", header: "Category", render: (r) => <Badge value={r.categoryName || "—"} tone="info" /> },
          { key: "barcode", header: "Barcode", render: (r) => <span className="mono">{r.barcode}</span> },
          { key: "selling", header: "Selling", render: (r) => formatMoney(r.sellingPrice) },
          { key: "cost", header: "Cost", render: (r) => formatMoney(r.costPrice) },
          { key: "reorder", header: "Reorder", render: (r) => r.reorderLevel },
          { key: "rx", header: "Rx", render: (r) => (r.requirePrescription ? <Badge value="Rx" tone="warning" /> : "—") },
          { key: "active", header: "Status", render: (r) => (r.active ? <Badge value="Active" tone="success" /> : <Badge value="Inactive" tone="muted" />) },
          { key: "actions", header: "", render: (r) => <button className="btn btn-sm" onClick={() => { setEditing(r); setOpen(true); }}>Edit</button> },
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />

      <MedicineModal open={open} onClose={() => setOpen(false)} editing={editing} categories={categories} saving={saving} onSave={handleSave} />
      <CategoryModal open={categoryOpen} onClose={() => setCategoryOpen(false)} onSave={handleCategory} />
    </div>
  );
}

function MedicineModal({
  open,
  onClose,
  editing,
  categories,
  saving,
  onSave,
}: {
  open: boolean;
  onClose: () => void;
  editing: Medicine | null;
  categories: MedicineCategory[];
  saving: boolean;
  onSave: (b: Record<string, unknown>) => void;
}) {
  const [form, setForm] = useState({
    name: "",
    genericName: "",
    brand: "",
    categoryId: "",
    strength: "",
    dosageForm: "",
    barcode: "",
    packSize: "",
    unit: "",
    reorderLevel: "0",
    requirePrescription: false,
    sellingPrice: "",
    costPrice: "",
  });

  useEffect(() => {
    if (open) {
      setForm({
        name: editing?.name ?? "",
        genericName: editing?.genericName ?? "",
        brand: editing?.brand ?? "",
        categoryId: editing?.categoryId != null ? String(editing.categoryId) : "",
        strength: editing?.strength ?? "",
        dosageForm: editing?.dosageForm ?? "",
        barcode: editing?.barcode ?? "",
        packSize: editing?.packSize ?? "",
        unit: editing?.unit ?? "",
        reorderLevel: editing?.reorderLevel != null ? String(editing.reorderLevel) : "0",
        requirePrescription: editing?.requirePrescription ?? false,
        sellingPrice: editing?.sellingPrice != null ? String(editing.sellingPrice) : "",
        costPrice: editing?.costPrice != null ? String(editing.costPrice) : "",
      });
    }
  }, [open, editing]);

  function submit() {
    if (!form.name || !form.barcode) {
      toast.error("Name and barcode are required");
      return;
    }
    onSave({
      name: form.name,
      genericName: form.genericName || null,
      brand: form.brand || null,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      strength: form.strength || null,
      dosageForm: form.dosageForm || null,
      barcode: form.barcode,
      packSize: form.packSize || null,
      unit: form.unit || null,
      reorderLevel: Number(form.reorderLevel) || 0,
      requirePrescription: form.requirePrescription,
      sellingPrice: form.sellingPrice ? Number(form.sellingPrice) : null,
      costPrice: form.costPrice ? Number(form.costPrice) : null,
    });
  }

  const set = (k: keyof typeof form, v: string | boolean) => setForm((f) => ({ ...f, [k]: v }));

  return (
    <Modal
      open={open}
      onClose={onClose}
      wide
      title={editing ? `Edit ${editing.name}` : "Add Medicine"}
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
          <input className="input" value={form.name} onChange={(e) => set("name", e.target.value)} />
        </div>
        <div className="field">
          <label>Barcode <span className="req">*</span></label>
          <input className="input" value={form.barcode} onChange={(e) => set("barcode", e.target.value)} />
        </div>
        <div className="field">
          <label>Generic Name</label>
          <input className="input" value={form.genericName} onChange={(e) => set("genericName", e.target.value)} />
        </div>
        <div className="field">
          <label>Brand</label>
          <input className="input" value={form.brand} onChange={(e) => set("brand", e.target.value)} />
        </div>
        <div className="field">
          <label>Category</label>
          <select className="select" value={form.categoryId} onChange={(e) => set("categoryId", e.target.value)}>
            <option value="">—</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Dosage Form</label>
          <input className="input" value={form.dosageForm} onChange={(e) => set("dosageForm", e.target.value)} placeholder="Tablet" />
        </div>
        <div className="field">
          <label>Strength</label>
          <input className="input" value={form.strength} onChange={(e) => set("strength", e.target.value)} placeholder="500mg" />
        </div>
        <div className="field">
          <label>Unit</label>
          <input className="input" value={form.unit} onChange={(e) => set("unit", e.target.value)} placeholder="tablet" />
        </div>
        <div className="field">
          <label>Pack Size</label>
          <input className="input" value={form.packSize} onChange={(e) => set("packSize", e.target.value)} />
        </div>
        <div className="field">
          <label>Reorder Level</label>
          <input className="input" type="number" value={form.reorderLevel} onChange={(e) => set("reorderLevel", e.target.value)} />
        </div>
        <div className="field">
          <label>Selling Price</label>
          <input className="input" type="number" step="0.01" value={form.sellingPrice} onChange={(e) => set("sellingPrice", e.target.value)} />
        </div>
        <div className="field">
          <label>Cost Price</label>
          <input className="input" type="number" step="0.01" value={form.costPrice} onChange={(e) => set("costPrice", e.target.value)} />
        </div>
        <div className="field span-2">
          <div className="checkbox-row">
            <input type="checkbox" checked={form.requirePrescription} onChange={(e) => set("requirePrescription", e.target.checked)} id="rx" />
            <label htmlFor="rx">Requires Prescription</label>
          </div>
        </div>
      </div>
    </Modal>
  );
}

function CategoryModal({
  open,
  onClose,
  onSave,
}: {
  open: boolean;
  onClose: () => void;
  onSave: (b: { name: string; description?: string }) => void;
}) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  useEffect(() => {
    if (open) {
      setName("");
      setDescription("");
    }
  }, [open]);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="New Category"
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={!name.trim()} onClick={() => onSave({ name, description: description || undefined })}>Create</button>
        </>
      }
    >
      <div className="form-grid">
        <div className="field span-2">
          <label>Name <span className="req">*</span></label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field span-2">
          <label>Description</label>
          <textarea className="textarea" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
      </div>
    </Modal>
  );
}