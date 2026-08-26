package com.transportlogistics.app.freight.manifest.adapters.inbound.web;

import com.transportlogistics.app.freight.manifest.adapters.inbound.web.controllers.CargoManifestController;
import com.transportlogistics.app.freight.manifest.adapters.inbound.web.mappers.CargoManifestWebMapper;
import com.transportlogistics.app.freight.manifest.domain.model.CargoManifest;
import com.transportlogistics.app.freight.manifest.domain.model.CargoManifestItem;
import com.transportlogistics.app.freight.manifest.domain.model.ManifestValidationFailure;
import com.transportlogistics.app.freight.manifest.ports.inbound.CargoManifestUseCase;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CargoManifestControllerTest {
    CargoManifestUseCase manifests;
    MockMvc mvc;
    UUID id = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    UUID lineId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        manifests = mock(CargoManifestUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new CargoManifestController(manifests, new CargoManifestWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void createsFromSavedOrder() throws Exception {
        when(manifests.create(any(), eq("manager"))).thenReturn(manifest(List.of()));
        mvc.perform(post("/v1/freight/manifests").principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freightOrderId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.manifestNumber").value("CM-2026-000001"))
                .andExpect(jsonPath("$.finalized").value(false));
    }

    @Test
    void mapsExplicitTrueAndFalseFromCreateRequestAndResponse() throws Exception {
        when(manifests.addItem(eq(id), any(), eq("manager")))
                .thenReturn(manifest(List.of(item(true, false))));

        mvc.perform(post("/v1/freight/manifests/{id}/items", id).principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON).content(itemJson("true", "false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].fragile").value(true))
                .andExpect(jsonPath("$.items[0].temperatureSensitive").value(false));

        var command = ArgumentCaptor.forClass(CargoManifestUseCase.ItemCommand.class);
        verify(manifests).addItem(eq(id), command.capture(), eq("manager"));
        assertEquals(Boolean.TRUE, command.getValue().fragile());
        assertEquals(Boolean.FALSE, command.getValue().temperatureSensitive());
    }

    @Test
    void preservesOmittedClassificationsAsUnknown() throws Exception {
        when(manifests.addItem(eq(id), any(), eq("manager")))
                .thenReturn(manifest(List.of(item(null, null))));

        mvc.perform(post("/v1/freight/manifests/{id}/items", id).principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON).content(itemJson(null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].fragile").value((Object) null))
                .andExpect(jsonPath("$.items[0].temperatureSensitive").value((Object) null));

        var command = ArgumentCaptor.forClass(CargoManifestUseCase.ItemCommand.class);
        verify(manifests).addItem(eq(id), command.capture(), eq("manager"));
        assertNull(command.getValue().fragile());
        assertNull(command.getValue().temperatureSensitive());
    }

    @Test
    void mapsUpdatedClassifications() throws Exception {
        when(manifests.updateItem(eq(id), eq(itemId), any(), eq("manager")))
                .thenReturn(manifest(List.of(item(false, true))));

        mvc.perform(patch("/v1/freight/manifests/{id}/items/{itemId}", id, itemId).principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON).content(itemJson("false", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].fragile").value(false))
                .andExpect(jsonPath("$.items[0].temperatureSensitive").value(true));
    }

    @Test
    void mapsClassificationFinalizationFailureThroughStandardEnvelope() throws Exception {
        when(manifests.finalizeManifest(eq(id), eq(0L), eq("manager"))).thenThrow(new BusinessRuleException(
                "SPECIAL_CARGO_CLASSIFICATION_MISSING", "Fragile and temperature-sensitive classifications must both be explicitly provided"));

        mvc.perform(post("/v1/freight/manifests/{id}/finalize", id).principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SPECIAL_CARGO_CLASSIFICATION_MISSING"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("explicitly provided")));
    }

    @Test
    void validatesBeforeMutation() throws Exception {
        mvc.perform(post("/v1/freight/manifests/{id}/items", id).principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"quantity\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(manifests);
    }

    @Test
    void mapsStaleVersionTo409() throws Exception {
        when(manifests.finalizeManifest(eq(id), eq(0L), eq("manager")))
                .thenThrow(new ConflictException("CARGO_MANIFEST_CONCURRENT_UPDATE", "Reload"));
        mvc.perform(post("/v1/freight/manifests/{id}/finalize", id).principal(() -> "manager")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CARGO_MANIFEST_CONCURRENT_UPDATE"));
    }

    @Test
    void exposesStructuredReadiness() throws Exception {
        when(manifests.validate(id)).thenReturn(new CargoManifestUseCase.Readiness(false, List.of(
                new ManifestValidationFailure("SPECIAL_CARGO_CLASSIFICATION_MISSING", "items." + itemId + ".specialCargoClassification", "Classification missing"))));
        mvc.perform(get("/v1/freight/manifests/{id}/readiness", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failures[0].code").value("SPECIAL_CARGO_CLASSIFICATION_MISSING"))
                .andExpect(jsonPath("$.failures[0].field").value(org.hamcrest.Matchers.containsString(itemId.toString())));
    }

    private String itemJson(String fragile, String temperatureSensitive) {
        var classifications = fragile == null ? "" : ",\"fragile\":" + fragile;
        classifications += temperatureSensitive == null ? "" : ",\"temperatureSensitive\":" + temperatureSensitive;
        return "{\"version\":0,\"freightOrderLineId\":\"" + lineId + "\",\"description\":\"Cargo\",\"quantity\":1,"
                + "\"packingInformation\":\"Wrapped\",\"commodityClassification\":\"CODE\",\"customsApplicable\":false,\"hazardous\":false"
                + classifications + "}";
    }

    private CargoManifest manifest(List<CargoManifestItem> items) {
        var now = OffsetDateTime.parse("2026-08-25T00:00:00Z");
        return new CargoManifest(id, "CM-2026-000001", UUID.randomUUID(), "FO-2026-000001", items, 0,
                now, now, "manager", "manager", null, null);
    }

    private CargoManifestItem item(Boolean fragile, Boolean temperatureSensitive) {
        return new CargoManifestItem(itemId, lineId, "Cargo", BigDecimal.ONE, "Wrapped", "CODE",
                false, null, false, null, null, fragile, temperatureSensitive);
    }
}
