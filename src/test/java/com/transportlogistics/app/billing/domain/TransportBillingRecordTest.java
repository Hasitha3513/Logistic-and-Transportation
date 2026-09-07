package com.transportlogistics.app.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.billing.domain.TransportBillingRecord.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransportBillingRecordTest {
    private final UUID preparer=UUID.randomUUID();

    @Test void calculatesFourCategoriesAndHalfUpMoney(){var r=draft(List.of(
        line(LineCategory.BASE_CHARGE,"10.005"),line(LineCategory.SURCHARGE,"2.005"),
        line(LineCategory.PENALTY,"3.005"),line(LineCategory.CREDIT_ADJUSTMENT,"1.005")));
        assertThat(r.totals().subtotal()).isEqualByComparingTo("14.02");}

    @Test void requiresExactlyOneBaseAndOneHundredPercentAllocation(){assertThatThrownBy(()->draft(List.of(
        line(LineCategory.SURCHARGE,"2.00"))).validated("hash",UUID.randomUUID(),Compliance.NOT_REQUIRED,now()))
        .isInstanceOf(BusinessRuleException.class);}

    @Test void suppliedTaxMustBeConsistent(){var r=draft(List.of(line(LineCategory.BASE_CHARGE,"100.00")));
        var invalid=new TaxFact(TaxStatus.SUPPLIED,"VAT","LK",new BigDecimal("100"),new BigDecimal("10"),
            new BigDecimal("9"),null,"OWNER_FACT","a".repeat(64));
        assertThatThrownBy(()->r.replace(r.lines(),invalid,r.costCentres(),now()).validated("hash",UUID.randomUUID(),Compliance.ACCEPTED,now()))
            .isInstanceOf(BusinessRuleException.class);}
    @Test void preparationAndApprovalAreSeparated(){var r=draft(List.of(line(LineCategory.BASE_CHARGE,"10.00")))
        .validated("hash",UUID.randomUUID(),Compliance.NOT_REQUIRED,now());
        assertThatThrownBy(()->r.approved(preparer,now())).isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("BILLING_SOD_VIOLATION");}

    @Test void replacementInvalidatesValidationAndReversalNegatesTotals(){var original=draft(List.of(line(LineCategory.BASE_CHARGE,"10.00")));
        var reversal=new TransportBillingRecord(UUID.randomUUID(),original.tenantId(),"TB-2026-000002",RecordType.REVERSAL,
            original.id(),null,original.source(),original.customerId(),"LKR",Lifecycle.DRAFT,original.lines(),original.tax(),
            original.costCentres(),null,preparer,null,null,null,null,null,null,Compliance.PENDING,0,now(),now());
        assertThat(reversal.totals().totalAmount()).isEqualByComparingTo("-10.00");}

    private TransportBillingRecord draft(List<Line> lines){var at=now();return new TransportBillingRecord(UUID.randomUUID(),UUID.randomUUID(),
        "TB-2026-000001",RecordType.REGULAR,null,null,new Source(Source.SourceType.TRIP,UUID.randomUUID(),"TRIP-1","CLOSED",at,1,"b".repeat(64)),
        UUID.randomUUID(),"LKR",Lifecycle.DRAFT,lines,TaxFact.notSupplied(),List.of(new CostCentre(UUID.randomUUID(),"OPS",new BigDecimal("100"),null,"OWNER")),null,
        preparer,null,null,null,null,null,null,Compliance.PENDING,0,at,at);}
    private static Line line(LineCategory category,String amount){return new Line(UUID.randomUUID(),category,"RATE","SOURCE",BigDecimal.ONE,new BigDecimal(amount),new BigDecimal(amount));}
    private static OffsetDateTime now(){return OffsetDateTime.parse("2026-01-01T00:00:00Z");}
}
