package com.example.MyProject.ORDER.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SePayConfig {

    @Value("${sepay.bank-name:MBBank}")
    private String bankName;

    @Value("${sepay.account-number:0399999999}")
    private String accountNumber;

    @Value("${sepay.account-name:HOANG 89 COMPUTER}")
    private String accountName;

    @Value("${sepay.webhook-api-key:}")
    private String webhookApiKey;

    @Value("${sepay.payment-prefix:DH}")
    private String paymentPrefix;
}
