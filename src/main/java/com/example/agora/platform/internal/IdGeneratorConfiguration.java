package com.example.agora.platform.internal;

import com.example.agora.platform.api.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class IdGeneratorConfiguration {

    @Bean
    IdGenerator idGenerator(IdGeneratorProperties properties){
        return new SnowflakeIdGenerator(properties.workerId(), Clock.systemUTC());
    }

}
