package com.example.agora.platform.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agora.id-generator")
public record IdGeneratorProperties(Integer workerId) {

    public IdGeneratorProperties{
        if(workerId == null || workerId < 0 || workerId > 1023)
            throw new IllegalArgumentException("agora.id-generator.worker-id must be between 0 and 1023");
    }
}
