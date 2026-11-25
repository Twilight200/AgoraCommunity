package com.example.agora.identity.internal;

import com.example.agora.identity.api.LoginRequest;
import com.example.agora.identity.api.RegisterRequest;
import com.example.agora.platform.api.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class UserAccountMySqlIntegrationTest {

    private static final String USERNAME = "mapper_test_user";

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Container
    static final MySQLContainer MYSQL =
            new MySQLContainer("mysql:8.0.36")
                    .withDatabaseName("agora")
                    .withUsername("agora")
                    .withPassword("test-password");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionTokenService sessionTokenService;

    @AfterEach
    void removeTestAccount() {
        jdbcTemplate.update(
                "DELETE FROM user_account WHERE username = ?",
                USERNAME
        );
    }

    @Test
    void writesBcryptHashAndTl0DefaultsToMysql() {
        String rawPassword = "correct horse battery staple";

        userAccountService.register(
                new RegisterRequest(USERNAME, rawPassword)
        );

        var account = jdbcTemplate.queryForMap(
                """
                SELECT password_hash, trust_level, status
                FROM user_account
                WHERE username = ?
                """,
                USERNAME
        );

        String storedHash = (String) account.get("password_hash");

        assertThat(storedHash).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, storedHash)).isTrue();
        assertThat(((Number) account.get("trust_level")).intValue()).isZero();
        assertThat(((Number) account.get("status")).intValue()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateUsernameUsingMysqlUniqueConstraint() {
        var request = new RegisterRequest(
                USERNAME,
                "correct horse battery staple"
        );

        userAccountService.register(request);

        assertThatThrownBy(() -> userAccountService.register(request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus().value()).isEqualTo(409);
                    assertThat(apiException.getCode())
                            .isEqualTo("IDENTITY_USERNAME_TAKEN");
                });
    }

    @Test
    void findsAccountCredentialsByUsernameThroughMyBatis() {
        String rawPassword = "correct horse battery staple";

        userAccountService.register(
                new RegisterRequest(USERNAME, rawPassword)
        );

        var account = userAccountRepository.findByUsername(USERNAME)
                .orElseThrow();

        assertThat(account.id()).isPositive();
        assertThat(account.username()).isEqualTo(USERNAME);
        assertThat(account.status()).isEqualTo(1);
        assertThat(account.trustLevel()).isZero();
        assertThat(passwordEncoder.matches(rawPassword, account.passwordHash()))
                .isTrue();
    }

    @Test
    void loginStoresTokenHashAndLogoutRevokesSessionInMysql() {
        String rawPassword = "correct horse battery staple";

        // 注册并登录，拿到只应该返回给客户端的原始 token。
        userAccountService.register(new RegisterRequest(USERNAME, rawPassword));
        var loginResult = userAccountService.login(
                new LoginRequest(USERNAME, rawPassword)
        );
        String rawToken = loginResult.accessToken();

        // 数据库应该存 hash，而不是原始 token。
        String expectedTokenHash = sessionTokenService.hash(rawToken);
        String storedTokenHash = jdbcTemplate.queryForObject(
                """
                SELECT token_hash
                FROM user_session
                WHERE token_hash = ?
                """,
                String.class,
                expectedTokenHash
        );

        assertThat(storedTokenHash).isEqualTo(expectedTokenHash);
        assertThat(storedTokenHash).isNotEqualTo(rawToken);

        // 原始 token 可以通过 session 查询到对应用户。
        long expectedUserId = userAccountRepository.findByUsername(USERNAME)
                .orElseThrow()
                .id();

        assertThat(sessionService.authenticate(rawToken))
                .isEqualTo(expectedUserId);

        // 登出后，数据库应记录 revoked_at，原 token 也不再有效。
        sessionService.revoke(rawToken);

        Integer revoked = jdbcTemplate.queryForObject(
                """
                SELECT CASE WHEN revoked_at IS NULL THEN 0 ELSE 1 END
                FROM user_session
                WHERE token_hash = ?
                """,
                Integer.class,
                expectedTokenHash
        );

        assertThat(revoked).isEqualTo(1);

        assertThatThrownBy(() -> sessionService.authenticate(rawToken))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    var apiException = (ApiException) exception;
                    assertThat(apiException.getStatus().value()).isEqualTo(401);
                    assertThat(apiException.getCode())
                            .isEqualTo("IDENTITY_INVALID_SESSION");
                });
    }
}