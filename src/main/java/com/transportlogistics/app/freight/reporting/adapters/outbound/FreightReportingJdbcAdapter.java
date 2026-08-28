package com.transportlogistics.app.freight.reporting.adapters.outbound;

import com.transportlogistics.app.freight.FreightReportingQuery;
import com.transportlogistics.app.tenancy.CurrentTenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
class FreightReportingJdbcAdapter implements FreightReportingQuery {
    private static final int SUMMARY_LIMIT = 10_000;
    private final JdbcTemplate jdbc;
    private final CurrentTenant currentTenant;

    FreightReportingJdbcAdapter(JdbcTemplate jdbc, CurrentTenant currentTenant) {
        this.jdbc = jdbc;
        this.currentTenant = currentTenant;
    }

    @Override
    public FreightSummary summary(FreightReportCriteria criteria) {
        List<FreightShipmentReportItem> rows = queryShipments(criteria, 0, SUMMARY_LIMIT, "fo.created_at", "ASC");
        long total = countShipments(criteria);
        if (total > SUMMARY_LIMIT) {
            throw new IllegalStateException("Freight summary source exceeds the controlled reporting limit");
        }
        var manifestRows = rows.stream().filter(row -> row.manifestId() != null)
                .collect(java.util.stream.Collectors.toMap(FreightShipmentReportItem::manifestId, row -> row, (left, right) -> left));
        long manifests = manifestRows.size();
        long items = manifestRows.values().stream().mapToLong(FreightShipmentReportItem::manifestItemCount).sum();
        var loadPlanRows = rows.stream().filter(row -> row.loadPlanId() != null)
                .collect(java.util.stream.Collectors.toMap(FreightShipmentReportItem::loadPlanId, row -> row, (left, right) -> left));
        long loadPlans = loadPlanRows.size();
        Map<String, Long> readiness = distribution(loadPlanRows.values().stream().filter(row -> row.loadPlanStatus() != null)
                .map(FreightShipmentReportItem::loadPlanStatus).toList());
        Map<String, Long> compliance = distribution(loadPlanRows.values().stream().filter(row -> row.complianceOutcome() != null)
                .map(FreightShipmentReportItem::complianceOutcome).toList());

        Map<String, Long> policyStatuses = groupedCount("freight_insurance_policy", "status", criteria, "freight_order_id");
        Map<String, Long> claimStatuses = groupedCount("freight_insurance_claim", "status", criteria, "freight_order_id");
        Map<String, Long> exceptionStatuses = groupedCount("cargo_exception", "status", criteria, "freight_order_id");
        Map<String, Long> exceptionTypes = groupedCount("cargo_exception", "exception_type", criteria, "freight_order_id");
        long settlements = relatedCount("freight_insurance_settlement s JOIN freight_insurance_claim x ON x.id=s.claim_id",
                "x.freight_order_id", criteria);
        long unresolved = exceptionStatuses.entrySet().stream()
                .filter(entry -> !"RESOLVED".equals(entry.getKey()) && !"REJECTED".equals(entry.getKey()))
                .mapToLong(Map.Entry::getValue).sum();
        return new FreightSummary(total, manifests, items, loadPlans, readiness, compliance,
                sum(policyStatuses), policyStatuses, sum(claimStatuses), claimStatuses, settlements,
                sum(exceptionStatuses), exceptionStatuses, exceptionTypes, unresolved);
    }

    @Override
    public Page<FreightShipmentReportItem> shipments(FreightReportCriteria criteria, Pageable pageable) {
        String requestedSort = pageable.getSort().stream().findFirst().map(order -> order.getProperty()).orElse("createdAt");
        String sort = switch (requestedSort) {
            case "orderNumber" -> "fo.order_number";
            case "requestedPickupAt" -> "fo.requested_pickup_at";
            case "manifestNumber" -> "cm.manifest_number";
            case "loadPlanStatus" -> "lp.readiness_status";
            default -> "fo.created_at";
        };
        String direction = pageable.getSort().stream().findFirst().map(order -> order.isDescending() ? "DESC" : "ASC")
                .orElse("DESC");
        List<FreightShipmentReportItem> content = queryShipments(criteria, (int) pageable.getOffset(), pageable.getPageSize(), sort, direction);
        return new PageImpl<>(content, pageable, countReportRows(criteria));
    }

    @Override
    public List<FreightShipmentReportItem> exportShipments(FreightReportCriteria criteria, int limit) {
        return queryShipments(criteria, 0, limit, "fo.created_at", "ASC");
    }

    private List<FreightShipmentReportItem> queryShipments(FreightReportCriteria criteria, int offset, int limit,
                                                            String sort, String direction) {
        var where = where(criteria, "fo");
        List<Object> args = new ArrayList<>(where.args());
        args.add(limit);
        args.add(offset);
        String sql = """
                SELECT fo.id freight_order_id, fo.order_number, fo.customer_id, fo.origin_location_id,
                  fo.destination_location_id, fo.created_at, fo.requested_pickup_at, fo.requested_delivery_at,
                  cm.id manifest_id, cm.manifest_number, cm.finalized_at,
                  COUNT(DISTINCT cmi.id) manifest_item_count,
                  lp.id load_plan_id, lp.load_plan_number, lp.readiness_status, lp.vehicle_id,
                  SUM(CASE WHEN cmi.id IS NULL OR cmi.unit_weight IS NULL OR cmi.weight_unit IS NULL THEN NULL
                    ELSE cmi.quantity*cmi.unit_weight*CASE cmi.weight_unit WHEN 'G' THEN 0.001 WHEN 'TONNE' THEN 1000 ELSE 1 END END) cargo_weight_kg,
                  SUM(CASE WHEN cmi.id IS NULL OR cmi.length IS NULL OR cmi.width IS NULL OR cmi.height IS NULL OR cmi.dimension_unit IS NULL THEN NULL
                    ELSE cmi.quantity*cmi.length*cmi.width*cmi.height*
                      CASE cmi.dimension_unit WHEN 'CM' THEN 0.000001 WHEN 'MM' THEN 0.000000001 ELSE 1 END END) cargo_volume_m3,
                  SUM(CASE WHEN cmi.id IS NOT NULL AND (cmi.unit_weight IS NULL OR cmi.weight_unit IS NULL) THEN 1 ELSE 0 END) missing_weight,
                  SUM(CASE WHEN cmi.id IS NOT NULL AND (cmi.length IS NULL OR cmi.width IS NULL OR cmi.height IS NULL OR cmi.dimension_unit IS NULL) THEN 1 ELSE 0 END) missing_volume,
                  v.capacity_kg, v.tare_weight_kg, v.gross_vehicle_weight_kg, v.cargo_volume_capacity_m3,
                  v.axle_count, v.max_axle_load_kg
                FROM freight_order fo
                LEFT JOIN cargo_manifest cm ON cm.freight_order_id=fo.id AND cm.tenant_id=fo.tenant_id
                LEFT JOIN cargo_manifest_item cmi ON cmi.cargo_manifest_id=cm.id AND cmi.tenant_id=fo.tenant_id
                LEFT JOIN load_plan lp ON lp.cargo_manifest_id=cm.id AND lp.tenant_id=fo.tenant_id
                LEFT JOIN vehicle v ON v.id=lp.vehicle_id AND v.tenant_id=fo.tenant_id
                """ + where.sql() + " GROUP BY fo.id,fo.order_number,fo.customer_id,fo.origin_location_id,fo.destination_location_id," +
                "fo.created_at,fo.requested_pickup_at,fo.requested_delivery_at,cm.id,cm.manifest_number,cm.finalized_at," +
                "lp.id,lp.load_plan_number,lp.readiness_status,lp.vehicle_id,v.capacity_kg,v.tare_weight_kg," +
                "v.gross_vehicle_weight_kg,v.cargo_volume_capacity_m3,v.axle_count,v.max_axle_load_kg ORDER BY " + sort + " " + direction + " LIMIT ? OFFSET ?";
        return jdbc.query(sql, this::mapShipment, args.toArray());
    }

    private FreightShipmentReportItem mapShipment(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal weight = rs.getBigDecimal("cargo_weight_kg");
        BigDecimal volume = rs.getBigDecimal("cargo_volume_m3");
        long itemCount = rs.getLong("manifest_item_count");
        long missingWeight = rs.getLong("missing_weight");
        long missingVolume = rs.getLong("missing_volume");
        if (itemCount == 0 || missingWeight > 0) weight = null;
        if (itemCount == 0 || missingVolume > 0) volume = null;
        var diagnostics = new ArrayList<String>();
        if (weight == null) diagnostics.add("CARGO_ITEM_WEIGHT_DATA_MISSING");
        if (volume == null) diagnostics.add("CARGO_ITEM_DIMENSIONS_DATA_MISSING");
        Double capacity = nullableDouble(rs, "capacity_kg");
        Double volumeCapacity = nullableDouble(rs, "cargo_volume_capacity_m3");
        Double tare = nullableDouble(rs, "tare_weight_kg");
        Double gvw = nullableDouble(rs, "gross_vehicle_weight_kg");
        if (capacity == null) diagnostics.add("VEHICLE_PAYLOAD_CAPACITY_MISSING");
        if (volumeCapacity == null) diagnostics.add("VEHICLE_VOLUME_CAPACITY_UNAVAILABLE");
        if (tare == null || gvw == null) diagnostics.add("VEHICLE_GVW_DATA_MISSING");
        String outcome = null;
        BigDecimal payloadUtilization = percent(weight, capacity);
        BigDecimal volumeUtilization = percent(volume, volumeCapacity);
        if (rs.getObject("load_plan_id") != null) {
            if (!diagnostics.isEmpty()) outcome = "INCOMPLETE";
            else if (payloadUtilization.compareTo(BigDecimal.valueOf(100)) > 0 ||
                    volumeUtilization.compareTo(BigDecimal.valueOf(100)) > 0 ||
                    BigDecimal.valueOf(tare).add(weight).compareTo(BigDecimal.valueOf(gvw)) > 0) outcome = "FAIL";
            else outcome = "PASS";
        }
        return new FreightShipmentReportItem(uuid(rs, "freight_order_id"), rs.getString("order_number"),
                uuid(rs, "customer_id"), uuid(rs, "origin_location_id"), uuid(rs, "destination_location_id"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("requested_pickup_at", java.time.OffsetDateTime.class),
                rs.getObject("requested_delivery_at", java.time.OffsetDateTime.class), uuid(rs, "manifest_id"),
                rs.getString("manifest_number"), rs.getObject("finalized_at") != null, itemCount,
                uuid(rs, "load_plan_id"), rs.getString("load_plan_number"), rs.getString("readiness_status"),
                uuid(rs, "vehicle_id"), weight, volume, payloadUtilization, volumeUtilization, outcome, List.copyOf(diagnostics));
    }

    private long countShipments(FreightReportCriteria criteria) {
        var where = where(criteria, "fo");
        return jdbc.queryForObject("SELECT COUNT(*) FROM freight_order fo " + where.sql(), Long.class, where.args().toArray());
    }

    private long countReportRows(FreightReportCriteria criteria) {
        var where = where(criteria, "fo");
        String sql = "SELECT COUNT(*) FROM (SELECT fo.id,cm.id manifest_id,lp.id load_plan_id FROM freight_order fo " +
                "LEFT JOIN cargo_manifest cm ON cm.freight_order_id=fo.id AND cm.tenant_id=fo.tenant_id " +
                "LEFT JOIN load_plan lp ON lp.cargo_manifest_id=cm.id AND lp.tenant_id=fo.tenant_id " + where.sql() +
                " GROUP BY fo.id,cm.id,lp.id) report_rows";
        return jdbc.queryForObject(sql, Long.class, where.args().toArray());
    }

    private Map<String, Long> groupedCount(String table, String field, FreightReportCriteria criteria, String orderColumn) {
        var where = whereForRelated(criteria, "x", orderColumn);
        String sql = "SELECT x." + field + ",COUNT(*) FROM " + table + " x JOIN freight_order fo ON fo.id=x." + orderColumn +
                " AND fo.tenant_id=x.tenant_id " + where.sql() + " GROUP BY x." + field + " ORDER BY x." + field;
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query(sql, (org.springframework.jdbc.core.RowCallbackHandler)
                rs -> result.put(rs.getString(1), rs.getLong(2)), where.args().toArray());
        return Map.copyOf(result);
    }

    private long relatedCount(String from, String orderColumn, FreightReportCriteria criteria) {
        var where = whereForRelated(criteria, "x", orderColumn);
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + from + " JOIN freight_order fo ON fo.id=" + orderColumn +
                " AND fo.tenant_id=x.tenant_id " + where.sql(), Long.class, where.args().toArray());
    }

    private SqlWhere where(FreightReportCriteria c, String alias) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        clauses.add(alias + ".tenant_id=?"); args.add(currentTenant.required().tenantId());
        add(clauses, args, c.from() != null, alias + ".created_at>=?", c.from());
        add(clauses, args, c.to() != null, alias + ".created_at<?", c.to());
        add(clauses, args, c.customerId() != null, alias + ".customer_id=?", c.customerId());
        add(clauses, args, c.freightOrderId() != null, alias + ".id=?", c.freightOrderId());
        add(clauses, args, c.originLocationId() != null, alias + ".origin_location_id=?", c.originLocationId());
        add(clauses, args, c.destinationLocationId() != null, alias + ".destination_location_id=?", c.destinationLocationId());
        add(clauses, args, c.loadPlanStatus() != null, "EXISTS(SELECT 1 FROM cargo_manifest m JOIN load_plan p ON p.cargo_manifest_id=m.id AND p.tenant_id=m.tenant_id WHERE m.freight_order_id="+alias+".id AND m.tenant_id="+alias+".tenant_id AND p.readiness_status=?)", upper(c.loadPlanStatus()));
        add(clauses, args, c.exceptionStatus() != null, "EXISTS(SELECT 1 FROM cargo_exception e WHERE e.freight_order_id="+alias+".id AND e.tenant_id="+alias+".tenant_id AND e.status=?)", upper(c.exceptionStatus()));
        add(clauses, args, c.exceptionType() != null, "EXISTS(SELECT 1 FROM cargo_exception e WHERE e.freight_order_id="+alias+".id AND e.tenant_id="+alias+".tenant_id AND e.exception_type=?)", upper(c.exceptionType()));
        add(clauses, args, c.policyStatus() != null, "EXISTS(SELECT 1 FROM freight_insurance_policy p WHERE p.freight_order_id="+alias+".id AND p.tenant_id="+alias+".tenant_id AND p.status=?)", upper(c.policyStatus()));
        add(clauses, args, c.claimStatus() != null, "EXISTS(SELECT 1 FROM freight_insurance_claim q WHERE q.freight_order_id="+alias+".id AND q.tenant_id="+alias+".tenant_id AND q.status=?)", upper(c.claimStatus()));
        return new SqlWhere(" WHERE " + String.join(" AND ", clauses), args);
    }

    private SqlWhere whereForRelated(FreightReportCriteria c, String alias, String orderColumn) {
        return where(c, "fo");
    }

    private static void add(List<String> clauses, List<Object> args, boolean condition, String clause, Object value) {
        if (condition) { clauses.add(clause); args.add(value); }
    }
    private static String upper(String value) { return value == null ? null : value.toUpperCase(Locale.ROOT); }
    private static long sum(Map<String, Long> values) { return values.values().stream().mapToLong(Long::longValue).sum(); }
    private static Map<String, Long> distribution(List<String> values) {
        Map<String, Long> result = new LinkedHashMap<>(); values.forEach(value -> result.merge(value, 1L, Long::sum)); return Map.copyOf(result);
    }
    private static UUID uuid(ResultSet rs, String name) throws SQLException { return rs.getObject(name, UUID.class); }
    private static Double nullableDouble(ResultSet rs, String name) throws SQLException { double value=rs.getDouble(name); return rs.wasNull()?null:value; }
    private static BigDecimal percent(BigDecimal value, Double capacity) {
        return value == null || capacity == null || capacity <= 0 ? null : value.divide(BigDecimal.valueOf(capacity), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
    private record SqlWhere(String sql, List<Object> args) {}
}
