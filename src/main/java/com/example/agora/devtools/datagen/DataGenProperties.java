package com.example.agora.devtools.datagen;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "agora.datagen")
public record DataGenProperties(
        long seed,
        String marker,
        int userCount,
        int categoryCount,
        int topicCount,
        int repliesPerTopic
) {

}
