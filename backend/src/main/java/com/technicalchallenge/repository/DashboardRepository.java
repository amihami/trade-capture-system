package com.technicalchallenge.repository;

import com.technicalchallenge.model.Trade;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DashboardRepository extends JpaRepository<Trade, Long> {

    @Query("""
                select t.tradeStatus.tradeStatus as status, count(t) as cnt
                from Trade t
                where t.active = true
                  and t.traderUser.id = :traderId
                  and (:fromDate is null or t.tradeDate >= :fromDate)
                  and (:toDate   is null or t.tradeDate <= :toDate)
                group by t.tradeStatus.tradeStatus
            """)
    List<Object[]> countByStatusForTrader(
            @Param("traderId") Long traderId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
                select curr.currency as currency, coalesce(sum(leg.notional), 0)
                from TradeLeg leg
                join leg.trade t
                left join leg.currency curr
                where t.active = true
                  and t.traderUser.id = :traderId
                  and (:fromDate is null or t.tradeDate >= :fromDate)
                  and (:toDate   is null or t.tradeDate <= :toDate)
                group by curr.currency
            """)
    List<Object[]> sumNotionalByCurrencyForTrader(
            @Param("traderId") Long traderId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
                select tt.tradeType as type, count(t)
                from Trade t
                left join t.tradeType tt
                where t.active = true
                  and t.traderUser.id = :traderId
                  and (:fromDate is null or t.tradeDate >= :fromDate)
                  and (:toDate   is null or t.tradeDate <= :toDate)
                group by tt.tradeType
            """)
    List<Object[]> countByTypeForTrader(
            @Param("traderId") Long traderId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
                select cp.name as counterparty, count(t)
                from Trade t
                left join t.counterparty cp
                where t.active = true
                  and t.traderUser.id = :traderId
                  and (:fromDate is null or t.tradeDate >= :fromDate)
                  and (:toDate   is null or t.tradeDate <= :toDate)
                group by cp.name
            """)
    List<Object[]> countByCounterpartyForTrader(
            @Param("traderId") Long traderId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
                select cp.name as counterparty, coalesce(sum(leg.notional), 0)
                from TradeLeg leg
                join leg.trade t
                left join t.counterparty cp
                where t.active = true
                  and t.traderUser.id = :traderId
                  and (:fromDate is null or t.tradeDate >= :fromDate)
                  and (:toDate   is null or t.tradeDate <= :toDate)
                group by cp.name
            """)
    List<Object[]> sumNotionalByCounterpartyForTrader(
            @Param("traderId") Long traderId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
                select b.bookName as book, coalesce(sum(leg.notional), 0)
                from TradeLeg leg
                join leg.trade t
                left join t.book b
                where t.active = true
                  and t.traderUser.id = :traderId
                  and (:fromDate is null or t.tradeDate >= :fromDate)
                  and (:toDate   is null or t.tradeDate <= :toDate)
                group by b.bookName
                order by coalesce(sum(leg.notional), 0) desc
            """)
    List<Object[]> topBooksByNotionalForTrader(
            @Param("traderId") Long traderId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
                select count(t), max(t.tradeDate)
                from Trade t
                where t.active = true
                  and t.traderUser.id = :traderId
                  and (:fromDate is null or t.tradeDate >= :fromDate)
                  and (:toDate   is null or t.tradeDate <= :toDate)
            """)
    Object[] totalAndMostRecentForTrader(
            @Param("traderId") Long traderId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
                select count(t)
                from Trade t
                where t.active = true
                  and t.traderUser.id = :traderId
                  and t.tradeDate = :onDate
            """)
    long countTradesForTraderOn(
            @Param("traderId") Long traderId,
            @Param("onDate") LocalDate onDate);

    @Query("""
                select coalesce(sum(leg.notional), 0)
                from TradeLeg leg
                join leg.trade t
                where t.active = true
                  and t.traderUser.id = :traderId
                  and t.tradeDate = :onDate
            """)
    BigDecimal sumNotionalForTraderOn(
            @Param("traderId") Long traderId,
            @Param("onDate") LocalDate onDate);

    @Query("""
                select b.bookName, count(t)
                from Trade t
                left join t.book b
                where t.active = true
                  and t.traderUser.id = :traderId
                  and t.tradeDate = :onDate
                group by b.bookName
            """)
    List<Object[]> countTodayByBookForTrader(
            @Param("traderId") Long traderId,
            @Param("onDate") LocalDate onDate);

    @Query("""
                select curr.currency, coalesce(sum(leg.notional), 0)
                from TradeLeg leg
                join leg.trade t
                left join leg.currency curr
                where t.active = true
                  and t.traderUser.id = :traderId
                  and t.tradeDate = :onDate
                group by curr.currency
            """)
    List<Object[]> sumTodayByCurrencyForTrader(
            @Param("traderId") Long traderId,
            @Param("onDate") LocalDate onDate);

    @Query("""
                select t
                from Trade t
                where t.active = true
                  and t.book.id = :bookId
            """)
    List<Trade> findAllByBookId(@Param("bookId") Long bookId);
}