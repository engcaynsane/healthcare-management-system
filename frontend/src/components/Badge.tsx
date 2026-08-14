import type { ReactNode } from "react";

type BadgeTone = "success" | "warning" | "danger" | "info" | "muted";

export function StatusBadge({ value }: { value: string | null | undefined }) {
  if (!value) return null;
  return <span className={`badge badge-${toneFor(value)}`}>{value.replaceAll("_", " ")}</span>;
}

export function Badge({ value, tone }: { value: ReactNode; tone: BadgeTone }) {
  return <span className={`badge badge-${tone}`}>{value}</span>;
}

function toneFor(value: string): BadgeTone {
  const v = value.toUpperCase();
  if (v.includes("COMPLETE") || v === "PAID" || v === "ACTIVE" || v === "RECEIVED" || v === "CONFIRMED" || v === "APPROVED") {
    return "success";
  }
  if (v === "CANCELLED" || v === "REJECTED" || v === "NO_SHOW" || v === "VOIDED" || v === "EXPIRED" || v === "DAMAGED") {
    return "danger";
  }
  if (v === "PARTIAL" || v === "REFUNDED" || v === "PARTIAL_REFUND" || v === "LOW" || v.includes("PROGRESS") || v === "IN_TRANSIT" || v === "PENDING") {
    return "warning";
  }
  return "info";
}