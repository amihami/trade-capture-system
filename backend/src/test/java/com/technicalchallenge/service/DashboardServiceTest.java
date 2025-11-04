package com.technicalchallenge.service;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.repository.DashboardRepository;
import com.technicalchallenge.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private ApplicationUserRepository applicationUserRepository;

    @InjectMocks
    private DashboardService service;

    private ApplicationUser user42;
    private ApplicationUser user7;

    @BeforeEach
    void setUp() {
        user42 = new ApplicationUser();
        user42.setId(42L);
        user42.setLoginId("forty_two");

        user7 = new ApplicationUser();
        user7.setId(7L);
        user7.setLoginId("tradera");
    }

    @Test
    void buildTraderSummary_aggregatesAllBuckets_andTotals() {
        String performedBy = "traderA";
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(applicationUserRepository.findByLoginId("tradera"))
                .thenReturn(Optional.of(user7));

        Long traderId = 7L;

        List<Object[]> byStatus = new ArrayList<>();
        byStatus.add(new Object[] { "NEW", 10L });
        byStatus.add(new Object[] { "LIVE", 7L });

        List<Object[]> notionalByCcy = new ArrayList<>();
        notionalByCcy.add(new Object[] { "USD", new BigDecimal("1000000") });
        notionalByCcy.add(new Object[] { "EUR", new BigDecimal("500000") });

        List<Object[]> byType = new ArrayList<>();
        byType.add(new Object[] { "IRS", 8L });
        byType.add(new Object[] { "CDS", 9L });

        List<Object[]> byCpCount = new ArrayList<>();
        byCpCount.add(new Object[] { "ABC", 9L });
        byCpCount.add(new Object[] { "XYZ", 8L });

        List<Object[]> byCpNotional = new ArrayList<>();
        byCpNotional.add(new Object[] { "ABC", new BigDecimal("900000") });
        byCpNotional.add(new Object[] { "XYZ", new BigDecimal("600000") });

        List<Object[]> topBooks = new ArrayList<>();
        topBooks.add(new Object[] { "Rates", new BigDecimal("1500000") });

        when(dashboardRepository.countByStatusForTrader(traderId, from, to)).thenReturn(byStatus);
        when(dashboardRepository.sumNotionalByCurrencyForTrader(traderId, from, to)).thenReturn(notionalByCcy);
        when(dashboardRepository.countByTypeForTrader(traderId, from, to)).thenReturn(byType);
        when(dashboardRepository.countByCounterpartyForTrader(traderId, from, to)).thenReturn(byCpCount);
        when(dashboardRepository.sumNotionalByCounterpartyForTrader(traderId, from, to)).thenReturn(byCpNotional);
        when(dashboardRepository.topBooksByNotionalForTrader(traderId, from, to)).thenReturn(topBooks);
        when(dashboardRepository.totalAndMostRecentForTrader(traderId, from, to))
                .thenReturn(new Object[] { 17L, LocalDate.of(2025, 10, 1) });

        TradeSummaryDTO dto = service.buildTraderSummary(performedBy, from, to);

        assertThat(dto.getTrader()).isEqualTo("traderA");
        assertThat(dto.getFromDate()).isEqualTo(from);
        assertThat(dto.getToDate()).isEqualTo(to);
        assertThat(dto.getTotalTrades()).isEqualTo(17L);
        assertThat(dto.getMostRecentTradeDate()).isEqualTo(LocalDate.of(2025, 10, 1));
        assertThat(dto.getTradesByStatus()).containsEntry("NEW", 10L).containsEntry("LIVE", 7L);
        assertThat(dto.getNotionalByCurrency()).containsEntry("USD", new BigDecimal("1000000"))
                .containsEntry("EUR", new BigDecimal("500000"));
        assertThat(dto.getTradesByType()).containsEntry("IRS", 8L).containsEntry("CDS", 9L);
        assertThat(dto.getTradesByCounterparty()).containsEntry("ABC", 9L).containsEntry("XYZ", 8L);
        assertThat(dto.getNotionalByCounterparty()).containsEntry("ABC", new BigDecimal("900000"))
                .containsEntry("XYZ", new BigDecimal("600000"));
        assertThat(dto.getNotionalByBookTop()).containsEntry("Rates", new BigDecimal("1500000"));
    }

    @Test
    void buildTraderSummary_returnsEmpty_whenUserNotFound() {
        String performedBy = "unknown_login";
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(applicationUserRepository.findByLoginId("unknown_login")).thenReturn(Optional.empty());

        TradeSummaryDTO dto = service.buildTraderSummary(performedBy, from, to);

        assertThat(dto.getTrader()).isEqualTo("unknown_login");
        assertThat(dto.getTotalTrades()).isZero();
        assertThat(dto.getTradesByStatus()).isEmpty();
        assertThat(dto.getNotionalByCurrency()).isEmpty();
        assertThat(dto.getNotionalByBookTop()).isEmpty();
    }

    @Test
    void buildDailySummary_computesDeltas_andHandlesZeroBase() {
        String performedBy = "42";
        LocalDate asOf = LocalDate.of(2025, 10, 1);
        Long traderId = 42L;

        when(applicationUserRepository.findById(traderId)).thenReturn(Optional.of(user42));

        when(dashboardRepository.countTradesForTraderOn(traderId, asOf)).thenReturn(10L);
        when(dashboardRepository.sumNotionalForTraderOn(traderId, asOf))
                .thenReturn(new BigDecimal("1000000"));

        List<Object[]> todayByBook = new ArrayList<>();
        todayByBook.add(new Object[] { "Rates", 6L });
        todayByBook.add(new Object[] { "Credit", 4L });
        when(dashboardRepository.countTodayByBookForTrader(traderId, asOf)).thenReturn(todayByBook);

        List<Object[]> todayByCcy = new ArrayList<>();
        todayByCcy.add(new Object[] { "USD", new BigDecimal("700000") });
        todayByCcy.add(new Object[] { "EUR", new BigDecimal("300000") });
        when(dashboardRepository.sumTodayByCurrencyForTrader(traderId, asOf)).thenReturn(todayByCcy);

        LocalDate prev = asOf.minusDays(1);
        when(dashboardRepository.countTradesForTraderOn(traderId, prev)).thenReturn(0L);
        when(dashboardRepository.sumNotionalForTraderOn(traderId, prev)).thenReturn(BigDecimal.ZERO);

        DailySummaryDTO dto = service.buildDailySummary(performedBy, asOf);

        assertThat(dto.getTrader()).isEqualTo("42");
        assertThat(dto.getAsOfDate()).isEqualTo(asOf);
        assertThat(dto.getTodayTradeCount()).isEqualTo(10);
        assertThat(dto.getTodayTotalNotional()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(dto.getPrevDayTradeCount()).isZero();
        assertThat(dto.getPrevDayTotalNotional()).isZero();
        assertThat(dto.getTradeCountDelta()).isEqualTo(10);
        assertThat(dto.getTradeCountDeltaPercentage()).isEqualTo(100.0);
        assertThat(dto.getNotionalDelta()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(dto.getNotionalDeltaPercentage()).isEqualTo(100.0);

        assertThat(dto.getTodayTradesByBook()).containsEntry("Rates", 6L)
                .containsEntry("Credit", 4L);
        assertThat(dto.getTodayNotionalByCurrency()).containsEntry("USD", new BigDecimal("700000"))
                .containsEntry("EUR", new BigDecimal("300000"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMyTrades_resolvesLoginId_andReturnsPage() {
        String performedBy = "traderA";
        when(applicationUserRepository.findByLoginId("tradera"))
                .thenReturn(Optional.of(user7));

        Trade t1 = new Trade();
        t1.setTradeId(1001L);
        Trade t2 = new Trade();
        t2.setTradeId(1002L);
        List<Trade> content = List.of(t1, t2);
        Page<Trade> page = new PageImpl<>(content, PageRequest.of(0, 2), 2);

        when(tradeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<Trade> result = service.getMyTrades(performedBy, PageRequest.of(0, 2));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getTradeId()).isEqualTo(1001L);
        assertThat(result.getContent().get(1).getTradeId()).isEqualTo(1002L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTradesByBook_returnsPage() {
        Long bookId = 55L;

        Trade t1 = new Trade();
        t1.setTradeId(2001L);
        Trade t2 = new Trade();
        t2.setTradeId(2002L);
        Page<Trade> page = new PageImpl<>(List.of(t1, t2), PageRequest.of(0, 2), 2);

        when(tradeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<Trade> result = service.getTradesByBook(bookId, PageRequest.of(0, 2));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getTradeId()).isEqualTo(2001L);
        assertThat(result.getContent().get(1).getTradeId()).isEqualTo(2002L);
    }
}
