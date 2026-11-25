package com.example.agora.identity.internal;

import com.example.agora.identity.api.QuotaDecision;

public class QuotaPolicy {

    public QuotaDecision decide(int trustLevel, QuotaResource resource, int usedCount, boolean containsLink){
        if(trustLevel == 0 && containsLink){
            return QuotaDecision.deny("IDENTITY_QUOTA_LINK_FORBIDDEN");
        }

        int dailyLimit = dailyLimit(trustLevel, resource);

        if(usedCount >= dailyLimit){
            return QuotaDecision.deny(resource == QuotaResource.TOPIC
                    ? "IDENTITY_TOPIC_QUOTA_EXCEEDED"
                    : "IDENTITY_REPLY_QUOTA_EXCEEDED");
        }

        return QuotaDecision.allow(dailyLimit - usedCount - 1);
    }


    private int dailyLimit(int trustLevel, QuotaResource resource){
        if(trustLevel == 0)
            return resource == QuotaResource.TOPIC ? 3 : 20;

        if(trustLevel == 1)
            return resource == QuotaResource.TOPIC ? 10 : 100;

        throw new IllegalArgumentException("Unsupported trust level: " + trustLevel);
    }
}
