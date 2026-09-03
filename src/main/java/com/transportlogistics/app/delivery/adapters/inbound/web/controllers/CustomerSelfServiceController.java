package com.transportlogistics.app.delivery.adapters.inbound.web.controllers;

import com.transportlogistics.app.delivery.adapters.inbound.web.dto.request.*;
import com.transportlogistics.app.delivery.ports.inbound.CustomerSelfServiceUseCase;
import com.transportlogistics.app.shared.domain.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/v1/delivery-self-service")
public class CustomerSelfServiceController {
    private static final String PREFIX = "DeliveryAccess ";
    private final CustomerSelfServiceUseCase selfService;

    public CustomerSelfServiceController(CustomerSelfServiceUseCase selfService) { this.selfService = selfService; }

    @GetMapping
    ResponseEntity<CustomerSelfServiceUseCase.Projection> track(HttpServletRequest request) {
        return response(selfService.track(token(request), sourceIp(request)));
    }
    @GetMapping("/notification-preferences")
    ResponseEntity<CustomerSelfServiceUseCase.Preferences> preferences(HttpServletRequest request) {
        return response(selfService.preferences(token(request), sourceIp(request)));
    }
    @PutMapping("/notification-preferences")
    ResponseEntity<CustomerSelfServiceUseCase.Preferences> replacePreferences(HttpServletRequest request,
            @Valid @RequestBody SelfServicePreferenceRequest body) {
        return response(selfService.replacePreferences(token(request), sourceIp(request),
                new CustomerSelfServiceUseCase.PreferenceCommand(body.emailEnabled(), body.smsEnabled(), body.version())));
    }
    @PostMapping("/issues")
    ResponseEntity<CustomerSelfServiceUseCase.Submission> issue(HttpServletRequest request,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody SelfServiceIssueRequest body) {
        return created(selfService.submitIssue(token(request), sourceIp(request), key,
                new CustomerSelfServiceUseCase.IssueCommand(body.category(), body.description())));
    }
    @PostMapping("/feedback")
    ResponseEntity<CustomerSelfServiceUseCase.Submission> feedback(HttpServletRequest request,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody SelfServiceFeedbackRequest body) {
        return created(selfService.submitFeedback(token(request), sourceIp(request), key,
                new CustomerSelfServiceUseCase.FeedbackCommand(body.rating(), body.comment())));
    }
    @PostMapping("/redelivery-requests")
    ResponseEntity<CustomerSelfServiceUseCase.Submission> deliveryRequest(HttpServletRequest request,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody SelfServiceDeliveryRequest body) {
        return created(selfService.submitRequest(token(request), sourceIp(request), key,
                new CustomerSelfServiceUseCase.RequestCommand(body.preferredStartAt(), body.preferredEndAt(), body.notes())));
    }

    private static String token(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX) || header.length() == PREFIX.length()
                || header.substring(PREFIX.length()).contains(" ")) throw invalid();
        return header.substring(PREFIX.length());
    }
    private static String sourceIp(HttpServletRequest request) { return request.getRemoteAddr(); }
    private static <T> ResponseEntity<T> response(T body) { return headers(HttpStatus.OK).body(body); }
    private static <T> ResponseEntity<T> created(T body) { return headers(HttpStatus.CREATED).body(body); }
    private static <T> ResponseEntity.BodyBuilder headers(HttpStatus status) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).header("Referrer-Policy", "no-referrer");
    }
    private static NotFoundException invalid() { return new NotFoundException("SELF_SERVICE_ACCESS_INVALID", "Self-service access is invalid"); }
}
