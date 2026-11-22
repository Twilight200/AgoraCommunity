package com.example.agora.platform.internal;

import com.example.agora.platform.api.FaultPoint;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
class FaultPointTestConfiguration {

    @Bean
    TestFaultPoint testFaultPoint() {
        return new TestFaultPoint();
    }

    @Bean
    FaultPoint faultPoint(TestFaultPoint testFaultPoint) {
        return testFaultPoint;
    }
}