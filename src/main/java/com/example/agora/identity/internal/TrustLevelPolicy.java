package com.example.agora.identity.internal;

public class TrustLevelPolicy {
    private static final int TL1_MIN_DISTINCT_TOPIC_READ = 20;
    private static final int TL1_MIN_VISIT_DAYS = 3;

    public boolean qualifiesForTl1(int currentTrustLevel, long distinctTopicsRead, long distinctVisitDays){
        return currentTrustLevel == 0 && distinctVisitDays >= TL1_MIN_VISIT_DAYS && distinctTopicsRead >= TL1_MIN_DISTINCT_TOPIC_READ;
    }
}
