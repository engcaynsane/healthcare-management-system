export function Pagination({
  page,
  totalPages,
  totalElements,
  onPage,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onPage: (page: number) => void;
}) {
  if (totalPages <= 0) return null;
  return (
    <div className="pagination">
      <span style={{ color: "var(--text-muted)", fontSize: 12, marginRight: "auto" }}>
        {totalElements} item{totalElements === 1 ? "" : "s"}
      </span>
      <button className="btn btn-sm" disabled={page <= 0} onClick={() => onPage(page - 1)}>
        ← Prev
      </button>
      <span style={{ fontSize: 13 }}>
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn btn-sm" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>
        Next →
      </button>
    </div>
  );
}