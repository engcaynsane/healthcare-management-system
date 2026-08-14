package com.hms.seed;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalog of every permission code in the system and the default permissions granted to each role.
 */
public final class PermissionCatalog {

    public static final Map<String, String> PERMISSIONS = new LinkedHashMap<>();

    static {
        PERMISSIONS.put("user.view", "View users");
        PERMISSIONS.put("user.create", "Create users");
        PERMISSIONS.put("user.update", "Update users");
        PERMISSIONS.put("user.delete", "Delete users");
        PERMISSIONS.put("role.manage", "Manage roles");
        PERMISSIONS.put("audit.view", "View audit logs");
        PERMISSIONS.put("notification.view", "View notifications");
        PERMISSIONS.put("notification.send", "Send notifications");

        PERMISSIONS.put("branch.view", "View branches");
        PERMISSIONS.put("branch.create", "Create branches");
        PERMISSIONS.put("branch.update", "Update branches");

        PERMISSIONS.put("patient.view", "View patients");
        PERMISSIONS.put("patient.create", "Create patients");
        PERMISSIONS.put("patient.update", "Update patients");
        PERMISSIONS.put("patient.delete", "Delete patients");

        PERMISSIONS.put("appointment.view", "View appointments");
        PERMISSIONS.put("appointment.create", "Create appointments");
        PERMISSIONS.put("appointment.update", "Update appointments");

        PERMISSIONS.put("doctor.view", "View doctors");
        PERMISSIONS.put("doctor.assign", "Assign doctors");

        PERMISSIONS.put("consultation.view", "View consultations");
        PERMISSIONS.put("consultation.create", "Create consultations");

        PERMISSIONS.put("medicine.view", "View medicines");
        PERMISSIONS.put("medicine.create", "Create medicines");
        PERMISSIONS.put("medicine.update", "Update medicines");
        PERMISSIONS.put("medicine.delete", "Delete medicines");

        PERMISSIONS.put("supplier.view", "View suppliers");
        PERMISSIONS.put("supplier.create", "Create suppliers");
        PERMISSIONS.put("supplier.update", "Update suppliers");

        PERMISSIONS.put("customer.view", "View customers");
        PERMISSIONS.put("customer.create", "Create customers");
        PERMISSIONS.put("customer.update", "Update customers");

        PERMISSIONS.put("inventory.view", "View inventory");
        PERMISSIONS.put("inventory.receive", "Receive stock");
        PERMISSIONS.put("inventory.adjust", "Adjust stock");
        PERMISSIONS.put("inventory.transfer", "Transfer stock");

        PERMISSIONS.put("sale.view", "View sales");
        PERMISSIONS.put("sale.create", "Create sales");
        PERMISSIONS.put("sale.refund", "Refund sales");
        PERMISSIONS.put("sale.priceOverride", "Override sale prices");

        PERMISSIONS.put("lab.view", "View lab tests");
        PERMISSIONS.put("lab.order", "Order lab tests");
        PERMISSIONS.put("lab.result", "Enter lab results");

        PERMISSIONS.put("billing.view", "View billing");
        PERMISSIONS.put("billing.create", "Create bills");
        PERMISSIONS.put("billing.refund", "Refund bills");
        PERMISSIONS.put("billing.discount", "Apply discounts");

        PERMISSIONS.put("finance.view", "View finances");
        PERMISSIONS.put("finance.expense", "Record expenses");
        PERMISSIONS.put("finance.report", "View financial reports");

        PERMISSIONS.put("report.view", "View reports");
    }

    public static final Map<String, String[]> ROLE_PERMISSIONS = new LinkedHashMap<>();

    static {
        String[] all = PERMISSIONS.keySet().toArray(String[]::new);
        ROLE_PERMISSIONS.put("SUPER_ADMIN", all);
        ROLE_PERMISSIONS.put("BRANCH_MANAGER", new String[]{
                "user.view", "user.create", "user.update", "audit.view",
                "notification.view", "notification.send", "branch.view",
                "patient.view", "patient.create", "patient.update", "patient.delete",
                "appointment.view", "appointment.create", "appointment.update",
                "doctor.view", "doctor.assign", "consultation.view", "consultation.create",
                "medicine.view", "medicine.create", "medicine.update",
                "supplier.view", "supplier.create", "supplier.update",
                "customer.view", "customer.create", "customer.update",
                "inventory.view", "inventory.receive", "inventory.adjust", "inventory.transfer",
                "sale.view", "sale.create", "sale.refund",
                "lab.view", "lab.order", "lab.result",
                "billing.view", "billing.create", "billing.refund", "billing.discount",
                "finance.view", "finance.expense", "finance.report",
                "report.view"});
        ROLE_PERMISSIONS.put("DOCTOR", new String[]{
                "patient.view", "patient.create", "appointment.view", "appointment.create", "appointment.update",
                "doctor.view", "consultation.view", "consultation.create",
                "lab.view", "lab.order", "notification.view"});
        ROLE_PERMISSIONS.put("RECEPTIONIST", new String[]{
                "patient.view", "patient.create", "patient.update",
                "appointment.view", "appointment.create", "appointment.update",
                "billing.view", "billing.create", "sale.view", "notification.view"});
        ROLE_PERMISSIONS.put("PHARMACIST", new String[]{
                "medicine.view", "customer.view", "customer.create", "customer.update",
                "inventory.view", "sale.view", "sale.create", "sale.refund",
                "billing.view", "lab.view", "notification.view"});
        ROLE_PERMISSIONS.put("LAB_TECHNICIAN", new String[]{
                "patient.view", "lab.view", "lab.order", "lab.result", "notification.view"});
        ROLE_PERMISSIONS.put("CASHIER", new String[]{
                "sale.view", "sale.create", "billing.view", "billing.create",
                "finance.view", "customer.view", "notification.view"});
        ROLE_PERMISSIONS.put("ACCOUNTANT", new String[]{
                "billing.view", "finance.view", "finance.expense", "finance.report",
                "audit.view", "report.view", "customer.view"});
        ROLE_PERMISSIONS.put("STORE_MANAGER", new String[]{
                "medicine.view", "medicine.create", "medicine.update", "medicine.delete",
                "supplier.view", "supplier.create", "supplier.update",
                "inventory.view", "inventory.receive", "inventory.adjust", "inventory.transfer",
                "customer.view", "sale.view", "report.view"});
    }

    public static Map<String, String> roleNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("SUPER_ADMIN", "Super Admin");
        names.put("BRANCH_MANAGER", "Branch Manager");
        names.put("DOCTOR", "Doctor");
        names.put("RECEPTIONIST", "Receptionist");
        names.put("PHARMACIST", "Pharmacist");
        names.put("LAB_TECHNICIAN", "Laboratory Technician");
        names.put("CASHIER", "Cashier");
        names.put("ACCOUNTANT", "Accountant");
        names.put("STORE_MANAGER", "Store Manager");
        return names;
    }

    private PermissionCatalog() {
    }
}