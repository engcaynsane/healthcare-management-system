import { request, LoginData } from "./api";
import type {
  AppointmentSummary,
  Paged,
  Patient,
  Medicine,
  MedicineCategory,
  Movement,
  Transfer,
  Customer,
  Supplier,
  Doctor,
  StockRow,
  LabTest,
  LabOrderSummary,
  LabOrderDetail,
  InvoiceSummary,
  InvoiceDetail,
  SaleSummary,
  User,
  Role,
  Branch,
  AuditLog,
  NotificationItem,
  DashboardSummary,
} from "./types";

// ---------- enum catalogs ----------
export const APPOINTMENT_STATUSES = ["SCHEDULED", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "NO_SHOW"];
export const PAYMENT_METHODS = ["CASH", "CARD", "MOBILE_MONEY", "BANK_TRANSFER", "INSURANCE", "CREDIT"];
export const PAYMENT_STATUSES = ["PAID", "PARTIAL", "UNPAID"];
export const SALE_STATUSES = ["COMPLETED", "PARTIAL_REFUND", "REFUNDED", "VOIDED"];
export const TRANSFER_STATUSES = ["PENDING", "APPROVED", "REJECTED", "IN_TRANSIT", "RECEIVED"];
export const MOVEMENT_TYPES = [
  "RECEIVE",
  "ADJUST",
  "SALE",
  "SALE_REFUND",
  "TRANSFER_IN",
  "TRANSFER_OUT",
  "DAMAGED",
  "EXPIRED",
];
export const LAB_STATUSES = ["REQUESTED", "SAMPLE_COLLECTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

// ---------- auth ----------
export const authApi = {
  login: (username: string, password: string) =>
    request<LoginData>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    }),
  changePassword: (oldPassword: string, newPassword: string) =>
    request<void>("/api/auth/change-password", {
      method: "PUT",
      body: JSON.stringify({ oldPassword, newPassword }),
    }),
  switchBranch: (branchId: number) =>
    request<LoginData>(`/api/auth/switch-branch/${branchId}`, { method: "POST" }),
  logout: (refreshToken: string) =>
    request<void>("/api/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }),
};

// ---------- branches ----------
export const branchApi = {
  list: (params: { q?: string; page?: number; size?: number } = {}) =>
    request<Paged<Branch>>(`/api/branches?${query(params)}`),
  all: () => request<Branch[]>("/api/branches/all"),
  create: (body: Partial<Branch>) =>
    request<Branch>("/api/branches", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: Partial<Branch>) =>
    request<Branch>(`/api/branches/${id}`, { method: "PUT", body: JSON.stringify(body) }),
};

// ---------- users ----------
export const userApi = {
  list: (params: { q?: string; role?: string; branchId?: number; page?: number; size?: number } = {}) =>
    request<Paged<User>>(`/api/users?${query(params)}`),
  get: (id: number) => request<User>(`/api/users/${id}`),
  create: (body: { username: string; fullName: string; email?: string; phone?: string; password: string; roleCodes?: string[]; branchId?: number }) =>
    request<User>("/api/users", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: { fullName: string; email?: string; phone?: string; active?: boolean; roleCodes?: string[]; branchId?: number }) =>
    request<User>(`/api/users/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  activate: (id: number) => request<User>(`/api/users/${id}/activate`, { method: "POST" }),
  deactivate: (id: number) => request<User>(`/api/users/${id}/deactivate`, { method: "POST" }),
};

// ---------- roles ----------
export const roleApi = {
  list: () => request<Role[]>("/api/roles"),
  permissions: () => request<string[]>("/api/roles/permissions"),
  updatePermissions: (id: number, codes: string[]) =>
    request<Role>(`/api/roles/${id}/permissions`, { method: "PUT", body: JSON.stringify(codes) }),
};

// ---------- patients ----------
export const patientApi = {
  list: (params: { q?: string; page?: number; size?: number } = {}) =>
    request<Paged<Patient>>(`/api/patients?${query(params)}`),
  get: (id: number) => request<Patient>(`/api/patients/${id}`),
  create: (body: Partial<Patient>) =>
    request<Patient>("/api/patients", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: Partial<Patient>) =>
    request<Patient>(`/api/patients/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  delete: (id: number) => request<void>(`/api/patients/${id}`, { method: "DELETE" }),
};

// ---------- doctors ----------
export const doctorApi = {
  list: (q?: string) => request<Doctor[]>(`/api/doctors?${query({ q })}`),
  create: (body: Record<string, unknown>) =>
    request<Doctor>("/api/doctors", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: Record<string, unknown>) =>
    request<Doctor>(`/api/doctors/${id}`, { method: "PUT", body: JSON.stringify(body) }),
};

// ---------- pharmacy ----------
export const medicineApi = {
  categories: () => request<MedicineCategory[]>("/api/medicine-categories"),
  createCategory: (body: { name: string; description?: string }) =>
    request<MedicineCategory>("/api/medicine-categories", { method: "POST", body: JSON.stringify(body) }),
  list: (params: { q?: string; categoryId?: number; page?: number; size?: number } = {}) =>
    request<Paged<Medicine>>(`/api/medicines?${query(params)}`),
  get: (id: number) => request<Medicine>(`/api/medicines/${id}`),
  create: (body: Record<string, unknown>) =>
    request<Medicine>("/api/medicines", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: Record<string, unknown>) =>
    request<Medicine>(`/api/medicines/${id}`, { method: "PUT", body: JSON.stringify(body) }),
};

// ---------- inventory ----------
export const inventoryApi = {
  stock: () => request<StockRow[]>("/api/inventory"),
  lowStock: () => request<StockRow[]>("/api/inventory/low-stock"),
  expiring: (days = 30) => request<StockRow["batchList"]>(`/api/inventory/expiring?days=${days}`),
  movements: (page = 0, size = 20) => request<Paged<Movement>>(`/api/inventory/movements?page=${page}&size=${size}`),
  receive: (body: Record<string, unknown>) =>
    request<unknown>("/api/inventory/receive", { method: "POST", body: JSON.stringify(body) }),
  adjust: (body: Record<string, unknown>) =>
    request<void>("/api/inventory/adjust", { method: "POST", body: JSON.stringify(body) }),
  transfers: (params: { status?: string; page?: number; size?: number } = {}) =>
    request<Paged<Transfer>>(`/api/inventory/transfers?${query(params)}`),
  requestTransfer: (body: { medicineId: number; toBranchId: number; quantity: number; reason?: string }) =>
    request<Transfer>("/api/inventory/transfers", { method: "POST", body: JSON.stringify(body) }),
  approveTransfer: (id: number) => request<Transfer>(`/api/inventory/transfers/${id}/approve`, { method: "POST" }),
  rejectTransfer: (id: number, reason?: string) =>
    request<Transfer>(`/api/inventory/transfers/${id}/reject?${query({ reason })}`, { method: "POST" }),
  shipTransfer: (id: number) => request<Transfer>(`/api/inventory/transfers/${id}/ship`, { method: "POST" }),
  receiveTransfer: (id: number) => request<Transfer>(`/api/inventory/transfers/${id}/receive`, { method: "POST" }),
};

// ---------- customers ----------
export const customerApi = {
  list: (params: { q?: string; page?: number; size?: number } = {}) =>
    request<Paged<Customer>>(`/api/customers?${query(params)}`),
  create: (body: Partial<Customer>) =>
    request<Customer>("/api/customers", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: Partial<Customer>) =>
    request<Customer>(`/api/customers/${id}`, { method: "PUT", body: JSON.stringify(body) }),
};

// ---------- suppliers ----------
export const supplierApi = {
  list: (params: { q?: string; page?: number; size?: number } = {}) =>
    request<Paged<Supplier>>(`/api/suppliers?${query(params)}`),
  create: (body: Partial<Supplier>) =>
    request<Supplier>("/api/suppliers", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: Partial<Supplier>) =>
    request<Supplier>(`/api/suppliers/${id}`, { method: "PUT", body: JSON.stringify(body) }),
};

// ---------- sales ----------
export const saleApi = {
  list: (params: { date?: string; status?: string; q?: string; page?: number; size?: number } = {}) =>
    request<Paged<SaleSummary>>(`/api/sales?${query(params)}`),
  get: (id: number) => request<SaleSummary & { items?: unknown }>(`/api/sales/${id}`),
  create: (body: Record<string, unknown>) =>
    request<UnknownSaleDetail>("/api/sales", { method: "POST", body: JSON.stringify(body) }),
  detail: (id: number) =>
    request<{
      id: number;
      saleNumber: string;
      createdAt: string;
      customerName: string | null;
      patientName: string | null;
      subtotal: number;
      discount: number;
      tax: number;
      total: number;
      paidAmount: number;
      changeAmount: number;
      paymentMethod: string | null;
      status: string;
      cashierName: string | null;
      note: string | null;
      items: { id: number; medicineName: string; batchNo: string | null; quantity: number; unitPrice: number; lineTotal: number; refunded: boolean }[];
    }>(`/api/sales/${id}`),
  refund: (id: number, reason?: string) =>
    request<UnknownSaleDetail>(`/api/sales/${id}/refund`, {
      method: "POST",
      body: JSON.stringify({ reason }),
    }),
};

type UnknownSaleDetail = {
  id: number;
  saleNumber: string;
  total: number;
  status: string;
};

// ---------- lab ----------
export const labApi = {
  tests: () => request<LabTest[]>("/api/lab/tests"),
  createTest: (body: { code: string; name: string; category?: string; price?: number; description?: string }) =>
    request<LabTest>("/api/lab/tests", { method: "POST", body: JSON.stringify(body) }),
  orders: (params: { status?: string; page?: number; size?: number } = {}) =>
    request<Paged<LabOrderSummary>>(`/api/lab/orders?${query(params)}`),
  orderDetail: (id: number) => request<LabOrderDetail>(`/api/lab/orders/${id}`),
  createOrder: (body: { patientId: number; testIds: number[] }) =>
    request<LabOrderDetail>("/api/lab/orders", { method: "POST", body: JSON.stringify(body) }),
  enterResult: (id: number, body: { itemId: number; result?: string; resultNotes?: string }) =>
    request<unknown>(`/api/lab/orders/${id}/result`, { method: "POST", body: JSON.stringify(body) }),
  complete: (id: number) => request<LabOrderDetail>(`/api/lab/orders/${id}/complete`, { method: "POST" }),
};

// ---------- billing ----------
export const billingApi = {
  list: (params: { date?: string; status?: string; page?: number; size?: number } = {}) =>
    request<Paged<InvoiceSummary>>(`/api/invoices?${query(params)}`),
  get: (id: number) => request<InvoiceDetail>(`/api/invoices/${id}`),
  create: (body: Record<string, unknown>) =>
    request<unknown>("/api/invoices", { method: "POST", body: JSON.stringify(body) }),
  pay: (id: number, body: { amount: number; method?: string; reference?: string }) =>
    request<unknown>(`/api/invoices/${id}/payments`, { method: "POST", body: JSON.stringify(body) }),
};

// ---------- appointments ----------
export const appointmentApi = {
  list: (params: { date?: string; status?: string; doctorId?: number; page?: number; size?: number } = {}) =>
    request<Paged<AppointmentSummary>>(`/api/appointments?${query(params)}`),
  create: (body: Record<string, unknown>) =>
    request<AppointmentSummary>("/api/appointments", { method: "POST", body: JSON.stringify(body) }),
  updateStatus: (id: number, status: string) =>
    request<AppointmentSummary>(`/api/appointments/${id}/status`, {
      method: "POST",
      body: JSON.stringify({ status }),
    }),
};

// ---------- audit ----------
export const auditApi = {
  list: (params: { action?: string; username?: string; page?: number; size?: number } = {}) =>
    request<Paged<AuditLog>>(`/api/audit-logs?${query(params)}`),
};

// ---------- notifications ----------
export const notificationApi = {
  list: (page = 0, size = 30) => request<Paged<NotificationItem>>(`/api/notifications?page=${page}&size=${size}`),
  unreadCount: () => request<{ count: number }>("/api/notifications/unread-count"),
  markRead: (id: number) => request<NotificationItem>(`/api/notifications/${id}/read`, { method: "POST" }),
  markAllRead: () => request<void>("/api/notifications/read-all", { method: "POST" }),
  send: (body: { title: string; message: string; type?: string; userId?: number }) =>
    request<void>("/api/notifications/send", { method: "POST", body: JSON.stringify(body) }),
};

// ---------- dashboard ----------
export const dashboardApi = {
  summary: () => request<DashboardSummary>("/api/dashboard/summary"),
};

function query(params: Record<string, unknown>): string {
  const url = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      url.set(key, String(value));
    }
  });
  return url.toString();
}