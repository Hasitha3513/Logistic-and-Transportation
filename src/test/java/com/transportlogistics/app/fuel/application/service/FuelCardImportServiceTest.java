package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelCardImportUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelCardUseCase;
import com.transportlogistics.app.fuel.application.ports.out.*;
import com.transportlogistics.app.fuel.domain.model.FuelCard;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FuelCardImportServiceTest {
    private final FuelCardImportParser parser = mock(FuelCardImportParser.class);
    private final FuelCardTransactionRepository transactions = mock(FuelCardTransactionRepository.class);
    private final FuelCardRepository cards = mock(FuelCardRepository.class);
    private final FuelCardReferencePort references = mock(FuelCardReferencePort.class);
    private final FuelTransaction transaction = mock(FuelTransaction.class);
    private final UUID tenant = UUID.randomUUID(); private final UUID importer = UUID.randomUUID();
    private final UUID provider = UUID.randomUUID(); private final UUID cardId = UUID.randomUUID();
    private FuelCardImportService service;

    @BeforeEach void setUp() {
        when(references.providerActive(provider)).thenReturn(true);
        when(transaction.execute(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        service = new FuelCardImportService(parser, transactions, cards, references, transaction,
                () -> new FuelPerformanceTenantPort.TenantContext(tenant, "UTC", "LKR"),
                Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test void sameBatchAndFileAreIdempotentButConflictingBatchFails() {
        var batch = batch("batch-1", "a".repeat(64)); when(parser.parse(any())).thenReturn(parsed(batch.providerBatchId(), batch.fileHash()));
        when(transactions.findBatch(tenant, provider, "batch-1")).thenReturn(Optional.of(batch));
        assertThat(service.importJson(context(), provider, new byte[]{1})).isEqualTo(batch);
        when(transactions.findBatch(tenant, provider, "batch-1")).thenReturn(Optional.of(batch("batch-1", "b".repeat(64))));
        assertThatThrownBy(() -> service.importJson(context(), provider, new byte[]{1})).isInstanceOf(ConflictException.class);
    }

    @Test void conflictingTransactionIdentityFailsWithoutOverwritingProviderFacts() {
        var fact = fact("transaction-1", "PURCHASE", null, "a".repeat(64));
        when(parser.parse(any())).thenReturn(new FuelCardImportParser.ParsedBatch("1", "batch-1", now(), "f".repeat(64), List.of(fact)));
        when(transactions.findProviderTransaction(tenant, provider, "transaction-1")).thenReturn(Optional.of(transactionRecord(importer)));
        when(transactions.providerTransactionHashMatches(tenant, provider, "transaction-1", fact.canonicalHash())).thenReturn(false);
        assertThatThrownBy(() -> service.importJson(context(), provider, new byte[]{1})).isInstanceOf(ConflictException.class);
        verify(transactions, never()).saveTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test void inactiveCardAndRestrictionMismatchAreRetainedForReview() {
        var fact = fact("transaction-1", "PURCHASE", null, "a".repeat(64));
        when(parser.parse(any())).thenReturn(new FuelCardImportParser.ParsedBatch("1", "batch-1", now(), "f".repeat(64), List.of(fact)));
        when(cards.findByProviderReference(tenant, provider, "opaque")).thenReturn(Optional.of(card(FuelCard.Status.BLOCKED)));
        when(cards.restriction(tenant, cardId)).thenReturn(Optional.of(new FuelCardUseCase.Restriction("LKR",
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ONE,
                Set.of("PETROL"), Set.of("OTHER"), 0)));
        when(transactions.totals(any(), any(), any(), any())).thenReturn(new FuelCardTransactionRepository.Totals(BigDecimal.ZERO, BigDecimal.ZERO));
        when(transactions.saveBatch(any(), eq(tenant))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactions.saveTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(transactionRecord(importer));
        service.importJson(context(), provider, new byte[]{1});
        verify(transactions).saveTransaction(eq(fact), eq(tenant), any(), eq(provider), eq(cardId), eq(importer),
                eq("REVIEW_REQUIRED"), argThat(codes -> codes.containsAll(Set.of("CARD_INACTIVE", "LIMIT_EXCEEDED", "FUEL_TYPE_NOT_ALLOWED", "STATION_NOT_ALLOWED"))), any());
        verify(cards).audit(eq(tenant), eq(cardId), any(), eq("TRANSACTION_IMPORTED"), eq("REVIEW_REQUIRED"),
                isNull(), eq(importer), any());
    }

    @Test void importerCannotReconcileOwnImmutableTransaction() {
        var current = transactionRecord(importer); when(transactions.transaction(tenant, current.id())).thenReturn(Optional.of(current));
        assertThatThrownBy(() -> service.reconcile(context(), current.id(),
                new FuelCardImportUseCase.Action(UUID.randomUUID(), 0, "match", "MATCH")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("FUEL_CARD_RECONCILIATION_INVALID");
    }

    @Test void independentActorCanMatchAnExistingFuelPurchase() {
        var reconciler = UUID.randomUUID(); var current = transactionRecord(importer); var purchase = UUID.randomUUID();
        when(transactions.transaction(tenant, current.id())).thenReturn(Optional.of(current));
        when(references.purchaseExists(purchase)).thenReturn(true);
        when(transactions.reconcile(eq(tenant), eq(current.id()), eq(reconciler), any(), any())).thenReturn(current);
        assertThat(service.reconcile(new FuelCardImportUseCase.Context(tenant, reconciler), current.id(),
                new FuelCardImportUseCase.Action(purchase, 0, "matched", "MATCH"))).isEqualTo(current);
        verify(cards).audit(eq(tenant), eq(cardId), eq(current.id()), eq("RECONCILIATION_MATCH"),
                eq("SUCCESS"), eq("matched"), eq(reconciler), any());
    }

    private FuelCardImportUseCase.Context context() { return new FuelCardImportUseCase.Context(tenant, importer); }
    private OffsetDateTime now() { return OffsetDateTime.parse("2026-09-04T10:00:00Z"); }
    private FuelCardImportParser.ParsedBatch parsed(String id, String hash) { return new FuelCardImportParser.ParsedBatch("1", id, now(), hash, List.of()); }
    private FuelCardImportUseCase.Batch batch(String id, String hash) { return new FuelCardImportUseCase.Batch(UUID.randomUUID(), provider, id, hash, now(), 1, 1, 0, importer, now()); }
    private FuelCard card(FuelCard.Status status) { return new FuelCard(cardId, tenant, provider, "Card", "opaque", "**** 4242", "4242", 12, 2028, status, 0, importer, now(), now()); }
    private FuelCardImportParser.ParsedTransaction fact(String id, String kind, String original, String hash) {
        return new FuelCardImportParser.ParsedTransaction(id, "opaque", hash, kind, original, now(), now(), "STATION",
                "DIESEL", new BigDecimal("10"), new BigDecimal("30"), new BigDecimal("300"), "LKR",
                null, null, null, "POSTED");
    }
    private FuelCardImportUseCase.Transaction transactionRecord(UUID importedBy) {
        return new FuelCardImportUseCase.Transaction(UUID.randomUUID(), UUID.randomUUID(), provider, cardId, "transaction-1",
                "PURCHASE", null, now(), now(), "STATION", "DIESEL", BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, "LKR", null, "POSTED", "IMPORTED", null, Set.of(), importedBy, 0, now());
    }
}
