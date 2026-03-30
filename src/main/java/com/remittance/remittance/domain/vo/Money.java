package com.remittance.remittance.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class Money {

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    protected Money() {
    }

    public Money(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다: " + amount);
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("통화 코드는 3자리여야 합니다: " + currency);
        }
        this.amount = amount;
        this.currency = currency.toUpperCase();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return 31 * amount.stripTrailingZeros().hashCode() + currency.hashCode();
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
