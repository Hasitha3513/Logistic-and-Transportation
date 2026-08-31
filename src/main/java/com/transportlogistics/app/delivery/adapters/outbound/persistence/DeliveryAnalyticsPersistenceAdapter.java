package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.DeliveryReportingQuery.*;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryAnalyticsPersistencePort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Component
public class DeliveryAnalyticsPersistenceAdapter implements DeliveryAnalyticsPersistencePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DeliveryAnalyticsPersistenceAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DeliveryAnalyticsSummary querySummary(UUID tenantId, OffsetDateTime from, OffsetDateTime to,
                                                 String serviceType, String priority, UUID destinationLocationId) {
        var params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("from", Timestamp.from(from.toInstant()))
                .addValue("to", Timestamp.from(to.toInstant()));

        StringBuilder filterSql = new StringBuilder();
        if (serviceType != null && !serviceType.isBlank()) {
            filterSql.append(" AND d.service_type = :serviceType");
            params.addValue("serviceType", serviceType.toUpperCase());
        }
        if (priority != null && !priority.isBlank()) {
            filterSql.append(" AND d.priority = :priority");
            params.addValue("priority", priority.toUpperCase());
        }
        if (destinationLocationId != null) {
            filterSql.append(" AND d.destination_location_id = :destinationLocationId");
            params.addValue("destinationLocationId", destinationLocationId);
        }

        // 1. Query delivery order counts and timing facts
        String orderSql = """
            SELECT 
                COUNT(*) AS total_orders,
                COUNT(*) FILTER (WHERE d.status IN ('DRAFT', 'READY_FOR_ASSIGNMENT', 'FAILED_ATTEMPT', 'ESCALATED')) AS active_orders,
                COUNT(*) FILTER (WHERE d.status IN ('DELIVERED', 'RETURN_TO_BASE')) AS terminal_completed_orders,
                COUNT(*) FILTER (WHERE d.status = 'DELIVERED') AS delivered_orders,
                COUNT(*) FILTER (WHERE d.status = 'RETURN_TO_BASE') AS returned_to_base_orders,
                COUNT(*) FILTER (WHERE d.status = 'DELIVERED' AND COALESCE(att.failed_attempts, 0) = 0) AS first_attempt_success_orders,
                COUNT(*) FILTER (
                    WHERE d.status = 'DELIVERED' AND pod.accepted_at IS NOT NULL AND pod.accepted_at <= COALESCE(rs.scheduled_end_time, d.window_end)
                ) AS on_time_delivered_orders,
                COUNT(*) FILTER (
                    WHERE d.status = 'DELIVERED' AND pod.accepted_at IS NOT NULL AND pod.accepted_at > COALESCE(rs.scheduled_end_time, d.window_end)
                ) AS late_delivered_orders,
                COALESCE(SUM(
                    CASE 
                        WHEN d.status = 'DELIVERED' AND pod.accepted_at IS NOT NULL AND pod.accepted_at > COALESCE(rs.scheduled_end_time, d.window_end)
                        THEN EXTRACT(EPOCH FROM (pod.accepted_at - COALESCE(rs.scheduled_end_time, d.window_end))) / 60.0
                        ELSE 0
                    END
                ), 0) AS total_delay_minutes,
                COUNT(*) FILTER (WHERE rs.id IS NOT NULL) AS redelivered_orders,
                COUNT(*) FILTER (WHERE rs.id IS NOT NULL AND d.status = 'DELIVERED') AS redelivered_successful_orders,
                COUNT(*) FILTER (WHERE rs.id IS NOT NULL AND d.status IN ('DELIVERED', 'RETURN_TO_BASE')) AS redelivered_terminal_orders
            FROM delivery_order d
            LEFT JOIN proof_of_delivery pod 
                ON pod.delivery_order_id = d.id AND pod.tenant_id = d.tenant_id AND pod.status = 'FINALIZED'
            LEFT JOIN (
                SELECT delivery_id, COUNT(*) AS failed_attempts 
                FROM delivery_attempt 
                WHERE tenant_id = :tenantId 
                GROUP BY delivery_id
            ) att ON att.delivery_id = d.id
            LEFT JOIN (
                SELECT DISTINCT ON (delivery_order_id) id, delivery_order_id, scheduled_end_time
                FROM delivery_redelivery_schedule
                WHERE tenant_id = :tenantId AND status = 'CONFIRMED'
                ORDER BY delivery_order_id, scheduled_at DESC
            ) rs ON rs.delivery_order_id = d.id
            WHERE d.tenant_id = :tenantId 
              AND d.created_at >= :from 
              AND d.created_at < :to
            """ + filterSql;

        var orderMetrics = jdbcTemplate.queryForMap(orderSql, params);

        long totalOrders = ((Number) orderMetrics.get("total_orders")).longValue();
        long activeOrders = ((Number) orderMetrics.get("active_orders")).longValue();
        long terminalCompletedOrders = ((Number) orderMetrics.get("terminal_completed_orders")).longValue();
        long deliveredOrders = ((Number) orderMetrics.get("delivered_orders")).longValue();
        long returnedToBaseOrders = ((Number) orderMetrics.get("returned_to_base_orders")).longValue();
        long firstAttemptSuccessOrders = ((Number) orderMetrics.get("first_attempt_success_orders")).longValue();
        long onTimeDeliveredOrders = ((Number) orderMetrics.get("on_time_delivered_orders")).longValue();
        long lateDeliveredOrders = ((Number) orderMetrics.get("late_delivered_orders")).longValue();
        double totalDelayMinutes = ((Number) orderMetrics.get("total_delay_minutes")).doubleValue();
        long redeliveredOrders = ((Number) orderMetrics.get("redelivered_orders")).longValue();
        long redeliveredSuccessfulOrders = ((Number) orderMetrics.get("redelivered_successful_orders")).longValue();
        long redeliveredTerminalOrders = ((Number) orderMetrics.get("redelivered_terminal_orders")).longValue();

        // 2. Query total failed attempts in period
        String attemptSql = """
            SELECT COUNT(*) AS total_failed_attempts
            FROM delivery_attempt da
            JOIN delivery_order d ON d.id = da.delivery_id AND d.tenant_id = da.tenant_id
            WHERE da.tenant_id = :tenantId
              AND da.attempt_timestamp >= :from
              AND da.attempt_timestamp < :to
            """ + filterSql;

        Long totalFailedAttemptsObj = jdbcTemplate.queryForObject(attemptSql, params, Long.class);
        long totalFailedAttempts = totalFailedAttemptsObj != null ? totalFailedAttemptsObj : 0L;

        // 3. Compute rates & averages
        BigDecimal orderSuccessRate = terminalCompletedOrders > 0
                ? BigDecimal.valueOf(deliveredOrders * 100.0 / terminalCompletedOrders).setScale(2, RoundingMode.HALF_UP)
                : null;

        BigDecimal firstAttemptSuccessRate = deliveredOrders > 0
                ? BigDecimal.valueOf(firstAttemptSuccessOrders * 100.0 / deliveredOrders).setScale(2, RoundingMode.HALF_UP)
                : null;

        BigDecimal onTimeDeliveryRate = deliveredOrders > 0
                ? BigDecimal.valueOf(onTimeDeliveredOrders * 100.0 / deliveredOrders).setScale(2, RoundingMode.HALF_UP)
                : null;

        BigDecimal lateDeliveryRate = deliveredOrders > 0
                ? BigDecimal.valueOf(lateDeliveredOrders * 100.0 / deliveredOrders).setScale(2, RoundingMode.HALF_UP)
                : null;

        BigDecimal averageDelayMinutes = lateDeliveredOrders > 0
                ? BigDecimal.valueOf(totalDelayMinutes / lateDeliveredOrders).setScale(1, RoundingMode.HALF_UP)
                : null;

        BigDecimal averageFailedAttemptsPerOrder = totalOrders > 0
                ? BigDecimal.valueOf((double) totalFailedAttempts / totalOrders).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal redeliveryRate = totalOrders > 0
                ? BigDecimal.valueOf(redeliveredOrders * 100.0 / totalOrders).setScale(2, RoundingMode.HALF_UP)
                : null;

        BigDecimal redeliverySuccessRate = redeliveredTerminalOrders > 0
                ? BigDecimal.valueOf(redeliveredSuccessfulOrders * 100.0 / redeliveredTerminalOrders).setScale(2, RoundingMode.HALF_UP)
                : null;

        BigDecimal returnToBaseRate = terminalCompletedOrders > 0
                ? BigDecimal.valueOf(returnedToBaseOrders * 100.0 / terminalCompletedOrders).setScale(2, RoundingMode.HALF_UP)
                : null;

        return new DeliveryAnalyticsSummary(
                null,
                totalOrders,
                activeOrders,
                terminalCompletedOrders,
                deliveredOrders,
                returnedToBaseOrders,
                orderSuccessRate,
                firstAttemptSuccessRate,
                onTimeDeliveredOrders,
                lateDeliveredOrders,
                onTimeDeliveryRate,
                lateDeliveryRate,
                averageDelayMinutes,
                totalFailedAttempts,
                averageFailedAttemptsPerOrder,
                redeliveredOrders,
                redeliveryRate,
                redeliverySuccessRate,
                returnToBaseRate
        );
    }

    @Override
    public List<FailureReasonBreakdownItem> queryFailureBreakdown(UUID tenantId, OffsetDateTime from, OffsetDateTime to,
                                                                 String serviceType, String priority, UUID destinationLocationId) {
        var params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("from", Timestamp.from(from.toInstant()))
                .addValue("to", Timestamp.from(to.toInstant()));

        StringBuilder filterSql = new StringBuilder();
        if (serviceType != null && !serviceType.isBlank()) {
            filterSql.append(" AND d.service_type = :serviceType");
            params.addValue("serviceType", serviceType.toUpperCase());
        }
        if (priority != null && !priority.isBlank()) {
            filterSql.append(" AND d.priority = :priority");
            params.addValue("priority", priority.toUpperCase());
        }
        if (destinationLocationId != null) {
            filterSql.append(" AND d.destination_location_id = :destinationLocationId");
            params.addValue("destinationLocationId", destinationLocationId);
        }

        String sql = """
            WITH total_count AS (
                SELECT COUNT(*) AS total_failed
                FROM delivery_attempt da
                JOIN delivery_order d ON d.id = da.delivery_id AND d.tenant_id = da.tenant_id
                WHERE da.tenant_id = :tenantId
                  AND da.attempt_timestamp >= :from
                  AND da.attempt_timestamp < :to
                  """ + filterSql + """
            )
            SELECT 
                da.failure_reason,
                COUNT(*) AS reason_count,
                COUNT(*) FILTER (WHERE da.disposition = 'REDELIVERY_ELIGIBLE') AS redelivery_eligible_count,
                COUNT(*) FILTER (WHERE da.disposition = 'RETURN_TO_BASE_REQUIRED') AS return_to_base_count,
                COUNT(*) FILTER (WHERE da.disposition = 'ESCALATED') AS escalated_count,
                COALESCE((SELECT total_failed FROM total_count), 0) AS total_failed
            FROM delivery_attempt da
            JOIN delivery_order d ON d.id = da.delivery_id AND d.tenant_id = da.tenant_id
            WHERE da.tenant_id = :tenantId
              AND da.attempt_timestamp >= :from
              AND da.attempt_timestamp < :to
              """ + filterSql + """
            GROUP BY da.failure_reason
            ORDER BY reason_count DESC
            """;

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String reason = rs.getString("failure_reason");
            long count = rs.getLong("reason_count");
            long totalFailed = rs.getLong("total_failed");
            long redeliveryEligible = rs.getLong("redelivery_eligible_count");
            long returnToBase = rs.getLong("return_to_base_count");
            long escalated = rs.getLong("escalated_count");

            BigDecimal percentage = totalFailed > 0
                    ? BigDecimal.valueOf(count * 100.0 / totalFailed).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            return new FailureReasonBreakdownItem(
                    reason,
                    count,
                    percentage,
                    redeliveryEligible,
                    returnToBase,
                    escalated
            );
        });
    }

    @Override
    public List<RawRegionalPerformance> queryRegionalPerformance(UUID tenantId, OffsetDateTime from, OffsetDateTime to,
                                                                 String serviceType, String priority, UUID destinationLocationId) {
        var params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("from", Timestamp.from(from.toInstant()))
                .addValue("to", Timestamp.from(to.toInstant()));

        StringBuilder filterSql = new StringBuilder();
        if (serviceType != null && !serviceType.isBlank()) {
            filterSql.append(" AND d.service_type = :serviceType");
            params.addValue("serviceType", serviceType.toUpperCase());
        }
        if (priority != null && !priority.isBlank()) {
            filterSql.append(" AND d.priority = :priority");
            params.addValue("priority", priority.toUpperCase());
        }
        if (destinationLocationId != null) {
            filterSql.append(" AND d.destination_location_id = :destinationLocationId");
            params.addValue("destinationLocationId", destinationLocationId);
        }

        String sql = """
            SELECT 
                d.destination_location_id,
                COUNT(*) AS total_orders,
                COUNT(*) FILTER (WHERE d.status = 'DELIVERED') AS delivered_orders,
                COUNT(*) FILTER (WHERE d.status = 'RETURN_TO_BASE') AS returned_to_base_orders,
                COUNT(*) FILTER (
                    WHERE d.status = 'DELIVERED' AND pod.accepted_at IS NOT NULL AND pod.accepted_at <= COALESCE(rs.scheduled_end_time, d.window_end)
                ) AS on_time_delivered_orders,
                COUNT(*) FILTER (
                    WHERE d.status = 'DELIVERED' AND pod.accepted_at IS NOT NULL AND pod.accepted_at > COALESCE(rs.scheduled_end_time, d.window_end)
                ) AS late_delivered_orders,
                COALESCE(SUM(
                    CASE 
                        WHEN d.status = 'DELIVERED' AND pod.accepted_at IS NOT NULL AND pod.accepted_at > COALESCE(rs.scheduled_end_time, d.window_end)
                        THEN EXTRACT(EPOCH FROM (pod.accepted_at - COALESCE(rs.scheduled_end_time, d.window_end))) / 60.0
                        ELSE 0
                    END
                ), 0) AS total_delay_minutes,
                COALESCE(SUM(att.failed_attempts), 0) AS failed_attempt_count
            FROM delivery_order d
            LEFT JOIN proof_of_delivery pod 
                ON pod.delivery_order_id = d.id AND pod.tenant_id = d.tenant_id AND pod.status = 'FINALIZED'
            LEFT JOIN (
                SELECT delivery_id, COUNT(*) AS failed_attempts 
                FROM delivery_attempt 
                WHERE tenant_id = :tenantId 
                GROUP BY delivery_id
            ) att ON att.delivery_id = d.id
            LEFT JOIN (
                SELECT DISTINCT ON (delivery_order_id) id, delivery_order_id, scheduled_end_time
                FROM delivery_redelivery_schedule
                WHERE tenant_id = :tenantId AND status = 'CONFIRMED'
                ORDER BY delivery_order_id, scheduled_at DESC
            ) rs ON rs.delivery_order_id = d.id
            WHERE d.tenant_id = :tenantId 
              AND d.created_at >= :from 
              AND d.created_at < :to
              """ + filterSql + """
            GROUP BY d.destination_location_id
            ORDER BY total_orders DESC
            """;

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            UUID locId = rs.getObject("destination_location_id", UUID.class);
            long total = rs.getLong("total_orders");
            long delivered = rs.getLong("delivered_orders");
            long returned = rs.getLong("returned_to_base_orders");
            long onTime = rs.getLong("on_time_delivered_orders");
            long late = rs.getLong("late_delivered_orders");
            long totalDelay = Math.round(rs.getDouble("total_delay_minutes"));
            long failedCount = rs.getLong("failed_attempt_count");

            return new RawRegionalPerformance(
                    locId,
                    total,
                    delivered,
                    returned,
                    onTime,
                    totalDelay,
                    late,
                    failedCount
            );
        });
    }

    @Override
    public List<DeliveryTrendItem> queryTrends(UUID tenantId, OffsetDateTime from, OffsetDateTime to,
                                               String serviceType, String priority, UUID destinationLocationId,
                                               TrendGranularity granularity, ZoneId zoneId) {
        var params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("from", Timestamp.from(from.toInstant()))
                .addValue("to", Timestamp.from(to.toInstant()))
                .addValue("tz", zoneId.getId());

        StringBuilder filterSql = new StringBuilder();
        if (serviceType != null && !serviceType.isBlank()) {
            filterSql.append(" AND d.service_type = :serviceType");
            params.addValue("serviceType", serviceType.toUpperCase());
        }
        if (priority != null && !priority.isBlank()) {
            filterSql.append(" AND d.priority = :priority");
            params.addValue("priority", priority.toUpperCase());
        }
        if (destinationLocationId != null) {
            filterSql.append(" AND d.destination_location_id = :destinationLocationId");
            params.addValue("destinationLocationId", destinationLocationId);
        }

        String truncUnit = switch (granularity) {
            case WEEK -> "week";
            case MONTH -> "month";
            case DAY -> "day";
        };

        String sql = """
            SELECT 
                DATE_TRUNC('__TRUNC_UNIT__', d.created_at AT TIME ZONE :tz)::DATE AS bucket_date,
                COUNT(*) AS total_created,
                COUNT(*) FILTER (WHERE d.status = 'DELIVERED') AS delivered,
                COUNT(*) FILTER (WHERE d.status = 'RETURN_TO_BASE') AS returned_to_base,
                COUNT(*) FILTER (
                    WHERE d.status = 'DELIVERED' AND pod.accepted_at IS NOT NULL AND pod.accepted_at <= COALESCE(rs.scheduled_end_time, d.window_end)
                ) AS on_time_delivered,
                COUNT(*) FILTER (
                    WHERE d.status = 'DELIVERED' AND pod.accepted_at IS NOT NULL AND pod.accepted_at > COALESCE(rs.scheduled_end_time, d.window_end)
                ) AS late_delivered,
                COALESCE(SUM(att.failed_attempts), 0) AS failed_attempts
            FROM delivery_order d
            LEFT JOIN proof_of_delivery pod 
                ON pod.delivery_order_id = d.id AND pod.tenant_id = d.tenant_id AND pod.status = 'FINALIZED'
            LEFT JOIN (
                SELECT delivery_id, COUNT(*) AS failed_attempts 
                FROM delivery_attempt 
                WHERE tenant_id = :tenantId 
                GROUP BY delivery_id
            ) att ON att.delivery_id = d.id
            LEFT JOIN (
                SELECT DISTINCT ON (delivery_order_id) id, delivery_order_id, scheduled_end_time
                FROM delivery_redelivery_schedule
                WHERE tenant_id = :tenantId AND status = 'CONFIRMED'
                ORDER BY delivery_order_id, scheduled_at DESC
            ) rs ON rs.delivery_order_id = d.id
            WHERE d.tenant_id = :tenantId 
              AND d.created_at >= :from 
              AND d.created_at < :to
              __FILTER_SQL__
            GROUP BY bucket_date
            ORDER BY bucket_date ASC
            """
                .replace("__TRUNC_UNIT__", truncUnit)
                .replace("__FILTER_SQL__", filterSql.toString());

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            LocalDate bucketDate = rs.getDate("bucket_date").toLocalDate();
            long totalCreated = rs.getLong("total_created");
            long delivered = rs.getLong("delivered");
            long returned = rs.getLong("returned_to_base");
            long onTime = rs.getLong("on_time_delivered");
            long late = rs.getLong("late_delivered");
            long failed = rs.getLong("failed_attempts");

            return new DeliveryTrendItem(
                    bucketDate,
                    totalCreated,
                    delivered,
                    failed,
                    returned,
                    onTime,
                    late
            );
        });
    }
}
