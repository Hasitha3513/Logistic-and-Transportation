package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class FuelPurchaseRepositoryIntegrationTest {
 @Autowired FuelPurchaseRepository purchases; @Autowired FuelPriceRepository prices; @Autowired FuelPurchaseHistoryRepository history; @Autowired FuelPurchaseNumberGenerator numbers; @Autowired JdbcTemplate jdbc; @Autowired EntityManager entityManager;
 @Test void migrationPersistsSearchesLocksAndAuditsPurchase(){var r=references();var p=purchase(UUID.randomUUID(),numbers.next(LocalDate.of(2026,8,16)),"INV-1",r);purchases.save(p);history.save(new FuelPurchaseHistory(UUID.randomUUID(),p.id(),null,FuelPurchaseStatus.DRAFT,"CREATED",r.userId,"tester",null,null,null,r.now));var page=purchases.search(new FuelPurchaseUseCase.SearchQuery(0,10,"FP-",null,null,r.vendorId,"DIESEL",FuelPurchaseStatus.DRAFT,null,LocalDate.of(2026,8,1),LocalDate.of(2026,8,31)));assertEquals(1,page.totalElements());assertEquals(p.id(),purchases.findByIdForUpdate(p.id()).orElseThrow().id());assertEquals("CREATED",history.findByPurchaseId(p.id()).getFirst().action());}
 @Test void databaseEnforcesVendorInvoiceAndPurchaseNumberUniqueness(){var r=references();var number=numbers.next(LocalDate.now());purchases.save(purchase(UUID.randomUUID(),number,"INV-1",r));purchases.save(purchase(UUID.randomUUID(),numbers.next(LocalDate.now()),"INV-1",r));assertThrows(RuntimeException.class,entityManager::flush);}
 @Test void detectsOverlappingActiveCataloguePrices(){var r=references();var now=r.now;prices.save(new FuelPrice(UUID.randomUUID(),r.vendorId,"DIESEL",LocalDate.of(2026,1,1),LocalDate.of(2026,6,30),BigDecimal.TEN,"LKR",true,now,now));entityManager.flush();assertTrue(prices.hasOverlappingActivePrice(r.vendorId,"DIESEL",LocalDate.of(2026,6,1),null,null));assertEquals(0,BigDecimal.TEN.compareTo(prices.findEffective(r.vendorId,"DIESEL",LocalDate.of(2026,6,15)).orElseThrow().unitPrice()));}
 private FuelPurchase purchase(UUID id,String number,String invoice,Refs r){return new FuelPurchase(id,number,r.vendorId,null,"DIESEL",LocalDate.of(2026,8,16),invoice,LocalDate.of(2026,8,16),BigDecimal.TEN,BigDecimal.TEN,new BigDecimal("100"),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,new BigDecimal("100"),"LKR",FuelPurchaseStatus.DRAFT,ReconciliationStatus.PENDING,null,null,null,null,null,null,null,null,null,null,null,null,null,"notes",r.userId,r.now,r.now);}
 private Refs references(){var now=OffsetDateTime.parse("2026-08-16T00:00:00Z");var user=UUID.randomUUID();var vendor=UUID.randomUUID();jdbc.update("INSERT INTO app_user (id,username,email,password_hash,first_name,last_name,active,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?)",user,"purchase-"+user,user+"@test.local","x","Fuel","Buyer",true,now,now);jdbc.update("INSERT INTO vendor (id,code,name,active) VALUES (?,?,?,?)",vendor,"V-"+vendor,"Vendor",true);return new Refs(user,vendor,now);}
 private record Refs(UUID userId,UUID vendorId,OffsetDateTime now){}
}
