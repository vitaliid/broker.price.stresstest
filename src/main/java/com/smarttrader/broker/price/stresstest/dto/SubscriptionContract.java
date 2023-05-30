package com.smarttrader.broker.price.stresstest.dto;

import lombok.Data;

@Data
public class SubscriptionContract {
    private long account;
    private long brokerId;
    private String symbol;
}
