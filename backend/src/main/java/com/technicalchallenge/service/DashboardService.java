package com.technicalchallenge.service;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.repository.DashboardRepository;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.repository.TradeSpecifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final TradeRepository tradeRepository;
    private final ApplicationUserRepository applicationUserRepository;

    public DashboardService(
            DashboardRepository dashboardRepository,
            TradeRepository tradeRepository,
            ApplicationUserRepository applicationUserRepository) {
        this.dashboardRepository = dashboardRepository;
        this.tradeRepository = tradeRepository;
        this.applicationUserRepository = applicationUserRepository;
    }

    public Page<Trade> getMyTrades(String performedBy, Pageable pageable) {
        Long traderId = resolveUserId(performedBy);
        if (traderId == null) {
            return Page.empty(pageable);
        }
        return tradeRepository.findAll(
                TradeSpecifications.activeTrue()
                        .and(TradeSpecifications.traderIdEquals(traderId)),
                pageable);
    }

    public Page<Trade> getTradesByBook(Long bookId, Pageable pageable) {
        return tradeRepository.findAll(
                TradeSpecifications.activeTrue()
                        .and(TradeSpecifications.bookIdEquals(bookId)),
                pageable);
    }

    public TradeSummaryDTO buildTraderSummary(String performedBy, LocalDate from, LocalDate to) {
        Long traderId = resolveUserId(performedBy);
        if (traderId == null)
            return emptySummary(performedBy, from, to);

        TradeSummaryDTO dto = new TradeSummaryDTO();
        dto.setTrader(performedBy);
        dto.setFromDate(from);
        dto.setToDate(to);

        dto.setTradesByStatus(
                toStringLongMap(dashboardRepository.countByStatusForTrader(traderId, from, to)));

        dto.setNotionalByCurrency(
                toStringBigDecimalMap(dashboardRepository.sumNotionalByCurrencyForTrader(traderId, from, to)));

        dto.setTradesByType(
                toStringLongMap(dashboardRepository.countByTypeForTrader(traderId, from, to)));
        dto.setTradesByCounterparty(
                toStringLongMap(dashboardRepository.countByCounterpartyForTrader(traderId, from, to)));
        dto.setNotionalByCounterparty(
                toStringBigDecimalMap(dashboardRepository.sumNotionalByCounterpartyForTrader(traderId, from, to)));

        dto.setNotionalByBookTop(
                toStringBigDecimalMap(dashboardRepository.topBooksByNotionalForTrader(traderId, from, to)));

        Object[] totals = dashboardRepository.totalAndMostRecentForTrader(traderId, from, to);
        if (totals != null && totals.length >= 2) {
            dto.setTotalTrades(((Number) totals[0]).longValue());
            dto.setMostRecentTradeDate((LocalDate) totals[1]);
        }

        dto.setRiskExposures(Collections.emptyMap());
        dto.setWarnings(Collections.emptyList());

        return dto;
    }

    public DailySummaryDTO buildDailySummary(String performedBy, LocalDate asOfDate) {
        Long traderId = resolveUserId(performedBy);
        if (traderId == null)
            return emptyDaily(performedBy, asOfDate);

        LocalDate today = (asOfDate != null) ? asOfDate : LocalDate.now();
        LocalDate prevDay = today.minusDays(1);

        DailySummaryDTO dto = new DailySummaryDTO();
        dto.setTrader(performedBy);
        dto.setAsOfDate(today);

        long todayCount = dashboardRepository.countTradesForTraderOn(traderId, today);
        BigDecimal todayNotional = nvl(dashboardRepository.sumNotionalForTraderOn(traderId, today));

        dto.setTodayTradeCount(todayCount);
        dto.setTodayTotalNotional(todayNotional);
        dto.setTodayTradesByBook(
                toStringLongMap(dashboardRepository.countTodayByBookForTrader(traderId, today)));
        dto.setTodayNotionalByCurrency(
                toStringBigDecimalMap(dashboardRepository.sumTodayByCurrencyForTrader(traderId, today)));

        long prevCount = dashboardRepository.countTradesForTraderOn(traderId, prevDay);
        BigDecimal prevNotional = nvl(dashboardRepository.sumNotionalForTraderOn(traderId, prevDay));

        dto.setPrevDayTradeCount(prevCount);
        dto.setPrevDayTotalNotional(prevNotional);

        long countDelta = todayCount - prevCount;
        dto.setTradeCountDelta(countDelta);
        dto.setTradeCountDeltaPercentage(safePct(countDelta, prevCount));

        BigDecimal notionalDelta = todayNotional.subtract(prevNotional);
        dto.setNotionalDelta(notionalDelta);
        dto.setNotionalDeltaPercentage(safePct(notionalDelta, prevNotional));

        return dto;
    }

    private Long resolveUserId(String performedBy) {
        if (performedBy == null || performedBy.isBlank())
            return null;
        // try numeric id first
        try {
            long id = Long.parseLong(performedBy);
            return applicationUserRepository.findById(id)
                    .map(ApplicationUser::getId)
                    .orElse(null);
        } catch (NumberFormatException ignore) {
            // not numeric; treat as loginId
            return applicationUserRepository.findByLoginId(performedBy.toLowerCase(Locale.ROOT))
                    .map(ApplicationUser::getId)
                    .orElse(null);
        }
    }

    private TradeSummaryDTO emptySummary(String who, LocalDate from, LocalDate to) {
        TradeSummaryDTO dto = new TradeSummaryDTO();
        dto.setTrader(who);
        dto.setFromDate(from);
        dto.setToDate(to);
        dto.setTradesByStatus(Collections.emptyMap());
        dto.setNotionalByCurrency(Collections.emptyMap());
        dto.setTradesByType(Collections.emptyMap());
        dto.setTradesByCounterparty(Collections.emptyMap());
        dto.setNotionalByCounterparty(Collections.emptyMap());
        dto.setNotionalByBookTop(Collections.emptyMap());
        dto.setTotalTrades(0L);
        dto.setRiskExposures(Collections.emptyMap());
        dto.setWarnings(Collections.emptyList());
        return dto;
    }

    private DailySummaryDTO emptyDaily(String who, LocalDate asOfDate) {
        DailySummaryDTO dto = new DailySummaryDTO();
        dto.setTrader(who);
        dto.setAsOfDate(asOfDate != null ? asOfDate : LocalDate.now());
        dto.setTodayTradeCount(0);
        dto.setTodayTotalNotional(BigDecimal.ZERO);
        dto.setTodayTradesByBook(Collections.emptyMap());
        dto.setTodayNotionalByCurrency(Collections.emptyMap());
        dto.setPrevDayTradeCount(0);
        dto.setPrevDayTotalNotional(BigDecimal.ZERO);
        dto.setTradeCountDelta(0L);
        dto.setTradeCountDeltaPercentage(0.0);
        dto.setNotionalDelta(BigDecimal.ZERO);
        dto.setNotionalDeltaPercentage(0.0);
        return dto;
    }

    private Map<String, Long> toStringLongMap(List<Object[]> tuples) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (tuples == null)
            return map;
        for (Object[] row : tuples) {
            String key = (row[0] == null) ? "UNKNOWN" : String.valueOf(row[0]);
            Long val = (row[1] == null) ? 0L : ((Number) row[1]).longValue();
            map.put(key, val);
        }
        return map;
    }

    private Map<String, BigDecimal> toStringBigDecimalMap(List<Object[]> tuples) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (tuples == null)
            return map;
        for (Object[] row : tuples) {
            String key = (row[0] == null) ? "UNKNOWN" : String.valueOf(row[0]);
            BigDecimal val = (row[1] == null) ? BigDecimal.ZERO : (BigDecimal) row[1];
            map.put(key, val);
        }
        return map;
    }

    private BigDecimal nvl(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }

    private Double safePct(long delta, long base) {
        if (base == 0L)
            return (delta == 0L) ? 0.0 : 100.0;
        return (delta * 100.0) / base;
    }

    private Double safePct(BigDecimal delta, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) ? 0.0 : 100.0;
        }
        return delta
                .multiply(BigDecimal.valueOf(100))
                .divide(base, 4, RoundingMode.HALF_UP)
                .doubleValue();
    }
}