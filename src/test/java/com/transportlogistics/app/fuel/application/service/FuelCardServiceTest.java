package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelCardUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelCardReferencePort;
import com.transportlogistics.app.fuel.application.ports.out.FuelCardRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPerformanceTenantPort;
import com.transportlogistics.app.fuel.domain.model.FuelCard;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FuelCardServiceTest {
    private final FuelCardRepository cards = mock(FuelCardRepository.class);
    private final FuelCardReferencePort references = mock(FuelCardReferencePort.class);
    private final UUID tenant = UUID.randomUUID(); private final UUID actor = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID(); private final UUID provider = UUID.randomUUID();
    private FuelCardService service;

    @BeforeEach void setUp() {
        service = new FuelCardService(cards, references,
                () -> new FuelPerformanceTenantPort.TenantContext(tenant, "Asia/Colombo", "LKR"),
                Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test void createsDraftWithOpaqueReferenceUsedOnlyByPersistenceAndAudits() {
        when(references.providerActive(provider)).thenReturn(true);
        when(cards.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(context(), new FuelCardUseCase.Create(provider, " Card ", "opaque-secret",
                "**** 4242", "4242", 12, 2028));
        assertThat(result.status()).isEqualTo(FuelCard.Status.DRAFT);
        assertThat(result.alias()).isEqualTo("Card");
        verify(cards).audit(eq(tenant), eq(result.id()), isNull(), eq("CREATE"), eq("SUCCESS"), isNull(), eq(actor), any());
    }

    @Test void rejectsDuplicateProviderReference() {
        when(references.providerActive(provider)).thenReturn(true);
        when(cards.referenceExists(tenant, provider, "duplicate")).thenReturn(true);
        assertThatThrownBy(() -> service.create(context(), new FuelCardUseCase.Create(provider, "Card", "duplicate",
                "**** 4242", "4242", 12, 2028))).isInstanceOf(ConflictException.class);
    }

    @Test void activationRequiresBindingAndRestriction() {
        when(cards.find(tenant, cardId)).thenReturn(Optional.of(card(FuelCard.Status.DRAFT, 0)));
        assertThatThrownBy(() -> service.transition(context(), cardId, FuelCard.Status.ACTIVE, 0, null))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("FUEL_CARD_INVALID_STATE");
        verify(cards, never()).save(any());
    }

    @Test void bindingValidatesSameTenantReferenceAndCreatesImmutableHistory() {
        var target = UUID.randomUUID(); when(cards.find(tenant, cardId)).thenReturn(Optional.of(card(FuelCard.Status.DRAFT, 0)));
        when(references.vehicleActive(target)).thenReturn(true);
        var expected = new FuelCardUseCase.Binding(UUID.randomUUID(), "VEHICLE", target, OffsetDateTime.now(), null, "assign");
        when(cards.replaceBinding(eq(tenant), eq(cardId), any(), eq(actor), any())).thenReturn(expected);
        when(cards.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.bind(context(), cardId, new FuelCardUseCase.Bind("VEHICLE", target, 0, "assign"))).isEqualTo(expected);
        ArgumentCaptor<FuelCard> saved = ArgumentCaptor.forClass(FuelCard.class); verify(cards).save(saved.capture());
        assertThat(saved.getValue().version()).isEqualTo(1);
    }

    @Test void restrictionsRequireRealCurrencyPositiveLimitsAndFuelTypes() {
        when(cards.find(tenant, cardId)).thenReturn(Optional.of(card(FuelCard.Status.DRAFT, 0)));
        var invalid = new FuelCardUseCase.Restrict("ZZZ", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, Set.of("DIESEL"), Set.of(), 0, "configure");
        assertThatThrownBy(() -> service.restrict(context(), cardId, invalid)).isInstanceOf(BusinessRuleException.class);
    }

    private FuelCardUseCase.Context context() { return new FuelCardUseCase.Context(tenant, actor); }
    private FuelCard card(FuelCard.Status status, long version) {
        var now = OffsetDateTime.parse("2026-09-04T10:00:00Z");
        return new FuelCard(cardId, tenant, provider, "Card", "opaque", "**** 4242", "4242", 12, 2028,
                status, version, actor, now.minusDays(1), now.minusDays(1));
    }
}
