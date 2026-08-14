import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/AppLayout";
import { ToastContainer } from "./components/ToastContainer";
import { useAuthStore } from "./stores/auth";
import { can } from "./lib/permissions";

import { LoginPage } from "./pages/auth/LoginPage";
import { ProfilePage } from "./pages/auth/ProfilePage";
import { DashboardPage } from "./pages/DashboardPage";
import { PatientsPage } from "./pages/PatientsPage";
import { DoctorsPage } from "./pages/DoctorsPage";
import { AppointmentsPage } from "./pages/AppointmentsPage";
import { LaboratoryPage } from "./pages/LaboratoryPage";
import { PharmacyPage } from "./pages/PharmacyPage";
import { InventoryPage } from "./pages/InventoryPage";
import { PosPage } from "./pages/PosPage";
import { SalesPage } from "./pages/SalesPage";
import { InvoicesPage } from "./pages/InvoicesPage";
import { CustomersPage } from "./pages/CustomersPage";
import { SuppliersPage } from "./pages/SuppliersPage";
import { UsersPage } from "./pages/UsersPage";
import { RolesPage } from "./pages/RolesPage";
import { BranchesPage } from "./pages/BranchesPage";
import { AuditPage } from "./pages/AuditPage";

function RequireAuth({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user);
  const accessToken = useAuthStore((s) => s.accessToken);
  if (!user || !accessToken) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function RequirePermission({ permission, children }: { permission: string; children: React.ReactNode }) {
  if (!can(permission)) return <Navigate to="/" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastContainer />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <RequireAuth>
              <AppLayout />
            </RequireAuth>
          }
        >
          <Route path="/" element={<DashboardPage />} />
          <Route path="/patients" element={<PatientsPage />} />
          <Route path="/doctors" element={<DoctorsPage />} />
          <Route path="/appointments" element={<AppointmentsPage />} />
          <Route path="/lab" element={<LaboratoryPage />} />
          <Route path="/pharmacy" element={<PharmacyPage />} />
          <Route path="/inventory" element={<InventoryPage />} />
          <Route path="/pos" element={<PosPage />} />
          <Route path="/sales" element={<SalesPage />} />
          <Route path="/invoices" element={<InvoicesPage />} />
          <Route path="/customers" element={<CustomersPage />} />
          <Route path="/suppliers" element={<SuppliersPage />} />
          <Route path="/users" element={<RequirePermission permission="user.view"><UsersPage /></RequirePermission>} />
          <Route path="/roles" element={<RequirePermission permission="role.manage"><RolesPage /></RequirePermission>} />
          <Route path="/branches" element={<RequirePermission permission="branch.view"><BranchesPage /></RequirePermission>} />
          <Route path="/audit" element={<RequirePermission permission="audit.view"><AuditPage /></RequirePermission>} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}