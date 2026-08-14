import { useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Modal } from "../components/Modal";
import { roleApi } from "../lib/api-endpoints";
import type { Role } from "../lib/types";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function RolesPage() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState<Role | null>(null);
  const [selected, setSelected] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    try {
      const [r, p] = await Promise.all([roleApi.list(), roleApi.permissions()]);
      setRoles(r);
      setPermissions(p);
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to load roles");
    } finally {
      setLoading(false);
    }
  }

  function openEditor(role: Role) {
    setEditing(role);
    setSelected(role.permissions);
  }

  function toggle(perm: string) {
    setSelected((s) => (s.includes(perm) ? s.filter((x) => x !== perm) : [...s, perm]));
  }

  async function save() {
    if (!editing) return;
    setSaving(true);
    try {
      await roleApi.updatePermissions(editing.id, selected);
      toast.success(`Permissions updated for ${editing.code}`);
      setEditing(null);
      load();
    } catch (err) {
      toast.error(err instanceof ApiHttpError ? err.message : "Failed to update permissions");
    } finally {
      setSaving(false);
    }
  }

  const groups = groupPermissions(permissions);

  return (
    <div>
      <DataTable<Role>
        rows={roles}
        loading={loading}
        emptyMessage="No roles found."
        columns={[
          { key: "code", header: "Code", render: (r) => <span className="mono">{r.code}</span> },
          { key: "name", header: "Name", render: (r) => r.name },
          { key: "description", header: "Description", render: (r) => r.description || "—" },
          { key: "perms", header: "Permissions", render: (r) => <span>{r.permissions.length} permission{r.permissions.length === 1 ? "" : "s"}</span> },
          { key: "actions", header: "", render: (r) => <button className="btn btn-sm" onClick={() => openEditor(r)}>Edit Permissions</button> },
        ]}
      />

      <Modal
        open={!!editing}
        onClose={() => setEditing(null)}
        wide
        title={editing ? `Edit Permissions — ${editing.code}` : ""}
        footer={
          <>
            <button className="btn" onClick={() => setEditing(null)}>Cancel</button>
            <button className="btn btn-primary" disabled={saving} onClick={save}>{saving ? "Saving…" : "Save"}</button>
          </>
        }
      >
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
          {groups.map((group) => (
            <div key={group.name} style={{ border: "1px solid var(--border)", borderRadius: 6, padding: 10 }}>
              <div style={{ fontWeight: 600, marginBottom: 6, textTransform: "capitalize", fontSize: 13 }}>{group.name}</div>
              {group.perms.map((p) => (
                <label key={p} className="menu-item" style={{ padding: "4px 6px", display: "flex", gap: 8 }}>
                  <input type="checkbox" checked={selected.includes(p)} onChange={() => toggle(p)} />
                  <span className="mono">{p}</span>
                </label>
              ))}
            </div>
          ))}
        </div>
      </Modal>
    </div>
  );
}

function groupPermissions(perms: string[]): { name: string; perms: string[] }[] {
  const map = new Map<string, string[]>();
  perms.forEach((p) => {
    const idx = p.indexOf(".");
    const group = idx > 0 ? p.slice(0, idx) : "other";
    map.set(group, [...(map.get(group) || []), p]);
  });
  return [...map.entries()].map(([name, perms]) => ({ name, perms: perms.sort() }));
}