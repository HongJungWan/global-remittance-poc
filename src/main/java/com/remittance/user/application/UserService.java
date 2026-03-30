package com.remittance.user.application;

import com.remittance.shared.event.UserUpdatedEvent;
import com.remittance.shared.outbox.OutboxEventPublisher;
import com.remittance.user.domain.User;
import com.remittance.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final String SCHEMA = "fintech_user";

    private final UserRepository userRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    public UserService(UserRepository userRepository, OutboxEventPublisher outboxEventPublisher) {
        this.userRepository = userRepository;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    @Transactional
    public User updateProfile(UUID userId, String displayName) {
        User user = getUser(userId);
        user.updateProfile(displayName);
        userRepository.save(user);

        UserUpdatedEvent event = new UserUpdatedEvent(
                user.getId(), user.getDisplayName(), user.getKycStatus().name());
        outboxEventPublisher.publish(SCHEMA, "User", user.getId(), event);

        return user;
    }
}
