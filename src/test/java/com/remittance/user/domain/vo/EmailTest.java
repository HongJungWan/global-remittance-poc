package com.remittance.user.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    @DisplayName("유효한 이메일로 Email 생성이 성공한다")
    void validEmail_createsSuccessfully() {
        Email email = new Email("user@example.com");
        assertEquals("user@example.com", email.getValue());
    }

    @Test
    @DisplayName("이메일은 소문자로 정규화된다")
    void email_isNormalizedToLowercase() {
        Email email = new Email("User@EXAMPLE.COM");
        assertEquals("user@example.com", email.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "no-at-sign", "@no-local", "no-domain@"})
    @DisplayName("잘못된 이메일 형식은 예외를 발생시킨다")
    void invalidEmail_throwsException(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new Email(invalid));
    }

    @Test
    @DisplayName("null 이메일은 예외를 발생시킨다")
    void nullEmail_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }

    @Test
    @DisplayName("동일한 이메일 값은 equals가 true이다")
    void sameValue_areEqual() {
        Email email1 = new Email("test@example.com");
        Email email2 = new Email("test@example.com");
        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
    }

    @Test
    @DisplayName("다른 이메일 값은 equals가 false이다")
    void differentValue_areNotEqual() {
        Email email1 = new Email("a@example.com");
        Email email2 = new Email("b@example.com");
        assertNotEquals(email1, email2);
    }
}
