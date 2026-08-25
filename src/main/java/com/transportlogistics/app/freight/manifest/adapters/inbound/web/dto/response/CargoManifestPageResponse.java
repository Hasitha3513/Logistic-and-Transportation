package com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.response;
import java.util.List;
public record CargoManifestPageResponse(List<CargoManifestResponse> content,int page,int limit,long totalElements,int totalPages) { }
