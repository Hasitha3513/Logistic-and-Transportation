package com.transportlogistics.app.tracking.ports.outbound;
import java.time.Instant;import java.util.*;
public interface RouteDeviationEvaluationJobRepositoryPort {void enqueue(UUID tenantId,UUID positionId,Instant availableAt);List<Job> claim(String owner,int limit,Instant now,Instant leaseUntil);boolean renew(UUID jobId,String owner,Instant leaseUntil);boolean complete(UUID jobId,String owner);boolean retry(UUID jobId,String owner,Instant nextAttempt,String safeErrorCode);record Job(UUID id,UUID tenantId,UUID positionId,String leaseOwner,Instant leaseUntil,int attempts){} }
