package com.transportlogistics.app.fuel.infrastructure.adapters.in.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.transportlogistics.app.fuel.application.ports.out.FuelCardImportParser;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;

@Component
class CanonicalFuelCardJsonParser implements FuelCardImportParser {
    private static final Set<String> ROOT=Set.of("schemaVersion","providerBatchId","generatedAt","transactions");
    private static final Set<String> TX=Set.of("providerTransactionId","providerCardReference","transactionKind",
            "originalProviderTransactionId","transactionTimestamp","postedTimestamp","stationReference","fuelType",
            "quantityLitres","unitPrice","totalAmount","currency","providerVehicleReference","providerDriverReference",
            "tripId","providerStatus");
    private final ObjectMapper mapper;
    CanonicalFuelCardJsonParser(ObjectMapper mapper){this.mapper=mapper;}
    @Override public ParsedBatch parse(byte[] bytes){
        try{
            if(bytes.length>=3&&(bytes[0]&255)==0xEF&&(bytes[1]&255)==0xBB&&(bytes[2]&255)==0xBF) invalid();
            String json=decode(bytes); JsonNode root=mapper.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(json);
            if(root==null||!root.isObject()) invalid(); only(root,ROOT);
            if(!"FUEL_CARD_TRANSACTIONS_V1".equals(text(root,"schemaVersion"))) invalid();
            JsonNode values=root.get("transactions"); if(values==null||!values.isArray()||values.isEmpty()||values.size()>1000) invalid();
            List<ParsedTransaction> txs=new ArrayList<>();
            for(JsonNode n:values){ only(n,TX); String kind=text(n,"transactionKind"); String status=text(n,"providerStatus");
                if(!Set.of("PURCHASE","REVERSAL").contains(kind)||!Set.of("POSTED","REVERSED").contains(status)
                        ||("PURCHASE".equals(kind)&&!"POSTED".equals(status))
                        ||("REVERSAL".equals(kind)&&(!"REVERSED".equals(status)||optional(n,"originalProviderTransactionId")==null))) invalid();
                BigDecimal qty=positive(n,"quantityLitres"),unit=positive(n,"unitPrice"),total=positive(n,"totalAmount");
                String currency=text(n,"currency").toUpperCase(Locale.ROOT); Currency.getInstance(currency);
                txs.add(new ParsedTransaction(text(n,"providerTransactionId"),text(n,"providerCardReference"),hash(mapper.writeValueAsBytes(n)),kind,
                        optional(n,"originalProviderTransactionId"),time(n,"transactionTimestamp"),optionalTime(n,"postedTimestamp"),
                        optional(n,"stationReference"),text(n,"fuelType"),qty,unit,total,currency,optional(n,"providerVehicleReference"),
                        optional(n,"providerDriverReference"),optionalUuid(n,"tripId"),status));
            }
            return new ParsedBatch(text(root,"schemaVersion"),text(root,"providerBatchId"),time(root,"generatedAt"),hash(bytes),List.copyOf(txs));
        }catch(BusinessRuleException ex){throw ex;}catch(Exception ex){throw new BusinessRuleException("FUEL_CARD_IMPORT_INVALID","FUEL_CARD_IMPORT_INVALID");}
    }
    private static String decode(byte[] bytes)throws CharacterCodingException{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
    private static void only(JsonNode n,Set<String> allowed){if(!n.isObject())invalid();n.fieldNames().forEachRemaining(k->{if(!allowed.contains(k))invalid();});}
    private static String text(JsonNode n,String k){JsonNode v=n.get(k);if(v==null||!v.isTextual()||v.asText().isBlank())return invalid();return v.asText();}
    private static String optional(JsonNode n,String k){JsonNode v=n.get(k);return v==null||v.isNull()?null:v.isTextual()&&!v.asText().isBlank()?v.asText():invalid();}
    private static BigDecimal positive(JsonNode n,String k){JsonNode v=n.get(k);if(v==null||!v.isNumber()||v.decimalValue().signum()<=0)return invalid();return v.decimalValue();}
    private static OffsetDateTime time(JsonNode n,String k){return OffsetDateTime.parse(text(n,k));}
    private static OffsetDateTime optionalTime(JsonNode n,String k){String v=optional(n,k);return v==null?null:OffsetDateTime.parse(v);}
    private static UUID optionalUuid(JsonNode n,String k){String v=optional(n,k);return v==null?null:UUID.fromString(v);}
    private static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static <T>T invalid(){throw new BusinessRuleException("FUEL_CARD_IMPORT_INVALID","FUEL_CARD_IMPORT_INVALID");}
}
