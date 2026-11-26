package com.example.agora.devtools.datagen;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DataGenProperties.class)
public class DataGenConfiguration {
}
