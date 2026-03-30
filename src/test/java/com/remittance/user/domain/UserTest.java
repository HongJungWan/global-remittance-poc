package com.remittance.user.domain;

import com.remittance.user.domain.vo.Email;
import com.remittance.user.domain.vo.KycStatus;
import com.remittance.user.domain.vo.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("User 생성 시 기본 상태가 올바르게 설정된다")
    void create_setsDefaultValues() {
        User user = new User(new Email("test@example.com"), "hashedPw", "홍길동");

        assertNotNull(user.getId());
        assertEquals("test@example.com", user.getEmail().getValue());
        assertEquals("hashedPw", user.getPasswordHash());
        assertEquals("홍길동", user.getDisplayName());
        assertEquals(KycStatus.PENDING, user.getKycStatus());
        assertEquals(Role.CUSTOMER, user.getRole());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("프로필 업데이트 시 displayName과 updatedAt이 변경된다")
    void updateProfile_changesDisplayNameAndTimestamp() {
        User user = new User(new Email("test@example.com"), "hashedPw", "홍길동");
        var originalUpdatedAt = user.getUpdatedAt();

        user.updateProfile("김철수");

        assertEquals("김철수", user.getDisplayName());
        assertTrue(user.getUpdatedAt().compareTo(originalUpdatedAt) >= 0);
    }

    @Test
    @DisplayName("KYC 인증 성공 시 상태가 VERIFIED로 변경된다")
    void verifyKyc_changesStatusToVerified() {
        User user = new User(new Email("test@example.com"), "hashedPw", "홍길동");

        user.verifyKyc();

        assertEquals(KycStatus.VERIFIED, user.getKycStatus());
    }

    @Test
    @DisplayName("이미 VERIFIED 상태에서 verifyKyc 호출 시 예외 발생")
    void verifyKyc_throwsIfAlreadyVerified() {
        User user = new User(new Email("test@example.com"), "hashedPw", "홍길동");
        user.verifyKyc();

        assertThrows(IllegalStateException.class, user::verifyKyc);
    }

    @Test
    @DisplayName("KYC 거부 시 상태가 REJECTED로 변경된다")
    void rejectKyc_changesStatusToRejected() {
        User user = new User(new Email("test@example.com"), "hashedPw", "홍길동");

        user.rejectKyc();

        assertEquals(KycStatus.REJECTED, user.getKycStatus());
    }

    @Test
    @DisplayName("이미 REJECTED 상태에서 rejectKyc 호출 시 예외 발생")
    void rejectKyc_throwsIfAlreadyRejected() {
        User user = new User(new Email("test@example.com"), "hashedPw", "홍길동");
        user.rejectKyc();

        assertThrows(IllegalStateException.class, user::rejectKyc);
    }
}
