package com.smarttrader.broker.price.stresstest.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@ToString(of = {"brokerId", "account", "type"})
public class BrokerAccount implements Serializable {

    private Long brokerId;

    private Long account;

    private Integer type;//0 = mt4, 1 = mt5, 2 = FXDD

    private Set<String> symbols;
}
