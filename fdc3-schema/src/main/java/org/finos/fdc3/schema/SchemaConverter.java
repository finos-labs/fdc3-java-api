package org.finos.fdc3.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Converter for JSON to FDC3 API message types based on the "type" field in the JSON.
 */
public class SchemaConverter {

    private final ObjectMapper mapper;

    // Map of FDC3 message type strings to their corresponding Java classes
    private static final Map<String, Class<?>> TYPE_MAP = new HashMap<>();

    static {
        // Request messages
        TYPE_MAP.put("addContextListenerRequest", AddContextListenerRequest.class);
        TYPE_MAP.put("addEventListenerRequest", AddEventListenerRequest.class);
        TYPE_MAP.put("addIntentListenerRequest", AddIntentListenerRequest.class);
        TYPE_MAP.put("broadcastRequest", BroadcastRequest.class);
        TYPE_MAP.put("contextListenerUnsubscribeRequest", ContextListenerUnsubscribeRequest.class);
        TYPE_MAP.put("createPrivateChannelRequest", CreatePrivateChannelRequest.class);
        TYPE_MAP.put("eventListenerUnsubscribeRequest", EventListenerUnsubscribeRequest.class);
        TYPE_MAP.put("findInstancesRequest", FindInstancesRequest.class);
        TYPE_MAP.put("findIntentRequest", FindIntentRequest.class);
        TYPE_MAP.put("findIntentsByContextRequest", FindIntentsByContextRequest.class);
        TYPE_MAP.put("getCurrentChannelRequest", GetCurrentChannelRequest.class);
        TYPE_MAP.put("getCurrentContextRequest", GetCurrentContextRequest.class);
        TYPE_MAP.put("getInfoRequest", GetInfoRequest.class);
        TYPE_MAP.put("getOrCreateChannelRequest", GetOrCreateChannelRequest.class);
        TYPE_MAP.put("getUserChannelsRequest", GetUserChannelsRequest.class);
        TYPE_MAP.put("heartbeatAcknowledgementRequest", HeartbeatAcknowledgementRequest.class);
        TYPE_MAP.put("intentListenerUnsubscribeRequest", IntentListenerUnsubscribeRequest.class);
        TYPE_MAP.put("intentResultRequest", IntentResultRequest.class);
        TYPE_MAP.put("joinUserChannelRequest", JoinUserChannelRequest.class);
        TYPE_MAP.put("leaveCurrentChannelRequest", LeaveCurrentChannelRequest.class);
        TYPE_MAP.put("openRequest", OpenRequest.class);
        TYPE_MAP.put("privateChannelAddEventListenerRequest", PrivateChannelAddEventListenerRequest.class);
        TYPE_MAP.put("privateChannelDisconnectRequest", PrivateChannelDisconnectRequest.class);
        TYPE_MAP.put("privateChannelUnsubscribeEventListenerRequest", PrivateChannelUnsubscribeEventListenerRequest.class);
        TYPE_MAP.put("raiseIntentForContextRequest", RaiseIntentForContextRequest.class);
        TYPE_MAP.put("raiseIntentRequest", RaiseIntentRequest.class);

        // Response messages
        TYPE_MAP.put("addContextListenerResponse", AddContextListenerResponse.class);
        TYPE_MAP.put("addEventListenerResponse", AddEventListenerResponse.class);
        TYPE_MAP.put("addIntentListenerResponse", AddIntentListenerResponse.class);
        TYPE_MAP.put("broadcastResponse", BroadcastResponse.class);
        TYPE_MAP.put("contextListenerUnsubscribeResponse", ContextListenerUnsubscribeResponse.class);
        TYPE_MAP.put("createPrivateChannelResponse", CreatePrivateChannelResponse.class);
        TYPE_MAP.put("eventListenerUnsubscribeResponse", EventListenerUnsubscribeResponse.class);
        TYPE_MAP.put("findInstancesResponse", FindInstancesResponse.class);
        TYPE_MAP.put("findIntentResponse", FindIntentResponse.class);
        TYPE_MAP.put("findIntentsByContextResponse", FindIntentsByContextResponse.class);
        TYPE_MAP.put("getCurrentChannelResponse", GetCurrentChannelResponse.class);
        TYPE_MAP.put("getCurrentContextResponse", GetCurrentContextResponse.class);
        TYPE_MAP.put("getInfoResponse", GetInfoResponse.class);
        TYPE_MAP.put("getOrCreateChannelResponse", GetOrCreateChannelResponse.class);
        TYPE_MAP.put("getUserChannelsResponse", GetUserChannelsResponse.class);
        TYPE_MAP.put("intentListenerUnsubscribeResponse", IntentListenerUnsubscribeResponse.class);
        TYPE_MAP.put("intentResultResponse", IntentResultResponse.class);
        TYPE_MAP.put("joinUserChannelResponse", JoinUserChannelResponse.class);
        TYPE_MAP.put("leaveCurrentChannelResponse", LeaveCurrentChannelResponse.class);
        TYPE_MAP.put("openResponse", OpenResponse.class);
        TYPE_MAP.put("privateChannelAddEventListenerResponse", PrivateChannelAddEventListenerResponse.class);
        TYPE_MAP.put("privateChannelDisconnectResponse", PrivateChannelDisconnectResponse.class);
        TYPE_MAP.put("privateChannelUnsubscribeEventListenerResponse", PrivateChannelUnsubscribeEventListenerResponse.class);
        TYPE_MAP.put("raiseIntentForContextResponse", RaiseIntentForContextResponse.class);
        TYPE_MAP.put("raiseIntentResponse", RaiseIntentResponse.class);
        TYPE_MAP.put("raiseIntentResultResponse", RaiseIntentResultResponse.class);

        // Event messages
        TYPE_MAP.put("broadcastEvent", BroadcastEvent.class);
        TYPE_MAP.put("channelChangedEvent", ChannelChangedEvent.class);
        TYPE_MAP.put("heartbeatEvent", HeartbeatEvent.class);
        TYPE_MAP.put("intentEvent", IntentEvent.class);
        TYPE_MAP.put("privateChannelOnAddContextListenerEvent", PrivateChannelOnAddContextListenerEvent.class);
        TYPE_MAP.put("privateChannelOnDisconnectEvent", PrivateChannelOnDisconnectEvent.class);
        TYPE_MAP.put("privateChannelOnUnsubscribeEvent", PrivateChannelOnUnsubscribeEvent.class);
    }

    /**
     * Creates a new SchemaConverter with a default ObjectMapper configuration.
     */
    public SchemaConverter() {
        this.mapper = createObjectMapper();
    }

    /**
     * Creates a new SchemaConverter with a custom ObjectMapper.
     *
     * @param mapper the ObjectMapper to use
     */
    public SchemaConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return om;
    }

    /**
     * Get the Java class for a given FDC3 message type string.
     *
     * @param messageType the FDC3 message type string (e.g., "broadcastRequest")
     * @return the corresponding Java class, or null if not found
     */
    public Class<?> getClassForType(String messageType) {
        return TYPE_MAP.get(messageType);
    }

    /**
     * Parse a JSON string and return the appropriate message object based on the "type" field.
     *
     * @param json the JSON string to parse
     * @return the parsed message object
     * @throws IOException if parsing fails
     * @throws IllegalArgumentException if the type is unknown
     */
    public Object fromJson(String json) throws IOException {
        JsonNode node = mapper.readTree(json);
        String type = node.has("type") ? node.get("type").asText() : null;

        if (type == null) {
            throw new IllegalArgumentException("JSON does not contain a 'type' field");
        }

        Class<?> clazz = TYPE_MAP.get(type);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown message type: " + type);
        }

        return mapper.treeToValue(node, clazz);
    }

    /**
     * Parse a JSON string into a specific message class.
     *
     * @param json the JSON string to parse
     * @param clazz the target class
     * @param <T> the type of the message
     * @return the parsed message object
     * @throws IOException if parsing fails
     */
    public <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return mapper.readValue(json, clazz);
    }

    /**
     * Convert an object to another type (e.g., Map to typed object).
     *
     * @param fromValue the source object
     * @param toValueType the target type
     * @param <T> the type to convert to
     * @return the converted object
     */
    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return mapper.convertValue(fromValue, toValueType);
    }

    /**
     * Convert an object to a Map.
     *
     * @param value the object to convert
     * @return the object as a Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap(Object value) {
        return mapper.convertValue(value, Map.class);
    }

    /**
     * Serialize a message object to JSON.
     *
     * @param message the message object to serialize
     * @return the JSON string
     * @throws JsonProcessingException if serialization fails
     */
    public String toJson(Object message) throws JsonProcessingException {
        return mapper.writeValueAsString(message);
    }

    /**
     * Serialize a message object to pretty-printed JSON.
     *
     * @param message the message object to serialize
     * @return the pretty-printed JSON string
     * @throws JsonProcessingException if serialization fails
     */
    public String toJsonPretty(Object message) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(message);
    }

    /**
     * Check if two JSON strings are semantically equivalent (same content, possibly different formatting).
     *
     * @param json1 first JSON string
     * @param json2 second JSON string
     * @return true if the JSON objects are equivalent
     * @throws IOException if parsing fails
     */
    public boolean jsonEquals(String json1, String json2) throws IOException {
        JsonNode node1 = mapper.readTree(json1);
        JsonNode node2 = mapper.readTree(json2);
        return node1.equals(node2);
    }

    /**
     * Get the ObjectMapper used by this converter.
     *
     * @return the ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return mapper;
    }
}
