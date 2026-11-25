package com.example.agora.identity.internal;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class UserDailyQuotaRepository {
    private final UserDailyQuotaMapper mapper;

    public UserDailyQuotaRepository(UserDailyQuotaMapper mapper){
        this.mapper = mapper;
    }

    public UserDailyQuotaCount lockOrCreate(long userId, LocalDate quotaDate){
        mapper.insertIfAbsent(userId,quotaDate);

        UserDailyQuotaCount count = mapper.findForUpdate(userId, quotaDate);

        if(count == null){
            throw new IllegalStateException(
                    "Quota row was not found after insert."
            );
        }

        return count;
    }

    public void increment(QuotaResource resource, long userId, LocalDate quotaDate){
        int affectedRows = switch (resource){
            case REPLY -> mapper.incrementReplies(userId,quotaDate);
            case TOPIC -> mapper.incrementTopics(userId, quotaDate);
        };

        if(affectedRows != 1){
            throw new IllegalStateException(
                    "Incrementing a quota must affect exactly one row."
            );
        }
    }
}
