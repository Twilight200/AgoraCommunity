package com.example.agora.platform.internal;

import com.example.agora.platform.api.FaultPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(FaultPointTestConfiguration.class)
@ActiveProfiles("test")
class FaultPointTest {

    @Autowired
    private FaultPoint faultPoint;

    @Autowired
    private TestFaultPoint testFaultPoint;

    @Test
    void throwsArmedFailureOnlyOnce() {
        var expected = new IllegalStateException("injected failure");
        testFaultPoint.failNext("example.afterWrite", expected);

        assertThatThrownBy(() -> faultPoint.hit("example.afterWrite"))
                .isSameAs(expected);

        assertThatNoException()
                .isThrownBy(() -> faultPoint.hit("example.afterWrite"));
    }

    @Test
    void doesNothingWhenNoFailureIsArmed() {
        assertThatNoException()
                .isThrownBy(() -> faultPoint.hit("example.afterWrite"));
    }
}