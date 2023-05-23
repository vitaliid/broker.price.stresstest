package com.smarttrader.broker.price.stresstest.dto.proto;

import lombok.Data;

@Data
public class SubscriptionContract {
    private long account;
    private long brokerId;
    private String symbol;
}
