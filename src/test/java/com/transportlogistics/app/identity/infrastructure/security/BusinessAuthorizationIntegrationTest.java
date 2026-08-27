package com.transportlogistics.app.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.fleet.application.ports.in.DriverUseCase;
import com.transportlogistics.app.fleet.vehiclemaster.ports.inbound.VehicleUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelStationUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import com.transportlogistics.app.freight.order.domain.model.FreightOrder;
import com.transportlogistics.app.freight.order.domain.model.FreightOrderLine;
import com.transportlogistics.app.freight.order.ports.inbound.FreightOrderUseCase;
import com.transportlogistics.app.freight.manifest.domain.model.CargoManifest;
import com.transportlogistics.app.freight.manifest.ports.inbound.CargoManifestUseCase;
import com.transportlogistics.app.organization.application.ports.in.CustomerUseCase;
import com.transportlogistics.app.organization.application.ports.in.DepartmentUseCase;
import com.transportlogistics.app.organization.application.ports.in.LocationUseCase;
import com.transportlogistics.app.organization.application.ports.in.ProjectUseCase;
import com.transportlogistics.app.organization.application.ports.in.VendorUseCase;
import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BusinessAuthorizationIntegrationTest {
    private static final String PASSWORD = "authorization-test-password";
    private static final List<String> TEST_PERMISSIONS = List.of(
            "TRIP_APPROVE", "TRIP_REJECT", "TRIP_DISPATCH", "TRIP_START", "TRIP_COMPLETE",
            "TRIP_ASSIGN_ROUTE",
            "VEHICLE_CREATE", "DRIVER_CREATE", "ROUTE_CREATE", "ROUTE_UPDATE", "REPORT_VIEW", "DASHBOARD_VIEW",
            "CUSTOMER_VIEW", "CUSTOMER_CREATE", "CUSTOMER_UPDATE",
            "DEPARTMENT_VIEW", "DEPARTMENT_CREATE", "DEPARTMENT_UPDATE",
            "LOCATION_VIEW", "LOCATION_CREATE", "LOCATION_UPDATE",
            "PROJECT_VIEW", "PROJECT_CREATE", "PROJECT_UPDATE",
            "FUEL_ISSUE_AUTHORIZE",
            "FUEL_PURCHASE_APPROVE",
            "FUEL_PRICE_VIEW",
            "FREIGHT_ORDER_VIEW", "FREIGHT_ORDER_MANAGE",
            "CARGO_MANIFEST_VIEW", "CARGO_MANIFEST_MANAGE", "CARGO_MANIFEST_FINALIZE",
            "IDENTITY_MANAGE");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;

    @MockBean TripUseCase trips;
    @MockBean VehicleUseCase vehicles;
    @MockBean DriverUseCase drivers;
    @MockBean RouteUseCase routes;
    @MockBean FuelIssueUseCase fuelIssues;
    @MockBean FuelStationUseCase fuelStations;
    @MockBean FuelPurchaseUseCase fuelPurchases;
    @MockBean FreightOrderUseCase freightOrders;
    @MockBean CargoManifestUseCase cargoManifests;
    @MockBean CustomerUseCase customers;
    @MockBean DepartmentUseCase departments;
    @MockBean LocationUseCase locations;
    @MockBean ProjectUseCase projects;
    @MockBean VendorUseCase vendors;

    @BeforeEach
    void seedActors() {
        jdbc.update("DELETE FROM vehicle_meter_reset");
        jdbc.update("DELETE FROM vehicle_reading");
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM app_user_role");
        jdbc.update("DELETE FROM app_role_permission");
        jdbc.update("DELETE FROM app_user");
        jdbc.update("DELETE FROM app_role");

        var permittedRole = UUID.randomUUID();
        var restrictedRole = UUID.randomUUID();
        jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, ?, ?, ?)",
                permittedRole, "MVP_OPERATOR_TEST", "Test actor with explicit business authorities", true);
        jdbc.update("INSERT INTO app_role (id, name, description, active) VALUES (?, ?, ?, ?)",
                restrictedRole, "RESTRICTED_TEST", "Authenticated test actor without business authorities", true);
        TEST_PERMISSIONS.forEach(permission -> jdbc.update(
                "INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, ?)", permittedRole, permission));
        insertUser("permitted", permittedRole);
        insertUser("restricted", restrictedRole);

        var stationId = UUID.fromString("a1000000-0000-0000-0000-000000000001");
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        when(fuelIssues.authorize(any(), any(), eq("permitted"))).thenReturn(new FuelIssue(UUID.randomUUID(),
                "FUEL-2026-000001", UUID.randomUUID(), null, null, "DIESEL", new BigDecimal("10"), null,
                null, stationId, null, null, now, FuelIssueStatus.AUTHORIZED, UUID.randomUUID(), UUID.randomUUID(),
                now, null, now, now));
        when(fuelStations.get(stationId)).thenReturn(new FuelStation(stationId, "AUTH", "Authorization Station",
                FuelStationType.INTERNAL, true, null, null));
        var purchaseId = UUID.fromString("a2000000-0000-0000-0000-000000000001");
        var vendorId = UUID.fromString("a3000000-0000-0000-0000-000000000001");
        when(fuelPurchases.approve(eq(purchaseId), any(), eq("permitted"))).thenReturn(purchase(purchaseId, vendorId));
        when(fuelPurchases.vendor(vendorId)).thenReturn(new FuelPurchaseUseCase.VendorReference(vendorId, "V-1", "Vendor", true));
        var freightOrder = freightOrder();
        when(freightOrders.create(any(), eq("permitted"))).thenReturn(freightOrder);
        when(freightOrders.update(eq(freightOrder.id()), any(), eq("permitted"))).thenReturn(freightOrder);
        when(freightOrders.search(any())).thenReturn(new FreightOrderUseCase.PageResult<>(List.of(freightOrder), 0, 20, 1, 1));
        var cargoManifest = cargoManifest();
        when(cargoManifests.search(any())).thenReturn(new CargoManifestUseCase.PageResult<>(List.of(cargoManifest), 0, 20, 1, 1));
        when(cargoManifests.create(any(), eq("permitted"))).thenReturn(cargoManifest);
        when(cargoManifests.addItem(eq(cargoManifest.id()), any(), eq("permitted"))).thenReturn(cargoManifest);
        when(cargoManifests.finalizeManifest(eq(cargoManifest.id()), eq(0L), eq("permitted"))).thenReturn(cargoManifest);
    }

    @Test
    void unauthenticatedBusinessOperationsReturn401() throws Exception {
        for (var request : protectedOperations()) {
            mvc.perform(request).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void authenticatedActorWithoutAuthorityGets403BeforeDomainMutation() throws Exception {
        var token = login("restricted");
        for (var request : protectedOperations()) {
            mvc.perform(withBearer(request, token)).andExpect(status().isForbidden());
        }

        verifyNoInteractions(trips, vehicles, drivers, routes, fuelIssues, fuelStations, fuelPurchases,
                customers, departments, locations, projects, vendors, freightOrders, cargoManifests);
    }

    @Test
    void explicitlyPermittedActorCanReachEachBusinessOperation() throws Exception {
        var token = login("permitted");
        for (var request : protectedOperations()) {
            mvc.perform(withBearer(request, token)).andExpect(status().is2xxSuccessful());
        }

        verify(trips, times(4)).transition(any(), any(), eq("permitted"));
        verify(trips).dispatch(any(), eq("permitted"), any());
        verify(trips).assignRoute(any(), any(), eq("permitted"));
        verify(vehicles).create(any());
        verify(drivers).create(any());
        verify(routes).create(any(), eq("permitted"));
        verify(routes).optimizeRoute(any());
        verify(routes).applyOptimization(any(), any(), eq("permitted"));
        verify(routes).getRoutePerformance(any(), any(), any());
        verify(customers).create(any());
        verify(customers).update(any(), any());
        verify(customers).list();
        verify(departments).create(any());
        verify(departments).update(any(), any());
        verify(departments).list();
        verify(locations).create(any());
        verify(locations).update(any(), any());
        verify(locations).list();
        verify(projects).create(any());
        verify(projects).update(any(), any());
        verify(projects).list();
        verify(vendors).list(null);
        verify(fuelIssues).authorize(any(), any(), eq("permitted"));
        verify(fuelPurchases).approve(any(), any(), eq("permitted"));
        verify(freightOrders).create(any(), eq("permitted"));
        verify(freightOrders).update(any(), any(), eq("permitted"));
        verify(freightOrders).search(any());
        verify(cargoManifests).search(any());
        verify(cargoManifests).create(any(), eq("permitted"));
        verify(cargoManifests).addItem(any(), any(), eq("permitted"));
        verify(cargoManifests).finalizeManifest(any(), eq(0L), eq("permitted"));
    }

    @Test
    void actuatorDetailsRequireAdministrativeAuthority() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + login("restricted")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + login("permitted")))
                .andExpect(status().isOk());
    }

    private List<RequestBuilder> protectedOperations() {
        var tripId = UUID.randomUUID();
        var categoryId = UUID.randomUUID();
        var typeId = UUID.randomUUID();
        var originId = UUID.randomUUID();
        var destinationId = UUID.randomUUID();
        return List.of(
                post("/trips/{id}/approve", tripId),
                post("/trips/{id}/reject", tripId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Not authorized by operations\"}"),
                post("/trips/{id}/dispatch", tripId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remarks\":\"Ready\"}"),
                post("/trips/{id}/start", tripId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startOdometerKm\":1000}"),
                post("/trips/{id}/complete", tripId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endOdometerKm\":1100,\"completionRemarks\":\"Done\"}"),
                post("/trips/{id}/assign-route", tripId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeId\":\"" + UUID.randomUUID() + "\"}"),
                post("/vehicles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"registrationNumber":"AUTH-VEHICLE","categoryId":"%s","typeId":"%s"}
                        """.formatted(categoryId, typeId)),
                post("/drivers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"employeeNumber":"AUTH-DRIVER","firstName":"Test","lastName":"Driver"}
                        """),
                post("/routes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"AUTH-ROUTE","name":"Authorization Route","originLocationId":"%s",
                         "destinationLocationId":"%s","plannedDistanceKm":10,"estimatedDurationMinutes":30,
                         "stops":[]}
                        """.formatted(originId, destinationId)),
                post("/routes/{id}/optimize", UUID.randomUUID()),
                post("/routes/{id}/apply-optimization", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optimizedStopLocationIds\":[\"" + UUID.randomUUID() + "\",\"" + UUID.randomUUID() + "\"]}"),
                get("/routes/{id}/performance", UUID.randomUUID())
                        .param("from", "2026-01-01T00:00:00Z").param("to", "2026-01-31T00:00:00Z"),
                post("/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"AUTH-CUSTOMER","name":"Authorization Customer"}
                        """),
                put("/customers/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"AUTH-CUSTOMER","name":"Updated Authorization Customer"}
                        """),
                get("/customers"),
                post("/departments").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"AUTH-DEPARTMENT","name":"Authorization Department"}
                        """),
                put("/departments/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"AUTH-DEPARTMENT","name":"Updated Authorization Department"}
                        """),
                get("/departments"),
                post("/locations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"AUTH-LOCATION","name":"Authorization Location"}
                        """),
                put("/locations/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"AUTH-LOCATION","name":"Updated Authorization Location"}
                        """),
                get("/locations"),
                post("/projects").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"AUTH-PROJECT","name":"Authorization Project"}
                        """),
                put("/projects/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"code":"AUTH-PROJECT","name":"Updated Authorization Project"}
                        """),
                get("/projects"),
                get("/vendors"),
                post("/fuel-issues/{id}/authorize", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Approved by fuel manager\"}"),
                post("/fuel-purchases/{id}/approve", UUID.fromString("a2000000-0000-0000-0000-000000000001"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"Approved\"}"),
                get("/v1/freight/orders"),
                post("/v1/freight/orders").contentType(MediaType.APPLICATION_JSON).content(freightCreateJson()),
                patch("/v1/freight/orders/{id}", freightOrder().id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"priority\":\"URGENT\"}"),
                get("/v1/freight/manifests"),
                post("/v1/freight/manifests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freightOrderId\":\"" + freightOrder().id() + "\"}"),
                post("/v1/freight/manifests/{id}/items", cargoManifest().id()).contentType(MediaType.APPLICATION_JSON)
                        .content(manifestItemJson()),
                post("/v1/freight/manifests/{id}/finalize", cargoManifest().id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"),
                get("/reports/trips").param("fromDate", "2026-01-01").param("toDate", "2026-01-31"),
                get("/dashboard/operations"));
    }

    private RequestBuilder withBearer(RequestBuilder request, String token) {
        return servletContext -> {
            var servletRequest = request.buildRequest(servletContext);
            servletRequest.addHeader("Authorization", "Bearer " + token);
            return servletRequest;
        };
    }

    private String login(String username) throws Exception {
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private void insertUser(String username, UUID roleId) {
        var userId = UUID.randomUUID();
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        jdbc.update("""
                INSERT INTO app_user
                    (id, username, email, password_hash, first_name, last_name, phone, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, userId, username, username + "@example.com", passwords.encode(PASSWORD), "Test", "Actor", null,
                true, now, now);
        jdbc.update("INSERT INTO app_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    private com.transportlogistics.app.fuel.domain.model.FuelPurchase purchase(UUID id, UUID vendorId) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new com.transportlogistics.app.fuel.domain.model.FuelPurchase(id, "FP-2026-000001", vendorId,
                null, "DIESEL", now.toLocalDate(), "INV-1", now.toLocalDate(), new BigDecimal("10"),
                new BigDecimal("2"), new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("20.00"), "LKR", com.transportlogistics.app.fuel.domain.model.FuelPurchaseStatus.APPROVED,
                com.transportlogistics.app.fuel.domain.model.ReconciliationStatus.PENDING, null, null, null, null,
                null, null, null, UUID.randomUUID(), now, null, null, null, null, null, UUID.randomUUID(), now, now);
    }

    private FreightOrder freightOrder() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var id = UUID.fromString("b1000000-0000-0000-0000-000000000001");
        return new FreightOrder(id, "FO-2026-000001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                now.plusDays(1), now.plusDays(2), "STANDARD", "NORMAL", null,
                List.of(new FreightOrderLine(UUID.randomUUID(), "Pallets", BigDecimal.ONE)), 0,
                now, now, "permitted", "permitted");
    }

    private String freightCreateJson() {
        return """
                {"customerId":"%s","originLocationId":"%s","destinationLocationId":"%s",
                 "requestedPickupAt":"2026-02-01T08:00:00Z","requestedDeliveryAt":"2026-02-02T08:00:00Z",
                 "serviceLevel":"STANDARD","priority":"NORMAL","lines":[{"description":"Pallets","quantity":2}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private CargoManifest cargoManifest() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new CargoManifest(UUID.fromString("b2000000-0000-0000-0000-000000000001"), "CM-2026-000001",
                freightOrder().id(), freightOrder().orderNumber(), List.of(), 0, now, now,
                "permitted", "permitted", null, null);
    }

    private String manifestItemJson() {
        return """
                {"version":0,"freightOrderLineId":"%s","description":"Cargo","quantity":1,
                 "packingInformation":"Wrapped","commodityClassification":"SUPPLIER.CODE",
                 "customsApplicable":false,"hazardous":false}
                """.formatted(freightOrder().lines().getFirst().id());
    }
}
