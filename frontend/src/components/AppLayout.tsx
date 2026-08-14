import { useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
import { useAuthStore } from "../stores/auth";

const titles: Record<string, string> = {
  "/": "Dashboard",
  "/patients": "Patients",
  "/doctors": "Doctors",
  "/appointments": "Appointments",
  "/lab": "Laboratory",
  "/pharmacy": "Medicines",
  "/inventory": "Inventory",
  "/pos": "Point of Sale",
  "/sales": "Sales",
  "/invoices": "Invoices & Billing",
  "/customers": "Customers",
  "/suppliers": "Suppliers",
  "/users": "Users",
  "/roles": "Roles & Permissions",
  "/branches": "Branches",
  "/audit": "Audit Logs",
  "/profile": "My Profile",
};

export function AppLayout() {
  const user = useAuthStore((s) => s.user);
  const accessToken = useAuthStore((s) => s.accessToken);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  if (!user || !accessToken) {
    return <Navigate to="/login" replace />;
  }

  const pathKey = `/${location.pathname.split("/")[1]}`;
  const title = titles[pathKey] ?? "HMS";

  return (
    <div className="app-shell">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="main">
        <Topbar onMenu={() => setSidebarOpen(true)} />
        <main className="content">
          <h1 style={{ marginBottom: 18, fontSize: 20 }}>{title}</h1>
          <Outlet />
        </main>
      </div>
    </div>
  );
}