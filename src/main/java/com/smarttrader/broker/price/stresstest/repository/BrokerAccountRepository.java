package com.smarttrader.broker.price.stresstest.repository;


import com.smarttrader.broker.price.stresstest.domain.Broker;
import com.smarttrader.broker.price.stresstest.domain.BrokerAccountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface BrokerAccountRepository extends JpaRepository<Broker, Long> {

    @Query(value = """
            SELECT br_cr.BROKER_ID      brokerId,
                   br_cr.BROKER_ACCT_ID account,
                   br.TYPE              type
            FROM BROKER br
                     LEFT JOIN BROKER_CREDENTIAL br_cr ON br.ID = br_cr.BROKER_ID
                     JOIN broker_subscr br_subscr ON br_cr.ACCT_ID = br_subscr.ACCT_ID
            --WHERE br.ACCT_TYPE = 'D'
              AND br_cr.BROKER_ACCT_ID IS NOT NULL
              AND br_cr.ONLINE_FLG = 'Y'
              AND br_cr.CONNECTED_FLG = 'Y'
              AND br_cr.BROKER_ID NOT IN (:blacklistedBrokerIds)
              AND br_cr.BROKER_ACCT_ID NOT IN (:invalidAccounts)
            GROUP BY br_cr.BROKER_ID, br_cr.BROKER_ACCT_ID, br.TYPE, br_cr.ACCT_ID
            ORDER BY brokerId, account
            OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
            """
            , nativeQuery = true)
    List<BrokerAccountProjection> findDemoBrokerAccounts(Integer limit, Integer offset, List<Integer> blacklistedBrokerIds, List<String> invalidAccounts);

    @Query(value = """
            SELECT br_subscr.BROKER_SYMBOL symbol
            FROM BROKER_CREDENTIAL br_cr
                     LEFT JOIN broker_subscr br_subscr ON br_cr.ACCT_ID = br_subscr.ACCT_ID
            WHERE br_cr.BROKER_ID = :brokerId
              AND br_cr.BROKER_ACCT_ID = :account
            ORDER BY symbol
            """
            , nativeQuery = true)
    Set<String> findSymbols(Long brokerId, String account);
}
