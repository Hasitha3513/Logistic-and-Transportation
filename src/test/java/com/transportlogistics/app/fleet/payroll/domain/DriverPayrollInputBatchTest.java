package com.transportlogistics.app.fleet.payroll.domain;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class DriverPayrollInputBatchTest {
 private static final UUID TENANT=UUID.randomUUID(),PREPARER=UUID.randomUUID();
 @Test void calculatesAllFourCategoriesAndAllowsNegativeProvisionalInput(){var lines=List.of(line(DriverPayrollInputLine.Category.TRIP_EARNING,"10.005"),line(DriverPayrollInputLine.Category.ALLOWANCE,"2.004"),line(DriverPayrollInputLine.Category.OVERTIME,"3.335"),line(DriverPayrollInputLine.Category.DEDUCTION,"20.00"));var totals=DriverPayrollInputBatch.Totals.calculate(lines);assertThat(totals.tripEarnings()).isEqualByComparingTo("10.01");assertThat(totals.allowances()).isEqualByComparingTo("2.00");assertThat(totals.overtime()).isEqualByComparingTo("3.34");assertThat(totals.provisionalNetInput()).isEqualByComparingTo("-4.65");}
 @Test void approvalRequiresDifferentActorAndFreezesContent(){var batch=batch().replaceLines(List.of(line(DriverPayrollInputLine.Category.TRIP_EARNING,"10")),now()).validated(List.of(line(DriverPayrollInputLine.Category.TRIP_EARNING,"10").snapshot("a".repeat(64),"worker-1")),"b".repeat(64),UUID.randomUUID(),now());assertThatThrownBy(()->batch.approved(PREPARER,now())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("DRIVER_PAYROLL_SOD_VIOLATION");var approved=batch.approved(UUID.randomUUID(),now());assertThatThrownBy(()->approved.replaceLines(List.of(),now())).isInstanceOf(BusinessRuleException.class);}
 @Test void regularAndCorrectionShapeIsEnforced(){assertThatThrownBy(()->new DriverPayrollInputBatch(UUID.randomUUID(),TENANT,DriverPayrollInputBatch.Type.CORRECTION,null,LocalDate.now(),LocalDate.now().plusDays(1),now(),"LKR",DriverPayrollInputBatch.Lifecycle.DRAFT,List.of(),null,PREPARER,null,null,null,UUID.randomUUID(),null,0,now(),now())).isInstanceOf(IllegalArgumentException.class);}
 private static DriverPayrollInputBatch batch(){return new DriverPayrollInputBatch(UUID.randomUUID(),TENANT,DriverPayrollInputBatch.Type.REGULAR,null,LocalDate.of(2026,9,1),LocalDate.of(2026,10,1),now(),"lkr",DriverPayrollInputBatch.Lifecycle.DRAFT,List.of(),null,PREPARER,null,null,null,UUID.randomUUID(),null,0,now(),now());}
 private static DriverPayrollInputLine line(DriverPayrollInputLine.Category c,String rate){return new DriverPayrollInputLine(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"TRIP-1",c,"TRIP_RATE","source",BigDecimal.ONE,DriverPayrollInputLine.Unit.TRIP,new BigDecimal(rate),null,null,null,null);}
 private static OffsetDateTime now(){return OffsetDateTime.parse("2026-09-07T00:00:00Z");}
}
