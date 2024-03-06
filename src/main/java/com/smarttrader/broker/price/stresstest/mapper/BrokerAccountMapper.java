package com.smarttrader.broker.price.stresstest.mapper;


import com.smarttrader.broker.price.stresstest.domain.BrokerAccount;
import com.smarttrader.broker.price.stresstest.domain.BrokerAccountProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrokerAccountMapper {

    BrokerAccount toEntity(BrokerAccountProjection projection);
}
