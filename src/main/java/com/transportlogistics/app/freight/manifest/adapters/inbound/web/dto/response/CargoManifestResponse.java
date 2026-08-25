package com.transportlogistics.app.freight.manifest.adapters.inbound.web.dto.response;
import java.time.OffsetDateTime; import java.util.*;
public record CargoManifestResponse(UUID id,String manifestNumber,UUID freightOrderId,String freightOrderNumber,boolean finalized,List<CargoManifestItemResponse> items,long version,OffsetDateTime createdAt,OffsetDateTime updatedAt,String createdBy,String updatedBy,OffsetDateTime finalizedAt,String finalizedBy) { }
