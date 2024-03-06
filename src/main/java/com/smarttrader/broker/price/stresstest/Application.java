package com.smarttrader.broker.price.stresstest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import reactor.util.concurrent.Queues;

import static reactor.netty.ReactorNetty.POOL_MAX_CONNECTIONS;

@Slf4j
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableEnvironment env = SpringApplication.run(Application.class, args).getEnvironment();
        log.info("""

                        ----------------------------------------------------------
                        \tApplication '{}' is running!\s
                        \tAccess URL: http://localhost:{}
                        \tProfile(s): {}
                        ----------------------------------------------------------""",
                env.getProperty("spring.application.name"),
                env.getProperty("server.port"),
                env.getActiveProfiles());

        log.info("The maximum number of in-flight inner sequences: system {} or netty {}", System.getProperty("reactor.bufferSize.small"), Queues.SMALL_BUFFER_SIZE);
        log.info("EventLoop threads: {}", System.getProperty("io.netty.eventLoopThreads"));
        log.info("Max connections: {}", System.getProperty(POOL_MAX_CONNECTIONS));
    }
}
