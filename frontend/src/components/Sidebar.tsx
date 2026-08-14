import { NavLink } from "react-router-dom";
import { Icon } from "./icons";
import { useAuthStore } from "../stores/auth";
import type { ReactNode } from "react";

type NavItem = { to: string; label: string; icon: string; permission?: string; end?: boolean };

const sections: { title: string; items: NavItem[] }[] = [
  {
    title: "Overview",
    items: [{ to: "/", label: "Dashboard", icon: "dashboard", end: true }],
  },
  {
    title: "Clinical",
    items: [
      { to: "/patients", label: "Patients", icon: "patients", permission: "patient.view" },
      { to: "/doctors", label: "Doctors", icon: "doctors", permission: "doctor.view" },
      { to: "/appointments", label: "Appointments", icon: "appointments", permission: "appointment.view" },
      { to: "/lab", label: "Laboratory", icon: "lab", permission: "lab.view" },
    ],
  },
  {
    title: "Pharmacy",
    items: [
      { to: "/pharmacy", label: "Medicines", icon: "pharmacy", permission: "medicine.view" },
      { to: "/inventory", label: "Inventory", icon: "inventory", permission: "inventory.view" },
      { to: "/pos", label: "POS / Retail", icon: "pos", permission: "sale.create" },
      { to: "/sales", label: "Sales", icon: "sales", permission: "sale.view" },
    ],
  },
  {
    title: "Billing",
    items: [{ to: "/invoices", label: "Invoices", icon: "billing", permission: "billing.view" }],
  },
  {
    title: "Contacts",
    items: [
      { to: "/customers", label: "Customers", icon: "customers", permission: "customer.view" },
      { to: "/suppliers", label: "Suppliers", icon: "suppliers", permission: "supplier.view" },
    ],
  },
  {
    title: "Administration",
    items: [
      { to: "/users", label: "Users", icon: "users", permission: "user.view" },
      { to: "/roles", label: "Roles & Permissions", icon: "roles", permission: "role.manage" },
      { to: "/branches", label: "Branches", icon: "branches", permission: "branch.view" },
      { to: "/audit", label: "Audit Logs", icon: "audit", permission: "audit.view" },
    ],
  },
];

export function Sidebar({ open, onClose }: { open: boolean; onClose: () => void }) {
  const user = useAuthStore((s) => s.user);
  const check = (perm?: string) => !perm || user?.permissions?.includes(perm);

  return (
    <>
      <div className={open ? "sidebar-backdrop" : "sidebar-backdrop"} onClick={onClose} />
      <aside className={`sidebar ${open ? "open" : ""}`}>
        <div className="sidebar-brand">
          <div className="logo">H</div>
          <div className="brand-text">HMS Clinic</div>
        </div>
        <nav className="sidebar-nav">
          {sections.map((section) => (
            <div key={section.title}>
              <div className="nav-section">{section.title}</div>
              {section.items.map((item) =>
                check(item.permission) ? (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}
                    onClick={onClose}
                  >
                    <Icon name={item.icon} />
                    <span>{item.label}</span>
                  </NavLink>
                ) : null,
              )}
            </div>
          ))}
        </nav>
        <div className="sidebar-footer">
          {user ? (
            <div style={{ fontSize: 13, color: "#94a3b8" }}>
              <div style={{ color: "#e2e8f0", fontWeight: 500 }}>{user.fullName}</div>
              {user.branchName && (
                <div style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {user.branchName}
                </div>
              )}
            </div>
          ) : null}
          <div style={{ marginTop: 6 }}>v1.0.0</div>
        </div>
      </aside>
    </>
  );
}

export function NavSectionTitle({ children }: { children: ReactNode }) {
  return <div className="nav-section">{children}</div>;
}