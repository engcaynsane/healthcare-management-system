import { useAuthStore } from "../stores/auth";
import { ManagementDashboard } from "./dashboards/ManagementDashboard";
import { DoctorDashboard } from "./dashboards/DoctorDashboard";
import { ReceptionistDashboard } from "./dashboards/ReceptionistDashboard";
import { PharmacistDashboard } from "./dashboards/PharmacistDashboard";
import { LabDashboard } from "./dashboards/LabDashboard";
import { CashierDashboard } from "./dashboards/CashierDashboard";
import { AccountantDashboard } from "./dashboards/AccountantDashboard";
import { StoreDashboard } from "./dashboards/StoreDashboard";

export function DashboardPage() {
  const roles = useAuthStore((s) => s.user?.roles) ?? [];

  if (roles.includes("SUPER_ADMIN") || roles.includes("BRANCH_MANAGER")) {
    return <ManagementDashboard />;
  }
  if (roles.includes("DOCTOR")) return <DoctorDashboard />;
  if (roles.includes("LAB_TECHNICIAN")) return <LabDashboard />;
  if (roles.includes("RECEPTIONIST")) return <ReceptionistDashboard />;
  if (roles.includes("PHARMACIST")) return <PharmacistDashboard />;
  if (roles.includes("STORE_MANAGER")) return <StoreDashboard />;
  if (roles.includes("CASHIER")) return <CashierDashboard />;
  if (roles.includes("ACCOUNTANT")) return <AccountantDashboard />;
  return <ManagementDashboard />;
}