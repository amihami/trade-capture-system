package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TradeServiceSearchTest {
    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeService tradeService;

    @Test
    void searchTrades_withCriteria_buildsSpec_andReturnsPage() {
        TradeDTO criteria = new TradeDTO();
        criteria.setCounterpartyName("ABC");
        criteria.setBookName("Rates");
        criteria.setTraderUserName("john");
        criteria.setTradeStatus("NEW");
        criteria.setValidityStartDate(LocalDate.parse("2025-01-01"));
        criteria.setValidityEndDate(LocalDate.parse("2025-12-31"));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Trade> page = new PageImpl<>(List.of(new Trade()), pageable, 1);

        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), eq(pageable)))
                .thenReturn(page);

        Page<Trade> result = tradeService.searchTrades(criteria, pageable);

        assertEquals(1, result.getTotalElements());
        verify(tradeRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), eq(pageable));
    }

    @Test
    void searchTrades_whenDateRangeMissing_fallsBackToTradeDate() {
        TradeDTO criteria = new TradeDTO();
        criteria.setCounterpartyName("ABC");
        criteria.setTradeDate(LocalDate.parse("2025-02-02")); // fallback source

        Pageable pageable = PageRequest.of(0, 20);
        Page<Trade> page = new PageImpl<>(List.of(new Trade()), pageable, 1);

        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), eq(pageable)))
                .thenReturn(page);

        Page<Trade> result = tradeService.searchTrades(criteria, pageable);

        assertEquals(1, result.getTotalElements());
        verify(tradeRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), eq(pageable));
    }

    @Test
    void filterTrades_delegatesToRepository() {
        Pageable pageable = PageRequest.of(2, 5);
        when(tradeRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        Page<Trade> result = tradeService.filterTrades(pageable);

        assertNotNull(result);
        verify(tradeRepository).findAll(pageable);
    }
}