package com.technicalchallenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.service.TradeService;
import com.technicalchallenge.service.TradeValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TradeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdvancedSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeService tradeService;

    @MockBean
    private TradeMapper tradeMapper;

    @MockBean
    private TradeValidationService tradeValidationService;

    private ObjectMapper objectMapper;
    private Trade trade;
    private TradeDTO tradeDTO;
    private Page<Trade> tradePage;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(1001L);
        trade.setVersion(1);
        trade.setTradeDate(LocalDate.of(2025, 1, 15));

        tradeDTO = new TradeDTO();
        tradeDTO.setId(1L);
        tradeDTO.setTradeId(1001L);
        tradeDTO.setVersion(1);
        tradeDTO.setTradeDate(LocalDate.of(2025, 1, 15));
        tradeDTO.setBookName("TestBook");
        tradeDTO.setCounterpartyName("TestCounterparty");
        tradeDTO.setTradeStatus("NEW");

        Pageable pageable = PageRequest.of(0, 20, Sort.by("tradeId").descending());
        tradePage = new PageImpl<>(List.of(trade), pageable, 1);

        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);
    }

    @Test
    void testSearchTrades_withAllParams_ok() throws Exception {
        when(tradeService.searchTrades(any(TradeDTO.class), any(Pageable.class))).thenReturn(tradePage);

        mockMvc.perform(get("/api/trades/search")
                .param("counterparty", "TestCounterparty")
                .param("book", "TestBook")
                .param("trader", "alice")
                .param("status", "NEW")
                .param("dateFrom", "2025-01-01")
                .param("dateTo", "2025-12-31")
                .param("page", "0")
                .param("size", "20")
                .param("sort", "tradeId,desc")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tradeId", is(1001)))
                .andExpect(jsonPath("$.content[0].bookName", is("TestBook")))
                .andExpect(jsonPath("$.content[0].counterpartyName", is("TestCounterparty")));

        verify(tradeService).searchTrades(any(TradeDTO.class), any(Pageable.class));
    }

    @Test
    void testSearchTrades_emptyParams_ok() throws Exception {
        when(tradeService.searchTrades(any(TradeDTO.class), any(Pageable.class))).thenReturn(tradePage);

        mockMvc.perform(get("/api/trades/search")
                .param("page", "0")
                .param("size", "20")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(tradeService).searchTrades(any(TradeDTO.class), any(Pageable.class));
    }

    @Test
    void testFilterTrades_ok() throws Exception {
        when(tradeService.filterTrades(any(Pageable.class))).thenReturn(tradePage);

        mockMvc.perform(get("/api/trades/filter")
                .param("page", "0")
                .param("size", "20")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tradeId", is(1001)));

        verify(tradeService).filterTrades(any(Pageable.class));
    }

    @Test
    void testRsql_simpleCounterparty_ok() throws Exception {
        when(tradeService.searchBySpecification(any(), any(Pageable.class))).thenReturn(tradePage);

        mockMvc.perform(get("/api/trades/rsql")
                .param("query", "counterparty.name==TestCounterparty")
                .param("page", "0")
                .param("size", "20")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tradeId", is(1001)));

        verify(tradeService).searchBySpecification(any(), any(Pageable.class));
    }

    @Test
    void testRsql_complexAndOr_ok() throws Exception {
        when(tradeService.searchBySpecification(any(), any(Pageable.class))).thenReturn(tradePage);

        mockMvc.perform(get("/api/trades/rsql")
                .param("query", "(counterparty.name==ABC,counterparty.name==XYZ);tradeStatus.tradeStatus==NEW")
                .param("page", "0")
                .param("size", "20")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(tradeService).searchBySpecification(any(), any(Pageable.class));
    }

    @Test
    void testRsql_dateRange_ok() throws Exception {
        when(tradeService.searchBySpecification(any(), any(Pageable.class))).thenReturn(tradePage);

        mockMvc.perform(get("/api/trades/rsql")
                .param("query", "tradeDate=ge=2025-01-01;tradeDate=le=2025-12-31")
                .param("page", "0")
                .param("size", "20")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(tradeService).searchBySpecification(any(), any(Pageable.class));
    }

    @Test
    void testRsql_badSyntax_400() throws Exception {
        mockMvc.perform(get("/api/trades/rsql")
                .param("query", "counterparty.name===ABC") 
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid RSQL syntax")));

        verify(tradeService, never()).searchBySpecification(any(), any(Pageable.class));
    }

    @Test
    void testRsql_badDate_returns500_genericError() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);

        when(tradeService.searchBySpecification(
                org.mockito.ArgumentMatchers.<Specification<Trade>>any(),
                eq(pageable)))
                .thenThrow(new java.time.format.DateTimeParseException(
                        "Invalid value for MonthOfYear (valid values 1 - 12): 13",
                        "2025-13-01",
                        5));

        mockMvc.perform(get("/api/trades/rsql")
                .param("query", "tradeDate=ge=2025-13-01")
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Error executing RSQL search.")));

        verify(tradeService, times(1))
                .searchBySpecification(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), eq(pageable));
    }

    @Test
    void testRsql_unsupportedField_400() throws Exception {
        mockMvc.perform(get("/api/trades/rsql")
                .param("query", "foo==bar")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid RSQL query:")));

        verify(tradeService, never()).searchBySpecification(any(), any(Pageable.class));
    }
}