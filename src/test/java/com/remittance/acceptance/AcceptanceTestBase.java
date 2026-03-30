package com.remittance.acceptance;

import com.remittance.partner.application.PartnerIntegrationService;
import com.remittance.partner.infrastructure.mock.ConfigurableMockPartnerClient;
import com.remittance.partner.infrastructure.mock.MockConfig;
import com.remittance.payment.application.PaymentService;
import com.remittance.remittance.application.RemittanceService;
import com.remittance.remittance.infrastructure.RemittanceOrderRepository;
import com.remittance.shared.outbox.OutboxEventPublisher;
import com.remittance.user.application.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 인수 테스트 기반 클래스.
 * Kafka/Redis/Redisson 자동구성을 제외하고, Mock으로 대체하여 E2E 플로우를 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                "org.redisson.spring.starter.RedissonAutoConfigurationV2"
})
public abstract class AcceptanceTestBase {

    @Autowired
    protected AuthService authService;

    @Autowired
    protected RemittanceService remittanceService;

    @Autowired
    protected RemittanceOrderRepository remittanceOrderRepository;

    @Autowired
    protected PaymentService paymentService;

    @Autowired
    protected PartnerIntegrationService partnerIntegrationService;

    @Autowired
    protected ConfigurableMockPartnerClient mockPartnerClient;

    @MockBean
    protected RedisTemplate<String, Object> redisTemplate;

    @MockBean
    protected KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    protected OutboxEventPublisher outboxEventPublisher;

    protected void setPartnerMockMode(MockConfig.Mode mode) {
        mockPartnerClient.getConfig().setMode(mode);
        mockPartnerClient.getConfig().setDelayMs(0);
    }
}
