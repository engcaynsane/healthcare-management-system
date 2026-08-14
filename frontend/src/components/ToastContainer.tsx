import { useToastStore } from "../stores/toast";

export function ToastContainer() {
  const items = useToastStore((s) => s.items);
  const remove = useToastStore((s) => s.remove);

  if (items.length === 0) return null;
  return (
    <div className="toast-container">
      {items.map((t) => (
        <div key={t.id} className={`toast toast-${t.kind}`} onClick={() => remove(t.id)}>
          <span>{messageIcon(t.kind)}</span>
          <span>{t.message}</span>
        </div>
      ))}
    </div>
  );
}

function messageIcon(kind: string): string {
  switch (kind) {
    case "success":
      return "✓";
    case "error":
      return "✕";
    default:
      return "ℹ";
  }
}