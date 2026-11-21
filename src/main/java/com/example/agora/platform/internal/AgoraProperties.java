package com.example.agora.platform.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agora")
public record AgoraProperties(Role role) {

    public enum Role{
        API,
        WORKER
    }
}
