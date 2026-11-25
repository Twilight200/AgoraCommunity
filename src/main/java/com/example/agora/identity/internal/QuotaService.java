package com.example.agora.identity.internal;

import com.example.agora.identity.api.QuotaApi;
import com.example.agora.identity.api.QuotaDecision;
import com.example.agora.platform.api.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class QuotaService implements QuotaApi {
    private final UserAccountRepository userAccountRepository;
    private final UserDailyQuotaRepository quotaRepository;
    private final QuotaPolicy quotaPolicy;
    private final Clock clock;

    @Autowired
    public QuotaService(
            UserAccountRepository userAccountRepository,
            UserDailyQuotaRepository quotaRepository
    ) {
        this(
                userAccountRepository,
                quotaRepository,
                new QuotaPolicy(),
                Clock.systemUTC()
        );
    }

    QuotaService(
            UserAccountRepository userAccountRepository,
            UserDailyQuotaRepository quotaRepository,
            QuotaPolicy quotaPolicy,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.quotaRepository = quotaRepository;
        this.quotaPolicy = quotaPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public QuotaDecision tryConsumeTopicCreation(long userId, boolean containsLink){
        return tryConsume(userId, QuotaResource.TOPIC, containsLink);
    }

    @Override
    @Transactional
    public QuotaDecision tryConsumeReplyCreation(long userId, boolean containsLink) {
        return tryConsume(userId, QuotaResource.REPLY, containsLink);
    }

    private QuotaDecision tryConsume(long userId, QuotaResource resource, boolean containsLink){
        int trustLevel = userAccountRepository.findTrustLevelById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "IDENTITY_USER_NOT_FOUND",
                        "user does not exist"
                ));

        LocalDate quotaDate = LocalDate.now(clock);

        UserDailyQuotaCount count = quotaRepository.lockOrCreate(userId, quotaDate);

        int usedCount = switch (resource){
            case REPLY -> count.repliesCreated();
            case TOPIC -> count.topicsCreated();
        };

        QuotaDecision decision = quotaPolicy.decide(trustLevel,resource,usedCount,containsLink);

        if(!decision.allowed()) return decision;

        quotaRepository.increment(resource,userId,quotaDate);
        return decision;
    }
}
