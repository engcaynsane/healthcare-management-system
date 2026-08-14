import { useEffect, useState } from "react";
import { DataTable } from "../components/DataTable";
import { Pagination } from "../components/Pagination";
import { auditApi } from "../lib/api-endpoints";
import type { AuditLog } from "../lib/types";
import { formatDateTime } from "../lib/format";
import { Badge } from "../components/Badge";
import { toast } from "../stores/toast";
import { ApiHttpError } from "../lib/api";

export function AuditPage() {
  const [rows, setRows] = useState<AuditLog[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [action, setAction] = useState("");
  const [username, setUsername] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, action, username]);

  function load() {
    setLoading(true);
    auditApi.list({ action: action || undefined, username: username || undefined, page, size: 20 })
      .then((p) => { setRows(p.content); setTotalPages(p.totalPages); setTotal(p.totalElements); })
      .catch((e) => toast.error(e instanceof ApiHttpError ? e.message : "Failed to load audit logs"))
      .finally(() => setLoading(false));
  }

  return (
    <div>
      <div className="toolbar">
        <input className="input" style={{ width: 200 }} placeholder="Filter by action…" value={action} onChange={(e) => { setAction(e.target.value); setPage(0); }} />
        <input className="input" style={{ width: 180 }} placeholder="Filter by username…" value={username} onChange={(e) => { setUsername(e.target.value); setPage(0); }} />
      </div>

      <DataTable<AuditLog>
        rows={rows}
        loading={loading}
        emptyMessage="No audit log entries."
        columns={[
          { key: "date", header: "Date", render: (r) => formatDateTime(r.createdAt) },
          { key: "action", header: "Action", render: (r) => <Badge value={r.action} tone="info" /> },
          { key: "details", header: "Details", render: (r) => r.details || "—" },
          { key: "user", header: "Username", render: (r) => <span className="mono">{r.username || "—"}</span> },
          { key: "ip", header: "IP", render: (r) => <span className="mono">{r.ipAddress || "—"}</span> },
        ]}
      />
      <Pagination page={page} totalPages={totalPages} totalElements={total} onPage={setPage} />
    </div>
  );
}