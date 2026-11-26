package com.example.agora.platform.internal;

import com.example.agora.platform.api.IdGenerator;
import com.example.agora.platform.api.OutboxPublisher;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class DatabaseOutboxPublisher implements OutboxPublisher {
    private final OutboxEventMapper mapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public DatabaseOutboxPublisher(OutboxEventMapper mapper, ObjectMapper objectMapper, IdGenerator idGenerator){
        this.idGenerator = idGenerator;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public String record(
            String aggregateType,
            long aggregateId,
            String eventType,
            Map<String, Object> payload
    ) {
        String eventId = "%026d".formatted(idGenerator.nextId());
        String payloadJson;

        try{
            payloadJson = objectMapper.writeValueAsString(payload);

        } catch (JacksonException e){
            throw new IllegalArgumentException(
                    "Outbox payload cannot be serialized as JSON.",
                    e
            );
        }
        int affectedRows = mapper.insertNew(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                payloadJson
        );

        if(affectedRows != 1){
            throw new IllegalStateException(
                    "Recording an outbox event must affect one row"
            );
        }
        return eventId;
    }
}
