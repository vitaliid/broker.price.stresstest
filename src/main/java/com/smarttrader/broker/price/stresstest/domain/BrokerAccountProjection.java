package com.smarttrader.broker.price.stresstest.domain;

public interface BrokerAccountProjection {

    Long getBrokerId();

    String getAccount();

    Integer getType();
}
