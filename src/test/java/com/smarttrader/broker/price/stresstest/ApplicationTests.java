package com.smarttrader.broker.price.stresstest;

import com.smarttrader.broker.price.stresstest.repository.BrokerAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
class ApplicationTests {

    @MockBean
    private BrokerAccountRepository brokerAccountRepository;

    @Test
    void contextLoads() {
    }

}
