package com.smarttrader.broker.price.stresstest.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Broker {
    @Id
    private Long id;
}
