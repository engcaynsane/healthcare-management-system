export type Paged<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
};

export type Option = { value: string | number; label: string };

export type User = {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  active: boolean;
  branchId: number | null;
  branchName: string | null;
  roles: string[];
};

export type Role = {
  id: number;
  code: string;
  name: string;
  description: string | null;
  permissions: string[];
};

export type Branch = {
  id: number;
  name: string;
  code: string;
  address: string | null;
  phone: string | null;
  email: string | null;
  active: boolean;
  central: boolean;
};

export type Patient = {
  id: number;
  patientCode: string;
  firstName: string;
  lastName: string;
  gender: string | null;
  dateOfBirth: string | null;
  phone: string | null;
  email: string | null;
  bloodGroup: string | null;
  nationalId: string | null;
  allergies: string | null;
  medicalHistory: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  active: boolean;
  branchId: number;
};

export type Doctor = {
  id: number;
  userId: number | null;
  userName: string | null;
  fullName: string;
  specialty: string | null;
  licenseNumber: string | null;
  consultationFee: number | null;
  active: boolean;
};

export type MedicineCategory = {
  id: number;
  name: string;
  description: string | null;
};

export type Medicine = {
  id: number;
  name: string;
  genericName: string | null;
  brand: string | null;
  categoryId: number | null;
  categoryName: string | null;
  strength: string | null;
  dosageForm: string | null;
  barcode: string;
  packSize: string | null;
  unit: string | null;
  reorderLevel: number;
  requirePrescription: boolean;
  sellingPrice: number | null;
  costPrice: number | null;
  active: boolean;
};

export type Customer = {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  loyaltyPoints: number;
  creditLimit: number | null;
  balance: number | null;
  notes: string | null;
  active: boolean;
};

export type Supplier = {
  id: number;
  name: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  active: boolean;
};

export type StockBatch = {
  batchId: number;
  batchNo: string;
  expiryDate: string | null;
  quantity: number;
};

export type StockRow = {
  medicineId: number;
  name: string;
  genericName: string | null;
  barcode: string;
  sellingPrice: number | null;
  costPrice: number | null;
  reorderLevel: number;
  unit: string | null;
  active: boolean;
  totalQty: number;
  batches: number;
  expiringSoon: boolean;
  lowStock: boolean;
  batchList: StockBatch[];
};

export type Movement = {
  id: number;
  type: string | null;
  medicineName: string | null;
  medicineId: number | null;
  batchNo: string | null;
  quantityChange: number;
  beforeQty: number;
  afterQty: number;
  reference: string | null;
  reason: string | null;
  createdAt: string;
};

export type Transfer = {
  id: number;
  transferNumber: string;
  fromBranchId: number;
  toBranchId: number;
  medicineId: number;
  medicineName: string;
  batchNo: string | null;
  quantity: number;
  status: string;
  reason: string | null;
  createdAt: string;
};

export type SaleSummary = {
  id: number;
  saleNumber: string;
  createdAt: string;
  customerName: string | null;
  patientName: string | null;
  total: number;
  paidAmount: number;
  paymentMethod: string | null;
  status: string;
  cashierName: string | null;
};

export type SaleItem = {
  id: number;
  medicineName: string;
  batchNo: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  refunded: boolean;
};

export type SaleDetail = {
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
  items: SaleItem[];
};

export type LabTest = {
  id: number;
  code: string;
  name: string;
  category: string | null;
  price: number | null;
  description: string | null;
  active: boolean;
};

export type LabOrderSummary = {
  id: number;
  orderNumber: string;
  patientName: string;
  status: string;
  createdAt: string;
  requestedByName: string;
  itemCount: number;
};

export type LabOrderItem = {
  id: number;
  testId: number;
  code: string;
  name: string;
  price: number | null;
  result: string | null;
  resultNotes: string | null;
  status: string;
};

export type LabOrderDetail = {
  id: number;
  orderNumber: string;
  patientId: number;
  patientName: string;
  status: string;
  createdAt: string;
  requestedByName: string;
  items: LabOrderItem[];
};

export type InvoiceSummary = {
  id: number;
  invoiceNumber: string;
  patientName: string | null;
  customerName: string | null;
  description: string | null;
  total: number;
  paidAmount: number;
  status: string;
  createdAt: string;
  issuedByName: string;
};

export type Payment = {
  id: number;
  amount: number;
  method: string | null;
  reference: string | null;
  paidAt: string;
  invoiceId: number;
};

export type InvoiceDetail = {
  id: number;
  invoiceNumber: string;
  patientName: string | null;
  customerName: string | null;
  description: string | null;
  subtotal: number;
  discount: number;
  tax: number;
  total: number;
  paidAmount: number;
  status: string;
  issuedByName: string;
  createdAt: string;
  payments: Payment[];
};

export type AppointmentSummary = {
  id: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  startTime: string;
  endTime: string | null;
  status: string;
  purpose: string | null;
  notes: string | null;
};

export type AuditLog = {
  id: number;
  action: string;
  details: string | null;
  userId: number | null;
  username: string | null;
  ipAddress: string | null;
  branchId: number | null;
  createdAt: string;
};

export type NotificationItem = {
  id: number;
  title: string;
  message: string;
  type: string | null;
  read: boolean;
  createdAt: string;
};

export type DashboardSummary = {
  salesToday: number;
  revenueToday: number;
  patientsToday: number;
  appointmentsToday: number;
  pendingLabs: number;
  lowStock: number;
  expiringSoon: number;
  pendingTransfers: number;
};