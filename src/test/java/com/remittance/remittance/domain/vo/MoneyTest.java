package com.remittance.remittance.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    @DisplayName("유효한 금액과 통화로 Money 생성이 성공한다")
    void create_success() {
        Money money = new Money(new BigDecimal("1000.50"), "KRW");
        assertEquals(new BigDecimal("1000.50"), money.getAmount());
        assertEquals("KRW", money.getCurrency());
    }

    @Test
    @DisplayName("통화 코드는 대문자로 정규화된다")
    void currency_isNormalizedToUppercase() {
        Money money = new Money(BigDecimal.TEN, "usd");
        assertEquals("USD", money.getCurrency());
    }

    @Test
    @DisplayName("음수 금액은 예외를 발생시킨다")
    void negativeAmount_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Money(new BigDecimal("-1"), "KRW"));
    }

    @Test
    @DisplayName("null 금액은 예외를 발생시킨다")
    void nullAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Money(null, "KRW"));
    }

    @Test
    @DisplayName("잘못된 통화 코드는 예외를 발생시킨다")
    void invalidCurrency_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Money(BigDecimal.TEN, "KRWW"));
        assertThrows(IllegalArgumentException.class,
                () -> new Money(BigDecimal.TEN, null));
    }

    @Test
    @DisplayName("0원은 유효하다")
    void zeroAmount_isValid() {
        Money money = new Money(BigDecimal.ZERO, "USD");
        assertEquals(BigDecimal.ZERO, money.getAmount());
    }

    @Test
    @DisplayName("같은 금액·통화는 equals가 true이다")
    void sameAmountAndCurrency_areEqual() {
        Money a = new Money(new BigDecimal("100.00"), "USD");
        Money b = new Money(new BigDecimal("100.00"), "USD");
        assertEquals(a, b);
    }

    @Test
    @DisplayName("다른 통화는 equals가 false이다")
    void differentCurrency_areNotEqual() {
        Money a = new Money(BigDecimal.TEN, "USD");
        Money b = new Money(BigDecimal.TEN, "KRW");
        assertNotEquals(a, b);
    }
}
