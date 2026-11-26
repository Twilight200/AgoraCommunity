package com.example.agora.devtools.datagen;

import com.example.agora.platform.api.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataGenServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IdGenerator idGenerator;

    @Test
    void generatesUsersStatsAndCategories() {
        DataGenProperties properties = new DataGenProperties(
                20261126L,
                "datagen_20261126_",
                5,
                3,
                10,
                3
        );

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(Object[].class)
        )).thenReturn(0);

        when(passwordEncoder.encode("DatagenPassword123!"))
                .thenReturn("encoded-password");

        AtomicLong ids = new AtomicLong(1000);
        when(idGenerator.nextId())
                .thenAnswer(invocation -> ids.incrementAndGet());

        DataGenService service = new DataGenService(
                jdbcTemplate,
                passwordEncoder,
                idGenerator,
                properties
        );

        assertDoesNotThrow(service::generate);

        verify(jdbcTemplate, times(5)).update(
                argThat(sql -> sql.contains("insert into user_account")),
                any(Object[].class)
        );

        verify(jdbcTemplate, times(5)).update(
                argThat(sql -> sql.contains("insert into user_stat")),
                any(Object[].class)
        );

        verify(jdbcTemplate, times(3)).update(
                argThat(sql -> sql.contains("insert into category")),
                any(Object[].class)
        );

        verify(passwordEncoder).encode("DatagenPassword123!");
        verify(idGenerator, times(58)).nextId();
    }

    @Test
    void stopsWhenTheSameBatchAlreadyExists() {
        DataGenProperties properties = new DataGenProperties(
                20261126L,
                "datagen_20261126_",
                5,
                3,
                10,
                3
        );

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(Object[].class)
        )).thenReturn(1);

        DataGenService service = new DataGenService(
                jdbcTemplate,
                passwordEncoder,
                idGenerator,
                properties
        );

        assertThrows(
                IllegalStateException.class,
                service::generate
        );

        verify(jdbcTemplate, never()).update(
                anyString(),
                any(Object[].class)
        );

        verifyNoInteractions(passwordEncoder, idGenerator);
    }

    @Test
    void generatesTopicsWithIndependentFloorSequences() {
        DataGenProperties properties = new DataGenProperties(
                20261126L,
                "datagen_20261126_",
                5,
                3,
                2,
                2
        );

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(Object[].class)
        )).thenReturn(0);

        when(passwordEncoder.encode("DatagenPassword123!"))
                .thenReturn("encoded-password");

        AtomicLong ids = new AtomicLong(1000);

        when(idGenerator.nextId())
                .thenAnswer(invocation -> ids.incrementAndGet());

        DataGenService service = new DataGenService(
                jdbcTemplate,
                passwordEncoder,
                idGenerator,
                properties
        );

        service.generate();

        verify(jdbcTemplate, times(2)).update(
                argThat(sql -> sql.contains("insert into topic")),
                any(Object[].class)
        );

        verify(jdbcTemplate, times(6)).update(
                argThat(sql -> sql.contains("insert into reply")),
                any(Object[].class)
        );
    }

    @Test
    void generatedRepliesHaveOneIndependentFloorSequencePerTopic() {
        DataGenProperties properties = new DataGenProperties(
                20261126L,
                "datagen_20261126_",
                2,
                1,
                2,
                2
        );

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(Object[].class)
        )).thenReturn(0);

        when(passwordEncoder.encode("DatagenPassword123!"))
                .thenReturn("encoded-password");

        AtomicLong ids = new AtomicLong(1000);

        when(idGenerator.nextId())
                .thenAnswer(invocation -> ids.incrementAndGet());

        DataGenService service = new DataGenService(
                jdbcTemplate,
                passwordEncoder,
                idGenerator,
                properties
        );

        service.generate();

        ArgumentCaptor<Object[]> arguments =
                ArgumentCaptor.forClass(Object[].class);

        verify(jdbcTemplate, times(6)).update(
                argThat(sql -> sql.contains("insert into reply")),
                arguments.capture()
        );

        List<Object[]> replyArguments = arguments.getAllValues();

        assert replyArguments.get(0)[2].equals(1);
        assert replyArguments.get(1)[2].equals(2);
        assert replyArguments.get(2)[2].equals(3);

        assert replyArguments.get(3)[2].equals(1);
        assert replyArguments.get(4)[2].equals(2);
        assert replyArguments.get(5)[2].equals(3);

        assert !replyArguments.get(0)[1]
                .equals(replyArguments.get(3)[1]);
    }

}