package com.transportlogistics.app.fuel.infrastructure.adapters.out;

import com.transportlogistics.app.fuel.application.ports.in.BunkerTankUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelCardImportUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPriceUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelExceptionCorrectionExecutor;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.fleet.FuelExceptionReadingAccess;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
class FuelOwnerCorrectionAdapter implements FuelExceptionCorrectionExecutor {
 private final BunkerTankUseCase bunkers;private final FuelIssueUseCase issues;private final FuelPurchaseUseCase purchases;private final FuelPriceUseCase prices;private final FuelCardImportUseCase cards;private final FuelExceptionReadingAccess readings;
 FuelOwnerCorrectionAdapter(@Lazy BunkerTankUseCase bunkers,FuelIssueUseCase issues,FuelPurchaseUseCase purchases,FuelPriceUseCase prices,FuelCardImportUseCase cards,FuelExceptionReadingAccess readings){this.bunkers=bunkers;this.issues=issues;this.purchases=purchases;this.prices=prices;this.cards=cards;this.readings=readings;}
 @Override public String execute(UUID tenantId,String type,Map<String,String> c,UUID actorId,String actor){return switch(type){case "BUNKER_STOCK_ADJUSTMENT"->{var r=bunkers.adjustStock(uuid(c,"tankId"),decimal(c,"quantityDeltaLiters"),required(c,"reason"),optionalUuid(c,"sourceDipReadingId"),actor);yield r.id().toString();}case "FUEL_ISSUE_CANCEL"->issues.cancel(uuid(c,"fuelIssueId"),required(c,"reason"),actor).id().toString();case "FUEL_PURCHASE_RECONCILE"->purchases.reconcile(uuid(c,"fuelPurchaseId"),new FuelPurchaseUseCase.ReconciliationCommand(required(c,"notes"),required(c,"referenceNumber")),actor).id().toString();case "FUEL_PURCHASE_CANCEL"->purchases.cancel(uuid(c,"fuelPurchaseId"),required(c,"reason"),actor).id().toString();case "FUEL_PRICE_EFFECTIVE_DATED"->{var r=prices.create(new FuelPriceUseCase.Command(uuid(c,"vendorId"),required(c,"fuelType"),LocalDate.parse(required(c,"effectiveFrom")),c.get("effectiveTo")==null?null:LocalDate.parse(c.get("effectiveTo")),decimal(c,"unitPrice"),required(c,"currencyCode"),true));yield r.id().toString();}case "VEHICLE_READING_CORRECTION"->readings.correct(uuid(c,"vehicleId"),uuid(c,"readingId"),decimal(c,"correctedValue"),required(c,"reason"),java.time.OffsetDateTime.parse(required(c,"recordedAt")),actorId).toString();case "FUEL_CARD_MATCH","FUEL_CARD_UNMATCH","FUEL_CARD_REJECT"->{String action=type.substring("FUEL_CARD_".length());var r=cards.reconcile(new FuelCardImportUseCase.Context(tenantId,actorId),uuid(c,"transactionId"),new FuelCardImportUseCase.Action(optionalUuid(c,"purchaseId"),Long.parseLong(required(c,"version")),required(c,"reason"),action));yield r.id().toString();}default->throw new BusinessRuleException("FUEL_EXCEPTION_CORRECTION_INVALID","Unsupported owner correction type");};}
 private static String required(Map<String,String> c,String k){String v=c.get(k);if(v==null||v.isBlank())throw new BusinessRuleException("FUEL_EXCEPTION_CORRECTION_INVALID",k+" is required");return v.trim();}private static UUID uuid(Map<String,String> c,String k){return UUID.fromString(required(c,k));}private static UUID optionalUuid(Map<String,String> c,String k){return c.get(k)==null?null:UUID.fromString(c.get(k));}private static BigDecimal decimal(Map<String,String> c,String k){return new BigDecimal(required(c,k));}
}
