package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.*;
import com.transportlogistics.app.fuel.domain.service.FuelPurchasePolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FuelPurchaseServiceTest {
    private final UUID vendorId=UUID.randomUUID(), actorId=UUID.randomUUID();
    private FuelPurchaseRepository purchases; private FuelPurchaseHistoryRepository history; private FuelVendorPort vendors; private FuelEventPublisher events; private FuelPurchaseService service;
    @BeforeEach void setUp(){
        purchases=mock(FuelPurchaseRepository.class);history=mock(FuelPurchaseHistoryRepository.class);var prices=mock(FuelPriceRepository.class);var stations=mock(FuelStationRepository.class);vendors=mock(FuelVendorPort.class);var actors=mock(FuelActorPort.class);var numbers=mock(FuelPurchaseNumberGenerator.class);var tx=mock(FuelTransaction.class);events=mock(FuelEventPublisher.class);
        when(tx.execute(any())).thenAnswer(i->((java.util.function.Supplier<?>)i.getArgument(0)).get());when(actors.find("manager")).thenReturn(Optional.of(new FuelActorPort.Actor(actorId,"manager")));when(vendors.find(vendorId)).thenReturn(Optional.of(new FuelVendorPort.Vendor(vendorId,"V1","Vendor",true)));when(numbers.next(any())).thenReturn("FP-2026-000001");when(prices.findEffective(any(),any(),any())).thenReturn(Optional.of(new FuelPrice(UUID.randomUUID(),vendorId,"DIESEL",LocalDate.of(2026,1,1),null,new BigDecimal("9.50"),"LKR",true,time(),time())));when(purchases.save(any())).thenAnswer(i->i.getArgument(0));when(history.save(any())).thenAnswer(i->i.getArgument(0));
        service=new FuelPurchaseService(purchases,history,prices,stations,vendors,actors,numbers,tx,events,new FuelPurchasePolicy(),Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"),ZoneOffset.UTC));
    }
    @Test void createsDraftWithAuthoritativeTotalsVarianceAndHistory(){var created=service.create(command(),"manager");assertEquals("FP-2026-000001",created.purchaseNumber());assertEquals(new BigDecimal("100.00"),created.subtotal());assertEquals(new BigDecimal("15.00"),created.taxAmount());assertEquals(new BigDecimal("117.00"),created.totalAmount());assertEquals(new BigDecimal("0.50"),created.priceVariance());verify(history).save(argThat(h->h.action().equals("CREATED")));}
    @Test void rejectsMissingInactiveAndDuplicateVendorInvoice(){when(vendors.find(vendorId)).thenReturn(Optional.empty());assertCode("FUEL_VENDOR_NOT_FOUND",()->service.create(command(),"manager"));when(vendors.find(vendorId)).thenReturn(Optional.of(new FuelVendorPort.Vendor(vendorId,"V1","Vendor",false)));assertCode("FUEL_VENDOR_INACTIVE",()->service.create(command(),"manager"));when(vendors.find(vendorId)).thenReturn(Optional.of(new FuelVendorPort.Vendor(vendorId,"V1","Vendor",true)));when(purchases.existsByVendorAndInvoice(any(),any(),any())).thenReturn(true);assertThrows(ConflictException.class,()->service.create(command(),"manager"));}
    @Test void followsLifecycleCalculatesReceiptVarianceAndWritesHistory(){var draft=purchase(FuelPurchaseStatus.DRAFT);var submitted=with(draft,FuelPurchaseStatus.SUBMITTED);var approved=with(draft,FuelPurchaseStatus.APPROVED);var received=with(draft,FuelPurchaseStatus.RECEIVED);when(purchases.findByIdForUpdate(draft.id())).thenReturn(Optional.of(draft),Optional.of(submitted),Optional.of(approved),Optional.of(received));assertEquals(FuelPurchaseStatus.SUBMITTED,service.submit(draft.id(),"manager").status());assertEquals(FuelPurchaseStatus.APPROVED,service.approve(draft.id(),"ok","manager").status());var receipt=service.receive(draft.id(),new FuelPurchaseUseCase.ReceiptCommand(new BigDecimal("9.5"),time(),null,"DN1",null),"manager");assertEquals(new BigDecimal("-0.5000"),receipt.quantityVariance());assertEquals(FuelPurchaseStatus.RECONCILED,service.reconcile(draft.id(),new FuelPurchaseUseCase.ReconciliationCommand("accepted","R1"),"manager").status());verify(history,times(4)).save(any());verify(events,times(3)).publish(any());}
    @Test void rejectsEditingReconciledAndCancellationAfterReceipt(){var p=purchase(FuelPurchaseStatus.RECONCILED);when(purchases.findByIdForUpdate(p.id())).thenReturn(Optional.of(p));assertThrows(ConflictException.class,()->service.update(p.id(),command(),"manager"));var received=with(p,FuelPurchaseStatus.RECEIVED);when(purchases.findByIdForUpdate(p.id())).thenReturn(Optional.of(received));assertThrows(ConflictException.class,()->service.cancel(p.id(),"reason","manager"));}
    private FuelPurchaseUseCase.Command command(){return new FuelPurchaseUseCase.Command(vendorId,null,"diesel",LocalDate.of(2026,8,16),"INV-1",LocalDate.of(2026,8,16),new BigDecimal("10"),new BigDecimal("10"),new BigDecimal("15"),new BigDecimal("2"),"lkr","notes");}
    private FuelPurchase purchase(FuelPurchaseStatus status){var now=time();return new FuelPurchase(UUID.randomUUID(),"FP-2026-000001",vendorId,null,"DIESEL",LocalDate.of(2026,8,16),"INV-1",LocalDate.of(2026,8,16),new BigDecimal("10"),new BigDecimal("10"),new BigDecimal("100.00"),new BigDecimal("15"),new BigDecimal("15.00"),new BigDecimal("2.00"),new BigDecimal("117.00"),"LKR",status,ReconciliationStatus.PENDING,null,null,new BigDecimal("9.50"),new BigDecimal("0.50"),null,null,null,status==FuelPurchaseStatus.APPROVED?actorId:null,status==FuelPurchaseStatus.APPROVED?now:null,null,null,null,null,"notes",actorId,now,now);}
    private FuelPurchase with(FuelPurchase p,FuelPurchaseStatus status){return new FuelPurchase(p.id(),p.purchaseNumber(),p.vendorId(),p.fuelStationId(),p.fuelType(),p.purchaseDate(),p.invoiceNumber(),p.invoiceDate(),p.quantity(),p.unitPrice(),p.subtotal(),p.taxRate(),p.taxAmount(),p.otherCharges(),p.totalAmount(),p.currencyCode(),status,ReconciliationStatus.PENDING,status==FuelPurchaseStatus.RECEIVED?new BigDecimal("9.5"):null,status==FuelPurchaseStatus.RECEIVED?new BigDecimal("-0.5"):null,p.expectedUnitPrice(),p.priceVariance(),null,null,status==FuelPurchaseStatus.RECEIVED?time():null,status==FuelPurchaseStatus.APPROVED?actorId:null,status==FuelPurchaseStatus.APPROVED?time():null,null,null,null,null,p.notes(),p.createdBy(),p.createdAt(),p.updatedAt());}
    private OffsetDateTime time(){return OffsetDateTime.parse("2026-08-16T00:00:00Z");}
    private void assertCode(String code,Runnable action){var ex=assertThrows(BusinessRuleException.class,action::run);assertEquals(code,ex.code());}
}
