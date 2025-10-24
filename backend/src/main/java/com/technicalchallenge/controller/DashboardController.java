package com.technicalchallenge.controller;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Trader dashboard and blotter views")
public class DashboardController {

    private final DashboardService dashboardService;
    private final TradeMapper tradeMapper;

    public DashboardController(DashboardService dashboardService, TradeMapper tradeMapper) {
        this.dashboardService = dashboardService;
        this.tradeMapper = tradeMapper;
    }
    
    @GetMapping("/my-trades")
    @Operation(
        summary = "My trades (paginated blotter)",
        description = "Returns a paginated blotter for the current trader. "
                    + "Provide performedBy as loginId or numeric user id."
    )
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
        schema = @Schema(implementation = com.technicalchallenge.dto.TradeDTO.class)))
    public ResponseEntity<?> myTrades(
            @Parameter(description = "Trader identifier (loginId or numeric id)")
            @RequestParam(name = "performedBy") String performedBy,
            Pageable pageable
    ) {
        Page<Trade> page = dashboardService.getMyTrades(performedBy, pageable);
        return ResponseEntity.ok(page.map(tradeMapper::toDto));
    }

    @GetMapping("/book/{id}/trades")
    @Operation(
        summary = "Book-level trades (paginated blotter)",
        description = "Returns a paginated blotter for a given book id."
    )
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
        schema = @Schema(implementation = com.technicalchallenge.dto.TradeDTO.class)))
    public ResponseEntity<?> bookTrades(
            @Parameter(description = "Book id") @PathVariable("id") Long bookId,
            Pageable pageable
    ) {
        Page<Trade> page = dashboardService.getTradesByBook(bookId, pageable);
        return ResponseEntity.ok(page.map(tradeMapper::toDto));
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Trader portfolio summary",
        description = "Aggregated metrics for a trader over an optional date range. "
                    + "Provide performedBy as loginId or numeric id."
    )
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
        schema = @Schema(implementation = TradeSummaryDTO.class)))
    public ResponseEntity<TradeSummaryDTO> summary(
            @Parameter(description = "Trader identifier (loginId or numeric id)")
            @RequestParam(name = "performedBy") String performedBy,
            @Parameter(description = "From date (inclusive, yyyy-MM-dd)")
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "To date (inclusive, yyyy-MM-dd)")
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        TradeSummaryDTO dto = dashboardService.buildTraderSummary(performedBy, from, to);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/daily-summary")
    @Operation(
        summary = "Daily trading summary",
        description = "Today vs previous day metrics for a trader. "
                    + "Provide performedBy as loginId or numeric id."
    )
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
        schema = @Schema(implementation = DailySummaryDTO.class)))
    public ResponseEntity<DailySummaryDTO> dailySummary(
            @Parameter(description = "Trader identifier (loginId or numeric id)")
            @RequestParam(name = "performedBy") String performedBy,
            @Parameter(description = "As-of date (yyyy-MM-dd). Defaults to today if omitted.")
            @RequestParam(name = "asOf", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        DailySummaryDTO dto = dashboardService.buildDailySummary(performedBy, asOf);
        return ResponseEntity.ok(dto);
    }
}