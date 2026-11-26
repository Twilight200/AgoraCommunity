package com.example.agora.platform.api;

import java.util.Map;

public interface OutboxPublisher {

    String record(String aggregateType, long aggregateId, String eventType, Map<String, Object> payload);

}
