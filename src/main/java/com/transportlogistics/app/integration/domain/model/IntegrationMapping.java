package com.transportlogistics.app.integration.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public record IntegrationMapping(
        UUID id, UUID tenantId, UUID configurationId, String mappingKey, int mappingVersion,
        String sourceContract, int sourceVersion, String targetSchema, int targetVersion,
        List<Rule> rules, String definitionHash, Lifecycle lifecycle, OffsetDateTime createdAt, String createdBy
) {
    public static final String PROBE_CONTRACT = "US73_PLATFORM_PROBE";
    public static final String PROBE_SCHEMA = "US73_FILE_PROBE";
    private static final Set<String> SOURCE_FIELDS = Set.of("probeId", "probeType", "sequence");
    private static final Set<String> TARGET_FIELDS = Set.of("probe_id", "probe_type", "sequence");
    private static final Pattern FIELD = Pattern.compile("[A-Za-z][A-Za-z0-9_.]{0,99}");

    public IntegrationMapping {
        Objects.requireNonNull(id, "Mapping id is required");
        Objects.requireNonNull(tenantId, "Tenant is required");
        Objects.requireNonNull(configurationId, "Configuration is required");
        mappingKey = required(mappingKey, "Mapping key is required");
        sourceContract = required(sourceContract, "Source contract is required");
        targetSchema = required(targetSchema, "Target schema is required");
        rules = List.copyOf(Objects.requireNonNull(rules, "Mapping rules are required"));
        Objects.requireNonNull(lifecycle, "Mapping lifecycle is required");
        Objects.requireNonNull(createdAt, "Creation time is required");
        createdBy = required(createdBy, "Creation actor is required");
        validate(sourceContract, sourceVersion, targetSchema, targetVersion, mappingVersion, rules);
        String expected = hash(mappingKey, mappingVersion, sourceContract, sourceVersion, targetSchema, targetVersion,
            rules);
        if (definitionHash == null) definitionHash = expected;
        if (!definitionHash.equals(expected)) throw invalid("Mapping definition hash is invalid");
    }

    public static IntegrationMapping active(UUID tenantId, UUID configurationId, String mappingKey,
                                            int mappingVersion, String sourceContract, int sourceVersion,
                                            String targetSchema, int targetVersion, List<Rule> rules,
                                            OffsetDateTime now, String actor) {
        return new IntegrationMapping(UUID.randomUUID(), tenantId, configurationId, mappingKey, mappingVersion,
            sourceContract, sourceVersion, targetSchema, targetVersion, rules, null, Lifecycle.ACTIVE, now, actor);
    }

    public Map<String, Object> apply(Map<String, ?> source) {
        if (!source.keySet().equals(SOURCE_FIELDS)) {
            throw invalid("Probe payload fields do not match the registered contract");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Rule rule : rules) {
            Object raw = rule.sourceField() == null ? null : source.get(rule.sourceField());
            if (raw == null && rule.defaultValue() != null) raw = rule.defaultValue();
            if (raw == null && rule.omitIfNull()) continue;
            if (raw == null && rule.required()) throw invalid("Required mapping value is absent");
            result.put(rule.targetField(), format(raw, rule.format()));
        }
        return result;
    }

    private static Object format(Object value, Format format) {
        if (value == null) return null;
        return switch (format) {
            case STRING, ENUM, ISO_DATE_TIME -> String.valueOf(value);
            case UUID -> UUID.fromString(String.valueOf(value)).toString();
            case DECIMAL -> new BigDecimal(String.valueOf(value));
            case BOOLEAN -> {
                String text = String.valueOf(value).toLowerCase(Locale.ROOT);
                if (!text.equals("true") && !text.equals("false")) {
                    throw invalid("Mapping value is not a boolean");
                }
                yield Boolean.valueOf(text);
            }
        };
    }

    private static void validate(String sourceContract, int sourceVersion, String targetSchema, int targetVersion,
                                 int mappingVersion, List<Rule> rules) {
        if (!PROBE_CONTRACT.equals(sourceContract) || sourceVersion != 1 || !PROBE_SCHEMA.equals(targetSchema)
                || targetVersion != 1 || mappingVersion < 1) {
            throw invalid("Mapping contract, schema, or version is not registered");
        }
        if (rules.isEmpty() || rules.size() > 100) throw invalid("Mapping requires 1 to 100 rules");
        Set<String> targets = new java.util.HashSet<>();
        for (Rule rule : rules) {
            if (rule.sourceField() != null && !SOURCE_FIELDS.contains(rule.sourceField())) {
                throw invalid("Mapping source field is not allow-listed");
            }
            if (!TARGET_FIELDS.contains(rule.targetField()) || !targets.add(rule.targetField())) {
                throw invalid("Mapping target is invalid or duplicated");
            }
            if (rule.targetField().split("\\.").length > 10) {
                throw invalid("Mapping nesting depth exceeds 10");
            }
        }
        if (!targets.equals(TARGET_FIELDS)) throw invalid("Registered target fields are required");
    }

    private static String hash(String key, int mappingVersion, String sourceContract, int sourceVersion,
                               String targetSchema, int targetVersion, List<Rule> rules) {
        List<String> canonicalRules = new ArrayList<>();
        rules.stream().sorted(Comparator.comparing(Rule::targetField)).forEach(rule -> canonicalRules.add(
            String.join("|", Objects.toString(rule.sourceField(), ""), rule.targetField(),
                Objects.toString(rule.defaultValue(), ""), rule.format().name(), Boolean.toString(rule.omitIfNull()),
                Boolean.toString(rule.required()))));
        String value = String.join("#", key, Integer.toString(mappingVersion), sourceContract,
            Integer.toString(sourceVersion), targetSchema, Integer.toString(targetVersion),
            String.join(";", canonicalRules));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String required(String value, String message) {
        if (value == null || value.trim().isEmpty() || !FIELD.matcher(value.trim()).matches()) {
            throw invalid(message);
        }
        return value.trim();
    }

    public record Rule(String sourceField, String targetField, String defaultValue, Format format,
                       boolean omitIfNull, boolean required) {
        public Rule {
            if (sourceField != null && !FIELD.matcher(sourceField).matches()) {
                throw invalid("Invalid source field");
            }
            targetField = IntegrationMapping.required(targetField, "Invalid target field");
            if (defaultValue != null && defaultValue.length() > 200) {
                throw invalid("Mapping default is too long");
            }
            Objects.requireNonNull(format, "Mapping format is required");
            if (sourceField == null && defaultValue == null) {
                throw invalid("A source field or approved literal default is required");
            }
        }
    }

    private static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("INTEGRATION_MAPPING_INVALID", message);
    }

    public enum Format { STRING, ISO_DATE_TIME, DECIMAL, BOOLEAN, UUID, ENUM }
    public enum Lifecycle { ACTIVE, SUPERSEDED }
}
