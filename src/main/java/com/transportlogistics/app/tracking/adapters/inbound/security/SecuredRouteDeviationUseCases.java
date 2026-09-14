package com.transportlogistics.app.tracking.adapters.inbound.security;

import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationQueryUseCase;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationReviewUseCase;
import com.transportlogistics.app.tracking.ports.inbound.RouteDeviationRuleManagementUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

public class SecuredRouteDeviationUseCases implements RouteDeviationRuleManagementUseCase,
        RouteDeviationReviewUseCase, RouteDeviationQueryUseCase {
    private final RouteDeviationRuleManagementUseCase management;
    private final RouteDeviationReviewUseCase review;
    private final RouteDeviationQueryUseCase query;
    public SecuredRouteDeviationUseCases(RouteDeviationRuleManagementUseCase management,
            RouteDeviationReviewUseCase review, RouteDeviationQueryUseCase query) {
        this.management = management; this.review = review; this.query = query;
    }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    public RouteDeviationRule create(RouteDeviationRuleManagementUseCase.Context c,
            UUID r, String v, BigDecimal t, String k) {
        return management.create(c,r,v,t,k); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    public RouteDeviationRule update(RouteDeviationRuleManagementUseCase.Context c,
            UUID id, long v, BigDecimal t, String k) {
        return management.update(c,id,v,t,k); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    public RouteDeviationRule activate(RouteDeviationRuleManagementUseCase.Context c,
            UUID id, long v, String k) {
        return management.activate(c,id,v,k); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    public RouteDeviationRule disable(RouteDeviationRuleManagementUseCase.Context c,
            UUID id, long v, String r, String k) {
        return management.disable(c,id,v,r,k); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_MANAGE')")
    public RouteDeviationRule retire(RouteDeviationRuleManagementUseCase.Context c,
            UUID id, long v, String r, String k) {
        return management.retire(c,id,v,r,k); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_APPROVE')")
    public RouteDeviationReview approve(RouteDeviationReviewUseCase.Context c, UUID id, long v,
            RouteDeviationReview.Reason r, String n, String k) { return review.approve(c,id,v,r,n,k); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_APPROVE')")
    public RouteDeviationReview reject(RouteDeviationReviewUseCase.Context c, UUID id, long v,
            RouteDeviationReview.Reason r, String n, String k) { return review.reject(c,id,v,r,n,k); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_APPROVE')")
    public RouteDeviationReview correct(RouteDeviationReviewUseCase.Context c, UUID id, long v,
            RouteDeviationReview.Status s, RouteDeviationReview.Reason r, String n, String k) {
        return review.correct(c,id,v,s,r,n,k); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_VIEW')")
    public Optional<RouteDeviationRule> rule(UUID t, UUID id) { return query.rule(t,id); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_VIEW')")
    public Page<RouteDeviationRule> rules(UUID t, RouteDeviationRule.Lifecycle l,int p,int s) {
        return query.rules(t,l,p,s); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_VIEW')")
    public Optional<VehicleRouteDeviationState> state(UUID t,UUID v) { return query.state(t,v); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_VIEW')")
    public Page<VehicleRouteDeviationState> states(UUID t,VehicleRouteDeviationState.State s,int p,int z) {
        return query.states(t,s,p,z); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_EVENT_VIEW')")
    public Optional<RouteDeviationEpisode> episode(UUID t,UUID id) { return query.episode(t,id); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_EVENT_VIEW')")
    public CursorPage<RouteDeviationEpisode> episodes(UUID t,UUID v,UUID trip,UUID route,
            RouteDeviationEpisode.Severity s,Boolean o,Instant f,Instant to,String c,int l) {
        return query.episodes(t,v,trip,route,s,o,f,to,c,l); }
    @Override @PreAuthorize("hasAuthority('ROUTE_DEVIATION_EVENT_VIEW')")
    public List<RouteDeviationReview> reviews(UUID t,UUID e,int l) { return query.reviews(t,e,l); }
}
