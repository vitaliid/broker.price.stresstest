package com.smarttrader.broker.price.stresstest.service;


import com.smarttrader.broker.price.stresstest.domain.BrokerAccount;
import com.smarttrader.broker.price.stresstest.domain.BrokerAccountProjection;
import com.smarttrader.broker.price.stresstest.mapper.BrokerAccountMapper;
import com.smarttrader.broker.price.stresstest.repository.BrokerAccountRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class BrokerAccountService {

    private final BrokerAccountRepository brokerAccountRepository;

    private final BrokerAccountMapper brokerAccountMapper;

    private final Set<String> currencies = Set.of("USD",
            "HKD",
            "EUR",
            "BTC",
            "USDT",
            "LTC",
            "JPY",
            "ZAR",
            "AUD",
            "GBP",
            "NZD",
            "SGD",
            "GBX",
            "CHF",
            "HUF",
            "NOK",
            //"RUB",
            "SEC",
            "TRY",
            "MXN",
            "XAU",
            "XAG",
            "CNH",
            "CAD");

    public List<BrokerAccount> get(Integer pageNumber, Integer pageSize, List<Integer> blacklistedBrokerIds, List<String> invalidAccounts) {
        Integer limit = pageSize;
        Integer offset = pageNumber * pageSize;

        List<BrokerAccountProjection> projections = brokerAccountRepository.findDemoBrokerAccounts(limit, offset, blacklistedBrokerIds, invalidAccounts);
        log.info("Loaded accounts from db: {} ", projections.size());

        final AtomicInteger readyAccounts = new AtomicInteger();
        return projections.stream()
                //to avoid dirty data after QA actions
                .filter(brokerAccountProjection -> brokerAccountProjection.getAccount().chars().allMatch(Character::isDigit))
                .map(brokerAccountProjection -> {
                    BrokerAccount account = brokerAccountMapper.toEntity(brokerAccountProjection);


                    //load symbols separately, because it has a lot of them on production
                    Set<String> symbols = brokerAccountRepository.findSymbols(account.getBrokerId(), account.getAccount().toString());

                    //filter forex only symbols
                    Set<String> forexOnlySymbols = symbols.stream()
                            .filter(symbol -> currencies.stream()
                                    .filter(symbol::contains)
                                    .count() > 1)
                            .collect(Collectors.toSet());
                    account.setSymbols(forexOnlySymbols);

                    readyAccounts.getAndIncrement();
                    log.info("Loaded symbols for accounts {}/{}", readyAccounts.get(), projections.size());

                    return account;
                }).toList();
    }
}
