package com.hms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class HmsApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void loginAndSeedDataAvailable() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("SUPER_ADMIN"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(body).path("data").path("accessToken").asText();
        assertTrue(token.length() > 20);

        // authenticated request with token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.branchName").value("Main Branch"));

        // unauthenticated request must be rejected
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is(403));
    }

    @Test
    void createPatientAndMedicine() throws Exception {
        String token = loginToken();

        MvcResult patientCreated = mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"phone\":\"0711223344\",\"gender\":\"Male\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientCode").exists())
                .andReturn();

        String patientId = objectMapper.readTree(patientCreated.getResponse().getContentAsString())
                .path("data").path("id").asText();

        MvcResult medCreated = mockMvc.perform(post("/api/medicines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Paracetamol 500mg\",\"barcode\":\"8901234567890\",\"sellingPrice\":10.0}"))
                .andExpect(status().isOk())
                .andReturn();

        String medId = objectMapper.readTree(medCreated.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // receive stock
        mockMvc.perform(post("/api/inventory/receive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":%s,\"batchNo\":\"B1\",\"expiryDate\":\"2027-12-31\",\"quantity\":50,\"costPrice\":5.0}".formatted(medId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(50));

        // point-of-sale sale
        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%s,\"items\":[{\"medicineId\":%s,\"quantity\":3}],\"paymentMethod\":\"CASH\",\"paidAmount\":30}".formatted(patientId, medId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(30.0))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));

        // stock should be reduced
        mockMvc.perform(get("/api/inventory")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // dashboard summary
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientsToday").isNumber());

        // app registration endpoint flows
        mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(405));

        // audit log visible
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void authFlows() throws Exception {
        // wrong password -> 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(login.getResponse().getContentAsString()).path("data");
        String token = data.path("accessToken").asText();
        String refresh = data.path("refreshToken").asText();

        // me
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions.length()").isNumber());

        // refresh token rotation
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());

        // invalid branch switch -> 404
        mockMvc.perform(post("/api/auth/switch-branch/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // logout revokes the token; refresh afterwards is rejected
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userRolesBranchFlows() throws Exception {
        String token = loginToken();
        String suf = uniqueSuffix();

        // branch create/list/update
        MvcResult branch = mockMvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Garden City\",\"code\":\"GC%s\",\"address\":\"Nairobi\",\"active\":true}".formatted(suf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("GC" + suf))
                .andReturn();
        long branchId = idOf(branch);
        mockMvc.perform(get("/api/branches").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/branches/all").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(put("/api/branches/" + branchId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Garden City Updated\",\"code\":\"GC%s\"}".formatted(suf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Garden City Updated"));

        // roles list + permission catalog
        mockMvc.perform(get("/api/roles").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/roles/permissions").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        // update permissions of DOCTOR role
        MvcResult roles = mockMvc.perform(get("/api/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode roleList = objectMapper.readTree(roles.getResponse().getContentAsString()).path("data");
        long doctorRoleId = -1;
        for (JsonNode n : roleList) {
            if ("DOCTOR".equals(n.path("code").asText())) {
                doctorRoleId = n.path("id").asLong();
            }
        }
        assertTrue(doctorRoleId > 0);
        mockMvc.perform(put("/api/roles/" + doctorRoleId + "/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"patient.view\",\"appointment.view\",\"doctor.view\",\"lab.view\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions.length()").value(4));

        // create user
        MvcResult user = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cash%s\",\"fullName\":\"Cashier One\",\"email\":\"cash%s@hms.local\",\"password\":\"Passw0rd!\",\"roleCodes\":[\"CASHIER\"],\"branchId\":%d}".formatted(suf, suf, branchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("cash" + suf))
                .andReturn();
        long userId = idOf(user);

        mockMvc.perform(get("/api/users?q=cash" + suf).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/" + userId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.username").value("cash" + suf));
        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cashier Two\",\"email\":\"cash%s@hms.local\"}".formatted(suf)))
                .andExpect(jsonPath("$.data.fullName").value("Cashier Two"));
        mockMvc.perform(post("/api/users/" + userId + "/deactivate").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.active").value(false));
        mockMvc.perform(post("/api/users/" + userId + "/activate").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.active").value(true));

        // new user can login, cannot switch to a branch they don't own, can change password
        String cashToken = loginToken("cash" + suf, "Passw0rd!");
        mockMvc.perform(post("/api/auth/switch-branch/1")
                        .header("Authorization", "Bearer " + cashToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/auth/change-password")
                        .header("Authorization", "Bearer " + cashToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Passw0rd!\",\"newPassword\":\"NewPass123\"}"))
                .andExpect(status().isOk());
        assertTrue(loginToken("cash" + suf, "NewPass123").length() > 20);
        // wrong current password -> 400
        mockMvc.perform(put("/api/auth/change-password")
                        .header("Authorization", "Bearer " + loginToken("cash" + suf, "NewPass123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"Passw0rd!\",\"newPassword\":\"Xyz123456\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerSupplierMedicineInventoryFlows() throws Exception {
        String token = loginToken();
        String suf = uniqueSuffix();

        // medicine category
        MvcResult cat = mockMvc.perform(post("/api/medicine-categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Analgesics %s\"}".formatted(suf)))
                .andExpect(status().isOk())
                .andReturn();
        long catId = idOf(cat);
        mockMvc.perform(get("/api/medicine-categories").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        // duplicate category -> 409
        mockMvc.perform(post("/api/medicine-categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Analgesics %s\"}".formatted(suf)))
                .andExpect(status().isConflict());

        // medicine with category
        MvcResult med = mockMvc.perform(post("/api/medicines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Amoxy 500\",\"genericName\":\"Amoxicillin\",\"barcode\":\"AM%s\",\"categoryId\":%d,\"sellingPrice\":120.5,\"costPrice\":70.0,\"reorderLevel\":10}".formatted(suf, catId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value(catId))
                .andReturn();
        long medId = idOf(med);
        // duplicate barcode -> 409
        mockMvc.perform(post("/api/medicines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"barcode\":\"AM%s\"}".formatted(suf)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/medicines?q=Amoxy").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/medicines/" + medId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.barcode").value("AM" + suf));
        mockMvc.perform(put("/api/medicines/" + medId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Amoxy 500 Plus\",\"barcode\":\"AM%s\",\"sellingPrice\":130.0}".formatted(suf)))
                .andExpect(jsonPath("$.data.name").value("Amoxy 500 Plus"));

        // supplier
        MvcResult sup = mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Global Pharma %s\",\"contactPerson\":\"Mr X\",\"phone\":\"0711111111\",\"email\":\"g%s@pharma.com\"}".formatted(suf, suf)))
                .andExpect(status().isOk())
                .andReturn();
        long supId = idOf(sup);
        mockMvc.perform(get("/api/suppliers?q=Global").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(put("/api/suppliers/" + supId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Global Pharma Ltd %s\",\"contactPerson\":\"Mr Y\"}".formatted(suf)))
                .andExpect(status().isOk());

        // customer
        MvcResult cust = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Walk-in %s\",\"phone\":\"0722222222\",\"creditLimit\":5000}".formatted(suf)))
                .andExpect(status().isOk())
                .andReturn();
        long custId = idOf(cust);
        mockMvc.perform(get("/api/customers").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/customers/" + custId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.balance").value(0));
        mockMvc.perform(put("/api/customers/" + custId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Walk-in VIP %s\",\"creditLimit\":10000}".formatted(suf)))
                .andExpect(status().isOk());

        // inventory: receive with supplier, adjust, views, over-adjust -> 400
        mockMvc.perform(post("/api/inventory/receive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":%d,\"batchNo\":\"B%s\",\"expiryDate\":\"2028-12-31\",\"quantity\":100,\"costPrice\":65.0,\"supplierId\":%d,\"location\":\"Shelf A\"}".formatted(medId, suf, supId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(100));

        mockMvc.perform(post("/api/inventory/adjust")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":%d,\"quantityChange\":-5,\"reason\":\"Damage\"}".formatted(medId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/inventory/adjust")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":%d,\"quantityChange\":-100000,\"reason\":\"Too much\"}".formatted(medId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/inventory").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/inventory/low-stock").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/inventory/expiring?days=60").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/inventory/movements").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void doctorSeesOnlyTheirData() throws Exception {
        String admin = loginToken();
        String suf = uniqueSuffix();

        // ensure DOCTOR role grants what this scenario needs (other tests may shrink it)
        MvcResult roles = mockMvc.perform(get("/api/roles").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn();
        JsonNode roleList = objectMapper.readTree(roles.getResponse().getContentAsString()).path("data");
        long doctorRoleId = -1;
        for (JsonNode n : roleList) {
            if ("DOCTOR".equals(n.path("code").asText())) {
                doctorRoleId = n.path("id").asLong();
            }
        }
        assertTrue(doctorRoleId > 0);
        mockMvc.perform(put("/api/roles/" + doctorRoleId + "/permissions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"patient.view\",\"appointment.view\",\"appointment.create\",\"appointment.update\",\"doctor.view\",\"lab.view\",\"lab.order\"]"))
                .andExpect(status().isOk());

        long myPatientId = createPatient(admin, "MyPatient " + suf);
        long otherPatientId = createPatient(admin, "OtherPatient " + suf);

        // doctor login account + linked doctor record
        String drUsername = "dr" + suf;
        long doctorUserId = idOf(mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"fullName\":\"Dr One %s\",\"password\":\"Doctor123\",\"roleCodes\":[\"DOCTOR\"],\"branchId\":1}".formatted(drUsername, suf)))
                .andExpect(status().isOk()).andReturn());
        long myDoctorId = idOf(mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d,\"firstName\":\"Dr Doc%s\",\"lastName\":\"One\"}".formatted(doctorUserId, suf)))
                .andExpect(status().isOk()).andReturn());
        long otherDoctorId = idOf(mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Other\",\"lastName\":\"Doc %s\"}".formatted(suf)))
                .andExpect(status().isOk()).andReturn());

        long myApptId = idOf(mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%d,\"doctorId\":%d,\"startTime\":\"2026-09-01T09:00:00\"}".formatted(myPatientId, myDoctorId)))
                .andExpect(status().isOk()).andReturn());
        long otherApptId = idOf(mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%d,\"doctorId\":%d,\"startTime\":\"2026-09-02T09:00:00\"}".formatted(otherPatientId, otherDoctorId)))
                .andExpect(status().isOk()).andReturn());

        // lab order created by admin (someone else, must stay hidden)
        long testId = idOf(mockMvc.perform(post("/api/lab/tests")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"GLU%s\",\"name\":\"Glucose\",\"category\":\"Chemistry\",\"price\":300}".formatted(suf)))
                .andExpect(status().isOk()).andReturn());
        long adminOrderId = idOf(mockMvc.perform(post("/api/lab/orders")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%d,\"testIds\":[%d]}".formatted(otherPatientId, testId)))
                .andExpect(status().isOk()).andReturn());

        String dr = loginToken(drUsername, "Doctor123");

        // appointments: only their own
        JsonNode appts = objectMapper.readTree(mockMvc.perform(get("/api/appointments")
                        .header("Authorization", "Bearer " + dr))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("content");
        List<Long> apptIds = ids(appts);
        assertTrue(apptIds.contains(myApptId), "doctor sees own appointment");
        assertFalse(apptIds.contains(otherApptId), "doctor must not see another doctor's appointment");

        // cannot change another doctor's appointment status
        mockMvc.perform(post("/api/appointments/" + otherApptId + "/status")
                        .header("Authorization", "Bearer " + dr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isNotFound());

        // patients: only those with their own appointments
        JsonNode patients = objectMapper.readTree(mockMvc.perform(get("/api/patients")
                        .header("Authorization", "Bearer " + dr))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("content");
        List<Long> patientIds = ids(patients);
        assertTrue(patientIds.contains(myPatientId), "doctor sees own patient");
        assertFalse(patientIds.contains(otherPatientId), "doctor must not see unrelated patient");
        mockMvc.perform(get("/api/patients/" + myPatientId).header("Authorization", "Bearer " + dr)).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients/" + otherPatientId).header("Authorization", "Bearer " + dr)).andExpect(status().isNotFound());

        // creating an appointment as the doctor forces their own doctor record
        MvcResult selfAppt = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + dr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%d,\"doctorId\":%d,\"startTime\":\"2026-09-03T10:00:00\"}".formatted(myPatientId, otherDoctorId)))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(objectMapper.readTree(selfAppt.getResponse().getContentAsString())
                        .path("data").path("doctorId").asLong() == myDoctorId, "doctor appointment must be assigned to them");

        // lab: only orders they requested
        long myOrderId = idOf(mockMvc.perform(post("/api/lab/orders")
                        .header("Authorization", "Bearer " + dr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%d,\"testIds\":[%d]}".formatted(myPatientId, testId)))
                .andExpect(status().isOk()).andReturn());
        JsonNode orders = objectMapper.readTree(mockMvc.perform(get("/api/lab/orders")
                        .header("Authorization", "Bearer " + dr))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("content");
        List<Long> orderIds = ids(orders);
        assertTrue(orderIds.contains(myOrderId), "doctor sees own lab order");
        assertFalse(orderIds.contains(adminOrderId), "doctor must not see someone else's lab order");
        mockMvc.perform(get("/api/lab/orders/" + myOrderId).header("Authorization", "Bearer " + dr)).andExpect(status().isOk());
        mockMvc.perform(get("/api/lab/orders/" + adminOrderId).header("Authorization", "Bearer " + dr)).andExpect(status().isNotFound());
    }

    @Test
    void doctorAppointmentLabBillingFlows() throws Exception {
        String token = loginToken();
        String suf = uniqueSuffix();
        long patientId = createPatient(token, "Patient " + suf);

        // doctor create/list/update
        MvcResult doctor = mockMvc.perform(post("/api/doctors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Jane\",\"lastName\":\"Doe %s\",\"specialty\":\"Pediatrics\",\"consultationFee\":800}".formatted(suf)))
                .andExpect(status().isOk())
                .andReturn();
        long doctorId = idOf(doctor);
        mockMvc.perform(get("/api/doctors").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(put("/api/doctors/" + doctorId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Jane\",\"lastName\":\"Doe %s\",\"specialty\":\"Pediatrics\",\"consultationFee\":900}".formatted(suf)))
                .andExpect(jsonPath("$.data.consultationFee").value(900));

        // patient update
        mockMvc.perform(put("/api/patients/" + patientId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Patient\",\"lastName\":\"%s\",\"phone\":\"0733333333\",\"bloodGroup\":\"O+\"}".formatted(suf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bloodGroup").value("O+"));

        // appointment
        MvcResult appt = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%d,\"doctorId\":%d,\"startTime\":\"2026-08-20T09:00:00\",\"endTime\":\"2026-08-20T09:30:00\",\"purpose\":\"Routine check\"}".formatted(patientId, doctorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andReturn();
        long apptId = idOf(appt);
        mockMvc.perform(get("/api/appointments?date=2026-08-20").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(post("/api/appointments/" + apptId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // lab test + order
        MvcResult lt = mockMvc.perform(post("/api/lab/tests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CBC%s\",\"name\":\"Complete Blood Count\",\"category\":\"Hematology\",\"price\":450}".formatted(suf)))
                .andExpect(status().isOk())
                .andReturn();
        long testId = idOf(lt);
        mockMvc.perform(get("/api/lab/tests").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        MvcResult order = mockMvc.perform(post("/api/lab/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%d,\"testIds\":[%d]}".formatted(patientId, testId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andReturn();
        long orderId = idOf(order);

        MvcResult detail = mockMvc.perform(get("/api/lab/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long itemId = objectMapper.readTree(detail.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();

        mockMvc.perform(post("/api/lab/orders/" + orderId + "/result")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":%d,\"result\":\"Normal\",\"resultNotes\":\"All good\"}".formatted(itemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/lab/orders/" + orderId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/lab/orders").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        // billing: create, list, partial payment, overpay rejection, settle
        MvcResult invoice = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%d,\"description\":\"Consultation\",\"subtotal\":1000,\"discount\":100,\"tax\":16}".formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(916.0))
                .andExpect(jsonPath("$.data.status").value("UNPAID"))
                .andReturn();
        long invoiceId = idOf(invoice);

        mockMvc.perform(get("/api/invoices").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/invoices/" + invoiceId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.payments.length()").value(0));

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":400,\"method\":\"MOBILE_MONEY\",\"reference\":\"MPESA%s\"}".formatted(suf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(400));

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":600}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":516,\"method\":\"CASH\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/invoices/" + invoiceId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.payments.length()").value(2));
    }

    @Test
    void saleRefundAndTransferFlows() throws Exception {
        String token = loginToken();
        String suf = uniqueSuffix();
        long patientId = createPatient(token, "SalePatient " + suf);
        long medId = createMedicine(token, "SaleMed " + suf);

        // receive stock
        mockMvc.perform(post("/api/inventory/receive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":%d,\"batchNo\":\"B%s\",\"expiryDate\":\"2028-10-31\",\"quantity\":80,\"costPrice\":4.0}".formatted(medId, suf)))
                .andExpect(status().isOk());

        // sale with default selling price
        MvcResult sale = mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":%d,\"items\":[{\"medicineId\":%d,\"quantity\":5}],\"paidAmount\":75}".formatted(patientId, medId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(75.0))
                .andReturn();
        long saleId = idOf(sale);

        mockMvc.perform(get("/api/sales").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/sales/" + saleId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.items[0].refunded").value(false));

        // insufficient stock sale -> 400
        mockMvc.perform(post("/api/sales")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"medicineId\":%d,\"quantity\":999999}]}".formatted(medId)))
                .andExpect(status().isBadRequest());

        // refund restores stock and marks items refunded
        mockMvc.perform(post("/api/sales/" + saleId + "/refund")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Wrong medication\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
        mockMvc.perform(get("/api/sales/" + saleId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.items[0].refunded").value(true));

        // double refund -> 400
        mockMvc.perform(post("/api/sales/" + saleId + "/refund")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Again\"}"))
                .andExpect(status().isBadRequest());

        // ---- transfers ----
        MvcResult branch = mockMvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside\",\"code\":\"RV%s\",\"address\":\"Nairobi\"}".formatted(suf)))
                .andExpect(status().isOk())
                .andReturn();
        long toBranchId = idOf(branch);

        mockMvc.perform(post("/api/inventory/receive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":%d,\"batchNo\":\"X%s\",\"expiryDate\":\"2029-06-30\",\"quantity\":60,\"costPrice\":4.0}".formatted(medId, suf)))
                .andExpect(status().isOk());

        MvcResult tr = mockMvc.perform(post("/api/inventory/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":%d,\"toBranchId\":%d,\"quantity\":20,\"reason\":\"Replenish\"}".formatted(medId, toBranchId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        long transferId = idOf(tr);

        mockMvc.perform(get("/api/inventory/transfers").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(post("/api/inventory/transfers/" + transferId + "/approve").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(post("/api/inventory/transfers/" + transferId + "/ship").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"));

        // switch context to the receiving branch and accept
        MvcResult switched = mockMvc.perform(post("/api/auth/switch-branch/" + toBranchId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String branchToken = objectMapper.readTree(switched.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/inventory/transfers/" + transferId + "/receive")
                        .header("Authorization", "Bearer " + branchToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"));
        mockMvc.perform(get("/api/inventory").header("Authorization", "Bearer " + branchToken)).andExpect(status().isOk());

        // reject flow
        MvcResult tr2 = mockMvc.perform(post("/api/inventory/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineId\":%d,\"toBranchId\":%d,\"quantity\":5,\"reason\":\"Test\"}".formatted(medId, toBranchId)))
                .andExpect(status().isOk())
                .andReturn();
        long transfer2Id = idOf(tr2);
        mockMvc.perform(post("/api/inventory/transfers/" + transfer2Id + "/reject")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No longer needed\"}"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void notificationAuditDashboardFlows() throws Exception {
        String token = loginToken();

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").isNumber());
        mockMvc.perform(post("/api/notifications/read-all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/notifications/999999/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/audit-logs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salesToday").isNumber())
                .andExpect(jsonPath("$.data.revenueToday").isNumber())
                .andExpect(jsonPath("$.data.patientsToday").isNumber())
                .andExpect(jsonPath("$.data.appointmentsToday").isNumber())
                .andExpect(jsonPath("$.data.pendingLabs").isNumber())
                .andExpect(jsonPath("$.data.lowStock").isNumber())
                .andExpect(jsonPath("$.data.expiringSoon").isNumber())
                .andExpect(jsonPath("$.data.pendingTransfers").isNumber());

        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---- helpers ----
    private String loginToken() throws Exception {
        return loginToken("admin", "Admin@123");
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private long idOf(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private List<Long> ids(JsonNode array) {
        List<Long> out = new java.util.ArrayList<>();
        array.forEach(n -> out.add(n.path("id").asLong()));
        return out;
    }

    private String uniqueSuffix() {
        return String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L));
    }

    private long createPatient(String token, String lastName) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"%s\",\"phone\":\"07%s\"}".formatted(lastName, uniqueSuffix().substring(0, 8))))
                .andExpect(status().isOk())
                .andReturn();
        return idOf(res);
    }

    private long createMedicine(String token, String name) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/medicines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"barcode\":\"BC%s\",\"sellingPrice\":15.0,\"costPrice\":6.0}".formatted(name, uniqueSuffix())))
                .andExpect(status().isOk())
                .andReturn();
        return idOf(res);
    }
}