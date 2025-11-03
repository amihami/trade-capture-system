package com.technicalchallenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.service.DashboardService;
import com.technicalchallenge.service.TradeValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
public class DashboardControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DashboardService dashboardService;
        @MockBean
        private TradeMapper tradeMapper;
        @MockBean
        private TradeValidationService tradeValidationService;

        private Trade trade1;
        private Trade trade2;
        private TradeDTO dto1;
        private TradeDTO dto2;
        private Page<Trade> page;

        @BeforeEach
        void setup() {
                new ObjectMapper().registerModule(new JavaTimeModule());

                trade1 = new Trade();
                trade1.setTradeId(1001L);
                trade1.setTradeDate(LocalDate.now());

                trade2 = new Trade();
                trade2.setTradeId(1002L);
                trade2.setTradeDate(LocalDate.now());

                dto1 = new TradeDTO();
                dto1.setTradeId(1001L);
                dto1.setBookName("Rates");
                dto1.setCounterpartyName("ABC");

                dto2 = new TradeDTO();
                dto2.setTradeId(1002L);
                dto2.setBookName("Credit");
                dto2.setCounterpartyName("XYZ");

                when(tradeMapper.toDto(trade1)).thenReturn(dto1);
                when(tradeMapper.toDto(trade2)).thenReturn(dto2);

                List<Trade> content = List.of(trade1, trade2);
                page = new PageImpl<>(content, PageRequest.of(0, 2, Sort.by("tradeId").descending()), content.size());
        }

        @Test
        void myTrades_returnsPagedTradesForTrader() throws Exception {
                when(dashboardService.getMyTrades(eq("traderA"), ArgumentMatchers.<Pageable>any()))
                                .thenReturn(page);

                mockMvc.perform(get("/api/dashboard/my-trades")
                                .param("performedBy", "traderA")
                                .param("page", "0")
                                .param("size", "2")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)))
                                .andExpect(jsonPath("$.content[0].tradeId", is(1001)))
                                .andExpect(jsonPath("$.content[1].tradeId", is(1002)));
        }

        @Test
        void bookTrades_returnsPagedTradesForBook() throws Exception {
                when(dashboardService.getTradesByBook(eq(5L), ArgumentMatchers.<Pageable>any()))
                                .thenReturn(page);

                mockMvc.perform(get("/api/dashboard/book/{id}/trades", 5L)
                                .param("page", "0")
                                .param("size", "2")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)))
                                .andExpect(jsonPath("$.content[0].bookName", is("Rates")))
                                .andExpect(jsonPath("$.content[1].bookName", is("Credit")));
        }

        @Test
        void summary_returnsAggregatePortfolioStats() throws Exception {
                TradeSummaryDTO summary = new TradeSummaryDTO();
                summary.setTrader("traderA");
                summary.setFromDate(LocalDate.of(2025, 1, 1));
                summary.setToDate(LocalDate.of(2025, 12, 31));
                summary.setTradesByStatus(Map.of("NEW", 10L, "LIVE", 7L));
                summary.setNotionalByCurrency(
                                Map.of("USD", new BigDecimal("1000000"), "EUR", new BigDecimal("500000")));
                summary.setTradesByType(Map.of("IRS", 8L, "CDS", 9L));
                summary.setTradesByCounterparty(Map.of("ABC", 9L, "XYZ", 8L));
                summary.setNotionalByCounterparty(
                                Map.of("ABC", new BigDecimal("900000"), "XYZ", new BigDecimal("600000")));
                summary.setRiskExposures(Map.of("DV01", new BigDecimal("125000")));
                summary.setTotalTrades(17L);
                summary.setMostRecentTradeDate(LocalDate.of(2025, 10, 1));

                when(dashboardService.buildTraderSummary(eq("traderA"),
                                eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 12, 31))))
                                .thenReturn(summary);

                mockMvc.perform(get("/api/dashboard/summary")
                                .param("performedBy", "traderA")
                                .param("from", "2025-01-01")
                                .param("to", "2025-12-31")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.trader", is("traderA")))
                                .andExpect(jsonPath("$.totalTrades", is(17)))
                                .andExpect(jsonPath("$.tradesByStatus.NEW", is(10)))
                                .andExpect(jsonPath("$.notionalByCurrency.USD", is(1000000)));
        }

        @Test
        void dailySummary_returnsTodayVsPreviousStats() throws Exception {
                DailySummaryDTO daily = new DailySummaryDTO();
                daily.setTrader("traderA");
                daily.setAsOfDate(LocalDate.of(2025, 10, 1));
                daily.setTodayTradeCount(12);
                daily.setTodayTotalNotional(new BigDecimal("2500000"));
                daily.setTodayNotionalByCurrency(Map.of("USD", new BigDecimal("2000000")));
                daily.setTodayTradesByBook(Map.of("Rates", 7L, "Credit", 5L));
                daily.setPrevDayTradeCount(8);
                daily.setPrevDayTotalNotional(new BigDecimal("1500000"));
                daily.setTradeCountDelta(4L);
                daily.setTradeCountDeltaPercentage(50.0);
                daily.setNotionalDelta(new BigDecimal("1000000"));
                daily.setNotionalDeltaPercentage(66.67);

                when(dashboardService.buildDailySummary(eq("traderA"), eq(LocalDate.of(2025, 10, 1))))
                                .thenReturn(daily);

                mockMvc.perform(get("/api/dashboard/daily-summary")
                                .param("performedBy", "traderA")
                                .param("asOf", "2025-10-01")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.trader", is("traderA")))
                                .andExpect(jsonPath("$.todayTradeCount", is(12)))
                                .andExpect(jsonPath("$.prevDayTradeCount", is(8)))
                                .andExpect(jsonPath("$.tradeCountDelta", is(4)));
        }
}
