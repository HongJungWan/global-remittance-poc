package com.remittance.user.application;

import com.remittance.shared.event.UserCreatedEvent;
import com.remittance.shared.outbox.OutboxEventPublisher;
import com.remittance.user.domain.User;
import com.remittance.user.domain.vo.Email;
import com.remittance.user.infrastructure.JwtTokenProvider;
import com.remittance.user.infrastructure.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final String SCHEMA = "fintech_user";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OutboxEventPublisher outboxEventPublisher;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, OutboxEventPublisher outboxEventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public AuthResult register(String email, String password, String displayName) {
        if (userRepository.existsByEmailValue(email.toLowerCase().trim())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + email);
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(new Email(email), encodedPassword, displayName);
        userRepository.save(user);

        UserCreatedEvent event = new UserCreatedEvent(
                user.getId(), user.getDisplayName(), user.getKycStatus().name());
        outboxEventPublisher.publish(SCHEMA, "User", user.getId(), event);

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail().getValue(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(), user.getEmail().getValue(), user.getRole().name());

        return new AuthResult(user.getId(), accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmailValue(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail().getValue(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(), user.getEmail().getValue(), user.getRole().name());

        return new AuthResult(user.getId(), accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResult refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        UUID userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail().getValue(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(), user.getEmail().getValue(), user.getRole().name());

        return new AuthResult(user.getId(), newAccessToken, newRefreshToken);
    }

    public record AuthResult(UUID userId, String accessToken, String refreshToken) {
    }
}
