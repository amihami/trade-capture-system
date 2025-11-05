package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.TradeLeg;
import com.technicalchallenge.model.TradeStatus;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Cashflow;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.LegType;
import com.technicalchallenge.model.Schedule;
import com.technicalchallenge.repository.CashflowRepository;
import com.technicalchallenge.repository.TradeLegRepository;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.repository.TradeStatusRepository;
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.CounterpartyRepository;
import com.technicalchallenge.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.technicalchallenge.dto.ValidationResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
public class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeLegRepository tradeLegRepository;

    @Mock
    private CashflowRepository cashflowRepository;

    @Mock
    private TradeStatusRepository tradeStatusRepository;

    @Mock
    private AdditionalInfoService additionalInfoService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CounterpartyRepository counterpartyRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private TradeValidationService tradeValidationService;

    @InjectMocks
    private TradeService tradeService;

    private TradeDTO tradeDTO;
    private Trade trade;

    @BeforeEach
    void setUp() {
        // Set up test data
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.of(2025, 1, 15));
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 17));
        tradeDTO.setTradeMaturityDate(LocalDate.of(2026, 1, 17));
        tradeDTO.setBookName("TestBook");
        tradeDTO.setCounterpartyName("TestCounterparty");

        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);

        tradeDTO.setTradeLegs(Arrays.asList(leg1, leg2));

        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(100001L);
    }

    @Test
    void testCreateTrade_Success() {
        // Given
        when(bookRepository.findByBookName("TestBook")).thenReturn(Optional.of(new Book()));
        when(counterpartyRepository.findByName("TestCounterparty")).thenReturn(Optional.of(new Counterparty()));
        when(tradeStatusRepository.findByTradeStatus("NEW")).thenReturn(Optional.of(new TradeStatus()));

        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(inv -> inv.getArgument(0));

        when(tradeValidationService.validateTradeBusinessRules(any()))
                .thenReturn(ValidationResult.ok());

        // When
        Trade result = tradeService.createTrade(tradeDTO);

        // Then
        assertNotNull(result);
        assertEquals(100001L, result.getTradeId());
        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testCreateTrade_InvalidDates_ShouldFail() {
        // Given - This test is intentionally failing for candidates to fix
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 10)); // Before trade date

        when(tradeValidationService.validateTradeBusinessRules(any()))
                .thenAnswer(inv -> {
                    ValidationResult vr = ValidationResult.ok();
                    vr.addError("Start date cannot be before trade date"); // match assertion text
                    return vr;
                });
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        // This assertion is intentionally wrong - candidates need to fix it
        assertTrue(exception.getMessage().contains("Start date cannot be before trade date"));
    }

    @Test
    void testCreateTrade_InvalidLegCount_ShouldFail() {
        // Given
        tradeDTO.setTradeLegs(Arrays.asList(new TradeLegDTO())); // Only 1 leg

        when(tradeValidationService.validateTradeBusinessRules(any()))
                .thenReturn(ValidationResult.ok());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        String msg = exception.getMessage().toLowerCase();
        assertTrue(msg.contains("2") && msg.contains("leg"));
    }

    @Test
    void testGetTradeById_Found() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));

        // When
        Optional<Trade> result = tradeService.getTradeById(100001L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(100001L, result.get().getTradeId());
    }

    @Test
    void testGetTradeById_NotFound() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        // When
        Optional<Trade> result = tradeService.getTradeById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testAmendTrade_Success() {

        trade.setVersion(1);

        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));
        when(tradeStatusRepository.findByTradeStatus("AMENDED"))
                .thenReturn(Optional.of(new com.technicalchallenge.model.TradeStatus()));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(inv -> inv.getArgument(0));

        when(tradeValidationService.validateTradeBusinessRules(any()))
                .thenReturn(ValidationResult.ok());
        // When
        Trade result = tradeService.amendTrade(100001L, tradeDTO);

        // Then
        assertNotNull(result);
        verify(tradeRepository, times(2)).save(any(Trade.class)); // Save old and new
    }

    @Test
    void testAmendTrade_TradeNotFound() {
        lenient().when(tradeValidationService.validateTradeBusinessRules(any()))
                .thenReturn(ValidationResult.ok());
        lenient().when(tradeValidationService.validateUserPrivileges(any(), any(), any()))
                .thenReturn(true);

        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.amendTrade(999L, tradeDTO);
        });

        String msg = String.valueOf(exception.getMessage()).toLowerCase();
        assertTrue(msg.contains("not found"), "Expected error message to indicate 'not found' but was: " + msg);
    }

    // This test has a deliberate bug for candidates to find and fix
    @Test
    void testCashflowGeneration_MonthlySchedule() {
        LocalDate tradeDate = LocalDate.of(2025, 1, 1);
        LocalDate startDate = tradeDate;
        LocalDate maturityDate = startDate.plusMonths(1);

        TradeDTO dto = new TradeDTO();
        dto.setTradeDate(tradeDate);
        dto.setTradeStartDate(startDate);
        dto.setTradeMaturityDate(maturityDate);
        dto.setBookName("TestBook");
        dto.setCounterpartyName("TestCounterparty");

        TradeLegDTO legMonthly = new TradeLegDTO();
        legMonthly.setNotional(BigDecimal.valueOf(1_000_000));
        legMonthly.setRate(5.0); // percent input
        legMonthly.setCalculationPeriodSchedule("1M");

        TradeLegDTO legDefaultQuarterly = new TradeLegDTO();
        legDefaultQuarterly.setNotional(BigDecimal.valueOf(1_000_000));
        legDefaultQuarterly.setRate(5.0); // percent input
        legDefaultQuarterly.setCalculationPeriodSchedule(null);

        dto.setTradeLegs(java.util.List.of(legMonthly, legDefaultQuarterly));

        when(bookRepository.findByBookName("TestBook")).thenReturn(Optional.of(new Book()));
        when(counterpartyRepository.findByName("TestCounterparty")).thenReturn(Optional.of(new Counterparty()));
        when(tradeStatusRepository.findByTradeStatus("NEW")).thenReturn(Optional.of(new TradeStatus()));

        Schedule schedule = new Schedule();
        schedule.setSchedule("1M");
        when(scheduleRepository.findBySchedule("1M")).thenReturn(Optional.of(schedule));

        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));

        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(inv -> {
            TradeLeg leg = inv.getArgument(0);
            LegType fixed = new LegType();
            fixed.setType("Fixed");
            leg.setLegRateType(fixed);
            return leg;
        });

        when(cashflowRepository.save(any(Cashflow.class))).thenAnswer(inv -> inv.getArgument(0));

        when(tradeValidationService.validateTradeBusinessRules(any()))
                .thenReturn(ValidationResult.ok());

        tradeService.createTrade(dto);

        verify(cashflowRepository, times(1)).save(any(Cashflow.class));
    }

    @Test
    void testCashflowGeneration_QuarterlySchedule() {
        LocalDate tradeDate = LocalDate.of(2025, 1, 1);
        LocalDate startDate = tradeDate;
        LocalDate maturityDate = startDate.plusMonths(3);

        TradeDTO dto = new TradeDTO();
        dto.setTradeDate(tradeDate);
        dto.setTradeStartDate(startDate);
        dto.setTradeMaturityDate(maturityDate);
        dto.setBookName("TestBook");
        dto.setCounterpartyName("TestCounterparty");

        TradeLegDTO fixedQuarterly = new TradeLegDTO();
        fixedQuarterly.setNotional(BigDecimal.valueOf(10_000_000));
        fixedQuarterly.setRate(3.5); // percent input
        fixedQuarterly.setCalculationPeriodSchedule(null); // default quarterly

        TradeLegDTO dummy = new TradeLegDTO();
        dummy.setNotional(BigDecimal.ZERO);
        dummy.setRate(0.00);
        dummy.setCalculationPeriodSchedule(null);

        dto.setTradeLegs(java.util.List.of(fixedQuarterly, dummy));

        when(bookRepository.findByBookName("TestBook")).thenReturn(Optional.of(new Book()));
        when(counterpartyRepository.findByName("TestCounterparty")).thenReturn(Optional.of(new Counterparty()));
        when(tradeStatusRepository.findByTradeStatus("NEW")).thenReturn(Optional.of(new TradeStatus()));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));

        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(inv -> {
            TradeLeg leg = inv.getArgument(0);
            com.technicalchallenge.model.LegType lt = new com.technicalchallenge.model.LegType();
            if (leg.getNotional() != null && leg.getNotional().compareTo(BigDecimal.ZERO) > 0) {
                lt.setType("Fixed");
            } else {
                lt.setType("Floating");
            }
            leg.setLegRateType(lt);
            if (leg.getLegId() == null) {
                leg.setLegId(System.nanoTime());
            }
            return leg;
        });

        when(cashflowRepository.save(any(Cashflow.class))).thenAnswer(inv -> inv.getArgument(0));

        when(tradeValidationService.validateTradeBusinessRules(any()))
                .thenReturn(ValidationResult.ok());

        tradeService.createTrade(dto);

        verify(cashflowRepository, times(2)).save(any(Cashflow.class));
    }
}