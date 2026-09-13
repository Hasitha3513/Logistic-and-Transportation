package com.transportlogistics.app.tracking.domain.routedeviation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RouteDeviationReview(UUID id,UUID tenantId,UUID episodeId,Status status,Reason reason,String note,
                                   UUID reviewerId,Instant reviewedAt,long reviewVersion,UUID compensatesReviewId) {
    public enum Status { NOT_REQUIRED, PENDING, APPROVED, REJECTED }
    public enum Reason { AUTHORIZED_DETOUR, ROAD_CLOSURE, TRAFFIC_DIVERSION, OPERATIONAL_NECESSITY, UNKNOWN }
    public RouteDeviationReview {
        Objects.requireNonNull(id);Objects.requireNonNull(tenantId);Objects.requireNonNull(episodeId);Objects.requireNonNull(status);
        if(reviewVersion<0)throw new RouteDeviationException("STALE_REVIEW_VERSION","Review version cannot be negative");
        if(status==Status.APPROVED||status==Status.REJECTED){Objects.requireNonNull(reason);Objects.requireNonNull(reviewerId);Objects.requireNonNull(reviewedAt);}
        note=note==null?null:note.trim();
        if(note!=null&&note.length()>500)throw new RouteDeviationException("INVALID_REVIEW_NOTE","Review note cannot exceed 500 characters");
        if(reason==Reason.UNKNOWN&&(note==null||note.length()<10))throw new RouteDeviationException("INVALID_REVIEW_NOTE","UNKNOWN requires a 10 to 500 character note");
    }
    public static RouteDeviationReview decide(UUID tenant,UUID episode,Status status,Reason reason,String note,UUID actor,Instant at,long expected){
        if(status!=Status.APPROVED&&status!=Status.REJECTED)throw new RouteDeviationException("INVALID_REVIEW_REASON","Decision must approve or reject");
        return new RouteDeviationReview(UUID.randomUUID(),tenant,episode,status,reason,note,actor,at,expected+1,null);
    }
    public RouteDeviationReview correct(Status newStatus,Reason newReason,String newNote,UUID actor,Instant at,long expected){
        if(expected!=reviewVersion)throw new RouteDeviationException("STALE_REVIEW_VERSION","Review version is stale");
        if(reviewerId.equals(actor)&&newStatus!=status)throw new RouteDeviationException("SELF_REVIEW_REVERSAL_FORBIDDEN","A reviewer cannot reverse their own decision");
        return new RouteDeviationReview(UUID.randomUUID(),tenantId,episodeId,newStatus,newReason,newNote,actor,at,reviewVersion+1,id);
    }
}
