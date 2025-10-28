package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.ValidationResult;
import com.technicalchallenge.model.*;
import com.technicalchallenge.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradeValidationServiceTest {
    @Mock
    private ApplicationUserRepository applicationUserRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private CounterpartyRepository counterpartyRepository;

    @InjectMocks
    private TradeValidationService validationService;

    @Test
    void validateTradeBusinessRules_dateOrderingErrors() {
        TradeDTO dto = new TradeDTO();
        dto.setTradeDate(LocalDate.now());
        dto.setTradeStartDate(LocalDate.now().minusDays(1)); 
        dto.setTradeMaturityDate(LocalDate.now().minusDays(2)); 

        ValidationResult result = validationService.validateTradeBusinessRules(dto);
        assertTrue(result.failed());
        assertTrue(result.getErrors().stream().anyMatch(m -> m.toLowerCase().contains("start date")));
        assertTrue(result.getErrors().stream().anyMatch(m -> m.toLowerCase().contains("maturity date")));
    }

    @Test
    void validateTradeBusinessRules_legConsistencyErrors() {
        TradeDTO dto = new TradeDTO();
        dto.setTradeDate(LocalDate.now());
        dto.setTradeStartDate(LocalDate.now().plusDays(1));
        dto.setTradeMaturityDate(LocalDate.now().plusDays(10));

        TradeLegDTO l1 = new TradeLegDTO();
        l1.setNotional(BigDecimal.valueOf(1_000_000));
        l1.setLegType("Fixed");
        l1.setPayReceiveFlag("Pay");

        TradeLegDTO l2 = new TradeLegDTO();
        l2.setNotional(BigDecimal.valueOf(1_000_000));
        l2.setLegType("Fixed");
        l2.setPayReceiveFlag("Pay"); 
        l2.setRate(0.01);

        dto.setTradeLegs(java.util.List.of(l1, l2));

        ValidationResult result = validationService.validateTradeBusinessRules(dto);
        assertTrue(result.failed());
        assertTrue(result.getErrors().stream().anyMatch(m -> m.toLowerCase().contains("opposite")));
        assertTrue(result.getErrors().stream().anyMatch(m -> m.toLowerCase().contains("fixed leg")));
    }

    @Test
    void validateTradeBusinessRules_entityMustBeActive() {
        TradeDTO dto = new TradeDTO();
        dto.setTradeDate(LocalDate.now());
        dto.setTradeStartDate(LocalDate.now().plusDays(1));
        dto.setTradeMaturityDate(LocalDate.now().plusDays(10));
        dto.setBookName("Rates");
        dto.setCounterpartyName("XYZ");

        Book inactiveBook = new Book();
        inactiveBook.setActive(false);
        when(bookRepository.findByBookName("Rates")).thenReturn(Optional.of(inactiveBook));

        Counterparty inactiveCp = new Counterparty();
        inactiveCp.setActive(false);
        when(counterpartyRepository.findByName("XYZ")).thenReturn(Optional.of(inactiveCp));

        ValidationResult result = validationService.validateTradeBusinessRules(dto);
        assertTrue(result.failed());
        assertTrue(result.getErrors().stream().anyMatch(m -> m.toLowerCase().contains("book must")));
        assertTrue(result.getErrors().stream().anyMatch(m -> m.toLowerCase().contains("counterparty must")));
    }

}
