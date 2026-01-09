/**
 * Copyright FINOS and its Contributors
 */
package org.finos.fdc3.proxy.support.responses;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.finos.fdc3.proxy.support.TestMessaging;

/**
 * Support utilities for creating test responses.
 */
public class ResponseSupport {
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * Creates response metadata from request metadata.
     */
    public static Map<String, Object> createResponseMeta(Map<String, Object> requestMeta) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestUuid", requestMeta.get("requestUuid"));
        meta.put("responseUuid", UUID.randomUUID().toString());
        meta.put("source", requestMeta.get("source"));
        meta.put("timestamp", Instant.now().toString());
        return meta;
    }
    
    /**
     * Schedules a response to be sent after a short delay.
     * This simulates async message delivery.
     */
    public static void scheduleReceive(TestMessaging messaging, Map<String, Object> response) {
        scheduler.schedule(() -> {
            messaging.receive(response, null);
        }, 100, TimeUnit.MILLISECONDS);
    }
}

