package com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.response;
import java.util.List;
public record CargoManifestReadinessResponse(boolean ready,List<ManifestValidationFailureResponse> failures) { }
