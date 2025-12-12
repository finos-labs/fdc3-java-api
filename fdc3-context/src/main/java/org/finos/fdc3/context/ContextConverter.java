package org.finos.fdc3.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.finos.fdc3.schema.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting JSON to the appropriate FDC3 context type
 * based on the "type" field in the JSON.
 */
public class ContextConverter {

    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        // Register Java 8 date/time module
        om.registerModule(new JavaTimeModule());
        // Don't fail on unknown properties (FDC3 allows additional properties)
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Write dates as ISO strings, not timestamps
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return om;
    }

    // Map of FDC3 type strings to their corresponding Java classes
    private static final Map<String, Class<?>> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("fdc3.action", Action.class);
        TYPE_MAP.put("fdc3.chart", Chart.class);
        TYPE_MAP.put("fdc3.chat.initSettings", ChatInitSettings.class);
        TYPE_MAP.put("fdc3.chat.message", ChatMessage.class);
        TYPE_MAP.put("fdc3.chat.room", ChatRoom.class);
        TYPE_MAP.put("fdc3.chat.searchCriteria", ChatSearchCriteria.class);
        TYPE_MAP.put("fdc3.contact", Contact.class);
        TYPE_MAP.put("fdc3.contactList", ContactList.class);
        TYPE_MAP.put("fdc3.context", Context.class);
        TYPE_MAP.put("fdc3.country", Country.class);
        TYPE_MAP.put("fdc3.currency", Currency.class);
        TYPE_MAP.put("fdc3.email", Email.class);
        TYPE_MAP.put("fdc3.fileAttachment", FileAttachment.class);
        TYPE_MAP.put("fdc3.instrument", Instrument.class);
        TYPE_MAP.put("fdc3.instrumentList", InstrumentList.class);
        TYPE_MAP.put("fdc3.interaction", Interaction.class);
        TYPE_MAP.put("fdc3.message", Message.class);
        TYPE_MAP.put("fdc3.nothing", Nothing.class);
        TYPE_MAP.put("fdc3.order", Order.class);
        TYPE_MAP.put("fdc3.orderList", OrderList.class);
        TYPE_MAP.put("fdc3.organization", Organization.class);
        TYPE_MAP.put("fdc3.portfolio", Portfolio.class);
        TYPE_MAP.put("fdc3.position", Position.class);
        TYPE_MAP.put("fdc3.product", Product.class);
        TYPE_MAP.put("fdc3.timeRange", TimeRange.class);
        TYPE_MAP.put("fdc3.trade", Trade.class);
        TYPE_MAP.put("fdc3.tradeList", TradeList.class);
        TYPE_MAP.put("fdc3.transactionResult", TransactionResult.class);
        TYPE_MAP.put("fdc3.valuation", Valuation.class);
    }

    /**
     * Get the Java class for a given FDC3 type string.
     *
     * @param fdc3Type the FDC3 type string (e.g., "fdc3.contact")
     * @return the corresponding Java class, or null if not found
     */
    public static Class<?> getClassForType(String fdc3Type) {
        return TYPE_MAP.get(fdc3Type);
    }

    /**
     * Parse a JSON string and return the appropriate context object based on the "type" field.
     *
     * @param json the JSON string to parse
     * @return the parsed context object
     * @throws IOException if parsing fails
     * @throws IllegalArgumentException if the type is unknown
     */
    public static Object fromJson(String json) throws IOException {
        JsonNode node = mapper.readTree(json);
        String type = node.has("type") ? node.get("type").asText() : null;
        
        if (type == null) {
            throw new IllegalArgumentException("JSON does not contain a 'type' field");
        }

        Class<?> clazz = TYPE_MAP.get(type);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown context type: " + type);
        }

        return mapper.treeToValue(node, clazz);
    }

    /**
     * Parse a JSON string into a specific context class.
     *
     * @param json the JSON string to parse
     * @param clazz the target class
     * @param <T> the type of the context
     * @return the parsed context object
     * @throws IOException if parsing fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return mapper.readValue(json, clazz);
    }

    /**
     * Serialize a context object to JSON.
     *
     * @param context the context object to serialize
     * @return the JSON string
     * @throws JsonProcessingException if serialization fails
     */
    public static String toJson(Object context) throws JsonProcessingException {
        return mapper.writeValueAsString(context);
    }

    /**
     * Serialize a context object to pretty-printed JSON.
     *
     * @param context the context object to serialize
     * @return the pretty-printed JSON string
     * @throws JsonProcessingException if serialization fails
     */
    public static String toJsonPretty(Object context) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }

    /**
     * Check if two JSON strings are semantically equivalent (same content, possibly different formatting).
     *
     * @param json1 first JSON string
     * @param json2 second JSON string
     * @return true if the JSON objects are equivalent
     * @throws IOException if parsing fails
     */
    public static boolean jsonEquals(String json1, String json2) throws IOException {
        JsonNode node1 = mapper.readTree(json1);
        JsonNode node2 = mapper.readTree(json2);
        return node1.equals(node2);
    }
}

