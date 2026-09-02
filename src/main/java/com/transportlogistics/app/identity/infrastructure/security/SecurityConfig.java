package com.transportlogistics.app.identity.infrastructure.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {
    @Bean
    OpenAPI securityOpenApi() {
        return new OpenAPI().components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwt,
                                            RestAuthenticationEntryPoint authenticationEntryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/health", "/auth/login", "/auth/refresh", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/auth/me", "/auth/logout").authenticated()
                        .requestMatchers("/actuator/**").hasAuthority("IDENTITY_MANAGE")
                        .requestMatchers("/users/**", "/roles/**").hasAuthority("IDENTITY_MANAGE")
                        .requestMatchers(HttpMethod.POST, "/offline-sync/operations").authenticated()

                        .requestMatchers(HttpMethod.GET, "/vehicles/available", "/vehicles/*/availability")
                        .hasAuthority("VEHICLE_AVAILABILITY_VIEW")
                        .requestMatchers(HttpMethod.GET, "/vehicles/*/readings", "/vehicles/*/readings/latest",
                                "/vehicles/*/meter-resets", "/vehicles/*/mileage")
                        .hasAuthority("VEHICLE_READING_VIEW")
                        .requestMatchers(HttpMethod.POST, "/vehicles/*/readings/*/correct")
                        .hasAuthority("VEHICLE_READING_CORRECT")
                        .requestMatchers(HttpMethod.POST, "/vehicles/*/readings")
                        .hasAuthority("VEHICLE_READING_CREATE")
                        .requestMatchers(HttpMethod.POST, "/vehicles/*/meter-resets")
                        .hasAuthority("VEHICLE_READING_RESET_METER")
                        .requestMatchers(HttpMethod.GET, "/vehicles", "/vehicles/*", "/vehicles/*/documents",
                                "/vehicle-categories/**", "/vehicle-types/**")
                        .hasAuthority("VEHICLE_VIEW")
                        .requestMatchers(HttpMethod.POST, "/vehicles", "/vehicle-categories", "/vehicle-types")
                        .hasAuthority("VEHICLE_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/vehicles/*", "/vehicle-categories/*", "/vehicle-types/*")
                        .hasAuthority("VEHICLE_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/vehicles/*", "/vehicle-categories/*", "/vehicle-types/*")
                        .hasAuthority("VEHICLE_STATUS_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/vehicles/*/documents")
                        .hasAuthority("VEHICLE_DOCUMENT_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/vehicles/*/documents/*")
                        .hasAuthority("VEHICLE_DOCUMENT_MANAGE")
                        .requestMatchers(HttpMethod.DELETE, "/vehicles/*/documents/*")
                        .hasAuthority("VEHICLE_DOCUMENT_MANAGE")
                        .requestMatchers(HttpMethod.GET, "/vehicles/*/maintenance-schedules",
                                "/vehicles/*/maintenance-schedules/*")
                        .hasAuthority("VEHICLE_VIEW")
                        .requestMatchers(HttpMethod.POST, "/vehicles/*/maintenance-schedules",
                                "/vehicles/*/maintenance-schedules/*/cancel", "/vehicles/*/maintenance-schedules/*/complete")
                        .hasAuthority("VEHICLE_MAINTENANCE_MANAGE")
                        .requestMatchers(HttpMethod.PUT, "/vehicles/*/maintenance-schedules/*")
                        .hasAuthority("VEHICLE_MAINTENANCE_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/vehicles/*/maintenance-schedules/*")
                        .hasAuthority("VEHICLE_MAINTENANCE_MANAGE")
                        .requestMatchers(HttpMethod.GET, "/vehicles/*/lubricant-logs", "/vehicles/*/lubricant-logs/*")
                        .hasAuthority("LUBRICANT_LOG_VIEW")
                        .requestMatchers(HttpMethod.POST, "/vehicles/*/lubricant-logs")
                        .hasAuthority("LUBRICANT_LOG_MANAGE")

                        .requestMatchers(HttpMethod.GET, "/drivers/available", "/drivers/*/availability")
                        .hasAuthority("DRIVER_AVAILABILITY_VIEW")
                        .requestMatchers(HttpMethod.GET, "/drivers", "/drivers/*", "/drivers/*/licenses",
                                "/drivers/*/exceptions", "/drivers/*/exceptions/*",
                                "/drivers/*/violations", "/drivers/*/violations/*",
                                "/drivers/*/performance")
                        .hasAuthority("DRIVER_VIEW")
                        .requestMatchers(HttpMethod.POST, "/drivers").hasAuthority("DRIVER_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/drivers/*").hasAuthority("DRIVER_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/drivers/*").hasAuthority("DRIVER_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/drivers/*/licenses")
                        .hasAuthority("DRIVER_LICENSE_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/drivers/*/licenses/*")
                        .hasAuthority("DRIVER_LICENSE_MANAGE")
                        .requestMatchers(HttpMethod.DELETE, "/drivers/*/licenses/*")
                        .hasAuthority("DRIVER_LICENSE_MANAGE")
                        .requestMatchers(HttpMethod.POST, "/drivers/*/exceptions",
                                "/drivers/*/exceptions/*/cancel", "/drivers/*/exceptions/*/complete")
                        .hasAuthority("DRIVER_EXCEPTION_MANAGE")
                        .requestMatchers(HttpMethod.PUT, "/drivers/*/exceptions/*")
                        .hasAuthority("DRIVER_EXCEPTION_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/drivers/*/exceptions/*")
                        .hasAuthority("DRIVER_EXCEPTION_MANAGE")
                        .requestMatchers(HttpMethod.POST, "/drivers/*/violations",
                                "/drivers/*/violations/*/pay", "/drivers/*/violations/*/waive",
                                "/drivers/*/violations/*/dispute")
                        .hasAuthority("DRIVER_VIOLATION_MANAGE")
                        .requestMatchers(HttpMethod.GET, "/drivers/*/medical-records", "/drivers/*/medical-records/*")
                        .hasAuthority("DRIVER_MEDICAL_VIEW")
                        .requestMatchers(HttpMethod.POST, "/drivers/*/medical-records")
                        .hasAuthority("DRIVER_MEDICAL_MANAGE")
                        .requestMatchers(HttpMethod.PUT, "/drivers/*/medical-records/*")
                        .hasAuthority("DRIVER_MEDICAL_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/drivers/*/medical-records/*")
                        .hasAuthority("DRIVER_MEDICAL_MANAGE")
                        .requestMatchers(HttpMethod.GET, "/drivers/*/drug-tests", "/drivers/*/drug-tests/*")
                        .hasAuthority("DRIVER_DRUG_TEST_VIEW")
                        .requestMatchers(HttpMethod.POST, "/drivers/*/drug-tests",
                                "/drivers/*/drug-tests/*/sample",
                                "/drivers/*/drug-tests/*/result",
                                "/drivers/*/drug-tests/*/return-to-duty-clear",
                                "/drivers/*/drug-tests/*/cancel")
                        .hasAuthority("DRIVER_DRUG_TEST_MANAGE")

                        .requestMatchers(HttpMethod.GET, "/routes", "/routes/*", "/routes/*/revisions", "/routes/*/revisions/*",
                                "/routes/*/disruptions", "/routes/*/disruptions/*", "/routes/disruptions/active",
                                "/routes/*/performance")
                        .hasAnyAuthority("ROUTE_VIEW", "REPORT_VIEW")
                        .requestMatchers(HttpMethod.POST, "/routes").hasAuthority("ROUTE_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/routes/*").hasAuthority("ROUTE_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/routes/*").hasAuthority("ROUTE_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/routes/*/optimize", "/routes/*/apply-optimization").hasAuthority("ROUTE_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/routes/*/disruptions", "/routes/*/disruptions/*/resolve")
                        .hasAuthority("ROUTE_DISRUPTION_MANAGE")

                        .requestMatchers(HttpMethod.GET, "/customers", "/customers/*")
                        .hasAuthority("CUSTOMER_VIEW")
                        .requestMatchers(HttpMethod.POST, "/customers").hasAuthority("CUSTOMER_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/customers/*").hasAuthority("CUSTOMER_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/customers/*").hasAuthority("CUSTOMER_UPDATE")
                        .requestMatchers(HttpMethod.GET, "/departments", "/departments/*")
                        .hasAuthority("DEPARTMENT_VIEW")
                        .requestMatchers(HttpMethod.POST, "/departments").hasAuthority("DEPARTMENT_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/departments/*").hasAuthority("DEPARTMENT_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/departments/*").hasAuthority("DEPARTMENT_UPDATE")
                        .requestMatchers(HttpMethod.GET, "/locations", "/locations/*")
                        .hasAuthority("LOCATION_VIEW")
                        .requestMatchers(HttpMethod.POST, "/locations").hasAuthority("LOCATION_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/locations/*").hasAuthority("LOCATION_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/locations/*").hasAuthority("LOCATION_UPDATE")
                        .requestMatchers(HttpMethod.GET, "/projects", "/projects/*")
                        .hasAuthority("PROJECT_VIEW")
                        .requestMatchers(HttpMethod.POST, "/projects").hasAuthority("PROJECT_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/projects/*").hasAuthority("PROJECT_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/projects/*").hasAuthority("PROJECT_UPDATE")

                        .requestMatchers(HttpMethod.GET, "/trips/*/fuel-cost")
                        .hasAuthority("FUEL_COST_VIEW")
                        .requestMatchers(HttpMethod.GET, "/trips", "/trips/*", "/trips/*/status-history",
                                "/trips/*/operational-events", "/trips/*/operational-events/*")
                        .hasAnyAuthority("TRIP_VIEW", "TRIP_LOG_VIEW")
                        .requestMatchers(HttpMethod.POST, "/trips").hasAuthority("TRIP_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/trips/*").hasAuthority("TRIP_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/trips/*/submit").hasAuthority("TRIP_SUBMIT")
                        .requestMatchers(HttpMethod.POST, "/trips/*/approve").hasAuthority("TRIP_APPROVE")
                        .requestMatchers(HttpMethod.POST, "/trips/*/reject").hasAuthority("TRIP_REJECT")
                        .requestMatchers(HttpMethod.POST, "/trips/*/assign-vehicle", "/trips/*/unassign-vehicle")
                        .hasAuthority("TRIP_ASSIGN_VEHICLE")
                        .requestMatchers(HttpMethod.POST, "/trips/*/assign-driver", "/trips/*/unassign-driver")
                        .hasAuthority("TRIP_ASSIGN_DRIVER")
                        .requestMatchers(HttpMethod.POST, "/trips/*/assign-route")
                        .hasAuthority("TRIP_ASSIGN_ROUTE")
                        .requestMatchers(HttpMethod.POST, "/trips/*/dispatch").hasAuthority("TRIP_DISPATCH")
                        .requestMatchers(HttpMethod.POST, "/trips/*/start").hasAuthority("TRIP_START")
                        .requestMatchers(HttpMethod.POST, "/trips/*/complete").hasAuthority("TRIP_COMPLETE")
                        .requestMatchers(HttpMethod.POST, "/trips/*/close").hasAuthority("TRIP_CLOSE")
                        .requestMatchers(HttpMethod.POST, "/trips/*/cancel").hasAuthority("TRIP_CANCEL")
                        .requestMatchers(HttpMethod.POST, "/trips/*/checkpoints", "/trips/*/delays", "/trips/*/incidents")
                        .hasAnyAuthority("TRIP_DISPATCH", "TRIP_LOG_MANAGE", "TRIP_UPDATE")

                        .requestMatchers(HttpMethod.GET, "/fuel-issues", "/fuel-issues/*",
                                "/fuel-issues/*/history", "/fuel-stations", "/fuel-stations/*")
                        .hasAuthority("FUEL_ISSUE_VIEW")
                        .requestMatchers(HttpMethod.POST, "/fuel-issues").hasAuthority("FUEL_ISSUE_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/fuel-issues/*").hasAuthority("FUEL_ISSUE_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/fuel-issues/*/submit").hasAuthority("FUEL_ISSUE_SUBMIT")
                        .requestMatchers(HttpMethod.POST, "/fuel-issues/*/authorize").hasAuthority("FUEL_ISSUE_AUTHORIZE")
                        .requestMatchers(HttpMethod.POST, "/fuel-issues/*/issue").hasAuthority("FUEL_ISSUE_ISSUE")
                        .requestMatchers(HttpMethod.POST, "/fuel-issues/*/cancel").hasAuthority("FUEL_ISSUE_CANCEL")
                        .requestMatchers(HttpMethod.POST, "/fuel-stations").hasAuthority("FUEL_ISSUE_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/fuel-stations/*").hasAuthority("FUEL_ISSUE_UPDATE")

                        .requestMatchers(HttpMethod.GET, "/fuel-purchases", "/fuel-purchases/*",
                                "/fuel-purchases/*/history").hasAuthority("FUEL_PURCHASE_VIEW")
                        .requestMatchers(HttpMethod.POST, "/fuel-purchases").hasAuthority("FUEL_PURCHASE_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/fuel-purchases/*").hasAuthority("FUEL_PURCHASE_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/fuel-purchases/*/submit").hasAuthority("FUEL_PURCHASE_SUBMIT")
                        .requestMatchers(HttpMethod.POST, "/fuel-purchases/*/approve").hasAuthority("FUEL_PURCHASE_APPROVE")
                        .requestMatchers(HttpMethod.POST, "/fuel-purchases/*/receive").hasAuthority("FUEL_PURCHASE_RECEIVE")
                        .requestMatchers(HttpMethod.POST, "/fuel-purchases/*/reconcile").hasAuthority("FUEL_PURCHASE_RECONCILE")
                        .requestMatchers(HttpMethod.POST, "/fuel-purchases/*/cancel").hasAuthority("FUEL_PURCHASE_CANCEL")
                        .requestMatchers(HttpMethod.GET, "/fuel-prices", "/vendors", "/vendors/*")
                        .hasAuthority("FUEL_PRICE_VIEW")
                        .requestMatchers(HttpMethod.POST, "/fuel-prices", "/vendors").hasAuthority("FUEL_PRICE_MANAGE")
                        .requestMatchers(HttpMethod.PUT, "/fuel-prices/*", "/vendors/*").hasAuthority("FUEL_PRICE_MANAGE")
                        .requestMatchers(HttpMethod.DELETE, "/vendors/*").hasAuthority("FUEL_PRICE_MANAGE")

                        .requestMatchers(HttpMethod.GET, "/v1/freight/orders", "/v1/freight/orders/*")
                        .hasAuthority("FREIGHT_ORDER_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/freight/orders")
                        .hasAuthority("FREIGHT_ORDER_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/v1/freight/orders/*")
                        .hasAuthority("FREIGHT_ORDER_MANAGE")

                        // Servlet security matching excludes the /api context path at runtime; retain the
                        // literal variant below for controller-level regression and match the effective path here.
                        .requestMatchers(HttpMethod.GET, "/v1/deliveries/*/last-mile-planner")
                        .hasAnyAuthority("DELIVERY_FAIL_VIEW", "DELIVERY_EXCEPTION_VIEW")
                        .requestMatchers(HttpMethod.GET, "/v1/deliveries", "/v1/deliveries/*")
                        .hasAuthority("DELIVERY_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries")
                        .hasAuthority("DELIVERY_CREATE")
                        .requestMatchers(HttpMethod.PATCH, "/v1/deliveries/*")
                        .hasAuthority("DELIVERY_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/validate-readiness")
                        .hasAuthority("DELIVERY_ASSIGN")
                        .requestMatchers(HttpMethod.GET, "/v1/deliveries/*/proof")
                        .hasAnyAuthority("DELIVERY_POD_VIEW", "DELIVERY_POD_CAPTURE")
                        .requestMatchers(HttpMethod.GET, "/v1/deliveries/*/proof/evidence/*/content")
                        .hasAuthority("DELIVERY_POD_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/proof", "/v1/deliveries/*/proof/evidence", "/v1/deliveries/*/proof/finalize")
                        .hasAuthority("DELIVERY_POD_CAPTURE")
                        .requestMatchers(HttpMethod.DELETE, "/v1/deliveries/*/proof/evidence/*")
                        .hasAuthority("DELIVERY_POD_CAPTURE")

                        .requestMatchers(HttpMethod.GET, "/v1/deliveries/*/attempts")
                        .hasAuthority("DELIVERY_FAIL_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/failed-attempt", "/v1/deliveries/*/failed-attempts/*/contacts")
                        .hasAuthority("DELIVERY_FAIL_RECORD")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/escalate")
                        .hasAuthority("DELIVERY_FAIL_ESCALATE")
                        .requestMatchers(HttpMethod.PATCH, "/v1/deliveries/*/escalations/*")
                        .hasAuthority("DELIVERY_FAIL_ESCALATE")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/return-to-base")
                        .hasAuthority("DELIVERY_RETURN_INITIATE")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/redelivery/suggestions")
                        .hasAuthority("DELIVERY_REDELIVERY_VIEW")
                        .requestMatchers(HttpMethod.GET, "/v1/deliveries/*/redelivery/history")
                        .hasAuthority("DELIVERY_REDELIVERY_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/redelivery/schedule", "/v1/deliveries/*/redelivery/reschedule")
                        .hasAuthority("DELIVERY_REDELIVERY_SCHEDULE")
                        .requestMatchers(HttpMethod.GET, "/v1/deliveries/analytics/**")
                        .hasAuthority("DELIVERY_ANALYTICS_VIEW")

                        .requestMatchers(HttpMethod.GET, "/v1/deliveries/*/exceptions", "/v1/deliveries/*/exceptions/*")
                        .hasAuthority("DELIVERY_EXCEPTION_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/exceptions")
                        .hasAuthority("DELIVERY_EXCEPTION_CREATE")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/exceptions/*/investigate")
                        .hasAuthority("DELIVERY_EXCEPTION_MANAGE")
                        .requestMatchers(HttpMethod.POST, "/v1/deliveries/*/exceptions/*/resolve", "/v1/deliveries/*/exceptions/*/cancel")
                        .hasAuthority("DELIVERY_EXCEPTION_RESOLVE")

                        .requestMatchers(HttpMethod.GET, "/v1/delivery-zones", "/v1/delivery-zones/*", "/v1/delivery-zones/resolve")
                        .hasAuthority("DELIVERY_ZONE_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/delivery-zones")
                        .hasAuthority("DELIVERY_ZONE_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/v1/delivery-zones/*")
                        .hasAuthority("DELIVERY_ZONE_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/v1/delivery-zones/*/activate", "/v1/delivery-zones/*/deactivate")
                        .hasAuthority("DELIVERY_ZONE_ACTIVATE")

                        .requestMatchers(HttpMethod.GET, "/api/v1/delivery-slots", "/api/v1/delivery-slots/*", "/api/v1/delivery-slots/available", "/v1/delivery-slots", "/v1/delivery-slots/*", "/v1/delivery-slots/available")
                        .hasAuthority("DELIVERY_SLOT_VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/v1/delivery-slots", "/v1/delivery-slots")
                        .hasAuthority("DELIVERY_SLOT_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/delivery-slots/*", "/v1/delivery-slots/*")
                        .hasAuthority("DELIVERY_SLOT_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/delivery-slots/*/activate", "/api/v1/delivery-slots/*/deactivate", "/api/v1/delivery-slots/*/close", "/v1/delivery-slots/*/activate", "/v1/delivery-slots/*/deactivate", "/v1/delivery-slots/*/close")
                        .hasAuthority("DELIVERY_SLOT_ACTIVATE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/delivery-slots/*/reservations", "/api/v1/delivery-slots/*/reservations/*/release", "/v1/delivery-slots/*/reservations", "/v1/delivery-slots/*/reservations/*/release")
                        .hasAnyAuthority("DELIVERY_SLOT_ASSIGN", "DELIVERY_SLOT_OVERRIDE")

                        .requestMatchers(HttpMethod.POST, "/api/v1/deliveries/orders/*/eta/calculate")
                        .hasAuthority("DELIVERY_UPDATE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/deliveries/*/last-mile-planner")
                        .hasAnyAuthority("DELIVERY_FAIL_VIEW", "DELIVERY_EXCEPTION_VIEW")

                        // These controllers include the application context path in their explicit mapping.
                        // Their method-level @PreAuthorize contracts remain the authoritative action checks.
                        .requestMatchers("/api/v1/delivery-riders/**", "/api/v1/deliveries/batches/**", "/api/v1/deliveries/orders/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.GET, "/bunker-tanks/*/movements").hasAuthority("BUNKER_LEDGER_VIEW")
                        .requestMatchers(HttpMethod.GET, "/bunker-tanks", "/bunker-tanks/*", "/bunker-tanks/*/balance", "/bunker-tanks/*/dip-readings")
                        .hasAuthority("BUNKER_VIEW")
                        .requestMatchers(HttpMethod.POST, "/bunker-tanks").hasAuthority("BUNKER_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/bunker-tanks/*").hasAuthority("BUNKER_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/bunker-tanks/*/opening-balance", "/bunker-tanks/*/adjustments")
                        .hasAuthority("BUNKER_ADJUST")
                        .requestMatchers(HttpMethod.POST, "/bunker-tanks/*/dip-readings").hasAuthority("BUNKER_DIP_RECORD")
                        .requestMatchers(HttpMethod.POST, "/bunker-transfers").hasAuthority("BUNKER_TRANSFER")

                        .requestMatchers(HttpMethod.GET, "/reports/freight/export").hasAuthority("FREIGHT_REPORT_EXPORT")
                        .requestMatchers(HttpMethod.GET, "/reports/freight/**").hasAuthority("FREIGHT_REPORT_VIEW")
                        .requestMatchers(HttpMethod.GET, "/dashboard/**").hasAuthority("DASHBOARD_VIEW")
                        .requestMatchers(HttpMethod.GET, "/reports/**").hasAuthority("REPORT_VIEW")

                        .requestMatchers(HttpMethod.GET, "/v1/freight/manifests", "/v1/freight/manifests/*", "/v1/freight/manifests/*/readiness")
                        .hasAuthority("CARGO_MANIFEST_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/freight/manifests/*/finalize")
                        .hasAuthority("CARGO_MANIFEST_FINALIZE")
                        .requestMatchers(HttpMethod.POST, "/v1/freight/manifests", "/v1/freight/manifests/*/items")
                        .hasAuthority("CARGO_MANIFEST_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/v1/freight/manifests/*", "/v1/freight/manifests/*/items/*")
                        .hasAuthority("CARGO_MANIFEST_MANAGE")

                        .requestMatchers(HttpMethod.GET, "/v1/freight/load-plans", "/v1/freight/load-plans/*")
                        .hasAuthority("LOAD_PLAN_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/freight/load-plans", "/v1/freight/load-plans/*/validate-layout", "/v1/freight/load-plans/*/validate-weight-volume", "/v1/freight/load-plans/*/ready")
                        .hasAuthority("LOAD_PLAN_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/v1/freight/load-plans/*")
                        .hasAuthority("LOAD_PLAN_MANAGE")

                        .requestMatchers(HttpMethod.GET, "/v1/freight/insurance/policies", "/v1/freight/insurance/policies/*",
                                "/v1/freight/insurance/claims", "/v1/freight/insurance/claims/*")
                        .hasAuthority("CARGO_INSURANCE_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/freight/insurance/policies",
                                "/v1/freight/insurance/claims", "/v1/freight/insurance/claims/*/assess",
                                "/v1/freight/insurance/claims/*/approve", "/v1/freight/insurance/claims/*/reject",
                                "/v1/freight/insurance/claims/*/dispute", "/v1/freight/insurance/claims/*/settlements")
                        .hasAuthority("CARGO_INSURANCE_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/v1/freight/insurance/policies/*")
                        .hasAuthority("CARGO_INSURANCE_MANAGE")

                        .requestMatchers(HttpMethod.GET, "/notification-rules", "/notification-rules/*")
                        .hasAuthority("NOTIFICATION_RULE_VIEW")
                        .requestMatchers(HttpMethod.GET, "/notification-rule-executions")
                        .hasAuthority("NOTIFICATION_RULE_VIEW")
                        .requestMatchers(HttpMethod.GET, "/notification-deliveries", "/notification-deliveries/*/attempts")
                        .hasAuthority("NOTIFICATION_RULE_VIEW")
                        .requestMatchers(HttpMethod.GET, "/notification-event-catalogue", "/notification-templates", "/notification-templates/*")
                        .hasAuthority("NOTIFICATION_RULE_VIEW")
                        .requestMatchers(HttpMethod.POST, "/notification-rules")
                        .hasAuthority("NOTIFICATION_RULE_MANAGE")
                        .requestMatchers(HttpMethod.PUT, "/notification-rules/*")
                        .hasAuthority("NOTIFICATION_RULE_MANAGE")
                        .requestMatchers(HttpMethod.PATCH, "/notification-rules/*/enable", "/notification-rules/*/disable")
                        .hasAuthority("NOTIFICATION_RULE_MANAGE")
                        .requestMatchers(HttpMethod.DELETE, "/notification-rules/*")
                        .hasAuthority("NOTIFICATION_RULE_MANAGE")

                        .requestMatchers(HttpMethod.GET, "/notifications", "/notifications/unread-count")
                        .hasAuthority("NOTIFICATION_VIEW")
                        .requestMatchers(HttpMethod.PATCH, "/notifications/*/read", "/notifications/read-all")
                        .hasAuthority("NOTIFICATION_VIEW")

                        .requestMatchers("/e2e/**").hasAuthority("NOTIFICATION_RULE_MANAGE")

                        .requestMatchers(HttpMethod.GET, "/v1/freight/exceptions", "/v1/freight/exceptions/*")
                        .hasAuthority("CARGO_EXCEPTION_VIEW")
                        .requestMatchers(HttpMethod.POST, "/v1/freight/exceptions")
                        .hasAuthority("CARGO_EXCEPTION_MANAGE")
                        .requestMatchers(HttpMethod.POST, "/v1/freight/exceptions/*/hold",
                                "/v1/freight/exceptions/*/escalate",
                                "/v1/freight/exceptions/*/release",
                                "/v1/freight/exceptions/*/reject",
                                "/v1/freight/exceptions/*/resolve")
                        .hasAuthority("CARGO_EXCEPTION_MANAGE")

                        .requestMatchers("/vehicles/**", "/drivers/**", "/vehicle-categories/**", "/vehicle-types/**",
                                "/routes/**", "/customers/**", "/departments/**", "/locations/**", "/projects/**",
                                "/trips/**", "/fuel-issues/**", "/fuel-stations/**", "/v1/freight/orders/**", "/v1/freight/manifests/**",
                                "/v1/freight/load-plans/**", "/v1/freight/exceptions/**",
                                "/fuel-purchases/**", "/fuel-prices/**", "/vendors/**",
                                "/bunker-tanks/**", "/bunker-transfers/**",
                                "/dashboard/**", "/reports/**",
                                "/v1/deliveries/**",
                                "/notification-rules/**", "/notification-rule-executions/**", "/notification-deliveries/**", "/notification-event-catalogue/**", "/notification-templates/**",
                                "/notifications/**", "/offline-sync/**").denyAll()
                        .anyRequest().denyAll())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
