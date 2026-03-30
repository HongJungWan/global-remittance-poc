package com.remittance.remittance.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ReceiverInfo {

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String name;

    @Column(name = "receiver_account", nullable = false, length = 50)
    private String account;

    @Column(name = "receiver_bank_code", nullable = false, length = 20)
    private String bankCode;

    @Column(name = "receiver_country", nullable = false, length = 3)
    private String country;

    protected ReceiverInfo() {
    }

    public ReceiverInfo(String name, String account, String bankCode, String country) {
        this.name = name;
        this.account = account;
        this.bankCode = bankCode;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public String getAccount() {
        return account;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getCountry() {
        return country;
    }
}
