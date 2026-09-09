package com.transportlogistics.app.tracking.domain;

import com.transportlogistics.app.tracking.domain.TrackingModels.Connectivity;
import com.transportlogistics.app.tracking.domain.TrackingModels.Freshness;
import com.transportlogistics.app.tracking.domain.TrackingModels.Observation;
import java.time.Duration;
import java.time.Instant;

public final class TrackingStatusPolicy {
 private TrackingStatusPolicy() {}
 public static Freshness freshness(Observation trusted,Instant latestReceipt,Instant now){if(trusted==null)return Freshness.UNKNOWN;Duration sourceAge=Duration.between(trusted.sourceTimestamp(),now),receiptAge=Duration.between(latestReceipt,now);if(!sourceAge.isNegative()&&sourceAge.compareTo(Duration.ofSeconds(60))<=0&&receiptAge.compareTo(Duration.ofSeconds(60))<=0)return Freshness.LIVE;if(sourceAge.compareTo(Duration.ofMinutes(5))<=0)return Freshness.RECENT;return Freshness.STALE;}
 public static Connectivity connectivity(Instant latestReceipt,Instant now){if(latestReceipt==null)return Connectivity.UNKNOWN;Duration age=Duration.between(latestReceipt,now);return age.compareTo(Duration.ofSeconds(60))<=0?Connectivity.CONNECTED:age.compareTo(Duration.ofMinutes(5))<=0?Connectivity.DEGRADED:Connectivity.OFFLINE;}
}
