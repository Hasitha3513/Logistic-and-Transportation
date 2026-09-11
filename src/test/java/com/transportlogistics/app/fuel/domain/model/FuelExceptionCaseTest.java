package com.transportlogistics.app.fuel.domain.model;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FuelExceptionCaseTest {
 @Test void rejectsAnInvalidLifecycleExpectation(){var now=OffsetDateTime.now();var value=new FuelExceptionCase(UUID.randomUUID(),UUID.randomUUID(),FuelExceptionCase.Category.SUSPECTED_FUEL_LOSS,FuelExceptionCase.Lifecycle.OPEN,FuelExceptionCase.Impact.MEDIUM,"FUEL_ISSUE",UUID.randomUUID(),null,"Unexplained variance requires review",Map.of(),null,null,null,null,null,now,true,FuelExceptionCase.HandoffStatus.NOT_REQUIRED,null,null,UUID.randomUUID(),null,0,now,now);assertThatThrownBy(()->value.requireLifecycle(FuelExceptionCase.Lifecycle.UNDER_REVIEW)).hasMessage("FUEL_EXCEPTION_INVALID_STATE");}
}
