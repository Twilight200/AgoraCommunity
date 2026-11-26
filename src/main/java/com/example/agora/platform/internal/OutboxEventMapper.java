package com.example.agora.platform.internal;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OutboxEventMapper {

    @Insert("""
        INSERT INTO outbox_event (
            event_id,
            aggregate_type,
            aggregate_id,
            event_type,
            payload,
            status,
            attempts
        )
        VALUES (
            #{eventId},
            #{aggregateType},
            #{aggregateId},
            #{eventType},
            CAST(#{payloadJson} AS JSON),
            'NEW',
            0
        )
        """)
    int insertNew(
            @Param("eventId") String eventId,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") long aggregateId,
            @Param("eventType") String eventType,
            @Param("payloadJson") String payloadJson
    );
}