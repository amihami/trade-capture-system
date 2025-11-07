package com.technicalchallenge.controller;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.RsqlBuilder;
import com.technicalchallenge.service.TradeService;
import com.technicalchallenge.service.TradeValidationService;

import cz.jirutka.rsql.parser.ParseException;
import cz.jirutka.rsql.parser.RSQLParserException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/trades")
@Validated
@Tag(name = "Trades", description = "Trade management operations including booking, searching, and lifecycle management")
public class TradeController {
    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);

    @Autowired
    private TradeService tradeService;
    @Autowired
    private TradeMapper tradeMapper;
    @Autowired
    private TradeValidationService tradeValidationService;

    @GetMapping
    @Operation(summary = "Get all trades", description = "Retrieves a list of all trades in the system. Returns comprehensive trade information including legs and cashflows.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all trades", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public List<TradeDTO> getAllTrades() {
        logger.info("Fetching all trades");
        return tradeService.getAllTrades().stream()
                .map(tradeMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trade by ID", description = "Retrieves a specific trade by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trade found and returned successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Trade not found"),
            @ApiResponse(responseCode = "400", description = "Invalid trade ID format")
    })
    public ResponseEntity<TradeDTO> getTradeById(
            @Parameter(description = "Unique identifier of the trade", required = true) @PathVariable(name = "id") Long id) {
        logger.debug("Fetching trade by id: {}", id);
        return tradeService.getTradeById(id)
                .map(tradeMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search trades multicriteria and paginated", description = "Search by counterparty, book, trader, status and date range. Supports large result sets with pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })

    public ResponseEntity<Page<TradeDTO>> searchTrades(
            @Parameter(description = "Counterparty name (contains, case-insensitive)") @RequestParam(required = false) String counterparty,
            @Parameter(description = "Book name (contains, case-insensitive)") @RequestParam(required = false) String book,
            @Parameter(description = "Trader (first name / last name / loginId contains, case-insensitive)") @RequestParam(required = false) String trader,
            @Parameter(description = "Trade status (exact match, e.g. NEW, AMENDED, CANCELLED)") @RequestParam(required = false) String status,
            @Parameter(description = "Start of trade date range (inclusive, ISO-8601: yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End of trade date range (inclusive, ISO-8601: yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable) {
        TradeDTO criteria = new TradeDTO();
        criteria.setCounterpartyName(counterparty);
        criteria.setBookName(book);
        criteria.setTradeStatus(status);
        criteria.setValidityStartDate(dateFrom);
        criteria.setValidityEndDate(dateTo);

        Page<Trade> page = tradeService.searchTrades(criteria, pageable);
        Page<TradeDTO> dtoPage = page.map(tradeMapper::toDto);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/search/settlement-instructions")
    @Operation(summary = "Search trades by settlement instructions (partial, case-insensitive)", description = "Returns trades whose settlement instructions contain the given text.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class)))
    })
    public ResponseEntity<List<TradeDTO>> searchBySettlementInstructions(
            @Parameter(description = "Partial text to search within settlement instructions (case-insensitive)", required = true) @RequestParam String instructions) {

        logger.info("Searching trades by settlement instructions containing: {}", instructions);
        List<TradeDTO> results = tradeService.searchBySettlementInstructions(instructions)
                .stream()
                .map(tradeMapper::toDto)
                .toList();

        return ResponseEntity.ok(results);
    }

    @GetMapping("/filter")
    @Operation(summary = "List trades, paginated blotter", description = "Returns paginated list of trades without filters. Combine with pageable query params (?page=&size=&sort).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })

    public ResponseEntity<Page<TradeDTO>> filterTrades(Pageable pageable) {
        Page<Trade> page = tradeService.filterTrades(pageable);
        Page<TradeDTO> dtoPage = page.map(tradeMapper::toDto);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/rsql")
    @Operation(summary = "Search trades with RSQL for power users", description = "Supports ==, !=, =in=, =out=, =ge=, =le=, =gt=, =lt= and nested fields. Returns paginated results.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid RSQL query"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to create trade"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })

    public ResponseEntity<?> rsqlSearch(
            @Parameter(description = "RSQL query string, e.g. counterparty.name==ABC;tradeDate=ge=2025-01-01") @RequestParam(name = "query") String rsql,
            Pageable pageable) {
        try {
            if (rsql == null || rsql.isBlank()) {
                return ResponseEntity.badRequest().body("Query must not be blank.");
            }

            Specification<Trade> spec = RsqlBuilder.from(rsql);
            Page<Trade> page = tradeService.searchBySpecification(spec, pageable);

            Page<TradeDTO> dtoPage = page.map(tradeMapper::toDto);

            return ResponseEntity.ok(dtoPage);

        } catch (RSQLParserException ex) {
            return ResponseEntity.badRequest().body("Invalid RSQL syntax: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid RSQL query: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("RSQL search failed: {}", ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body("Error executing RSQL search.");
        }
    }

    @PostMapping
    @Operation(summary = "Create new trade", description = "Creates a new trade with the provided details. Automatically generates cashflows and validates business rules.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Trade created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid trade data or business rule violation"),
            @ApiResponse(responseCode = "500", description = "Internal server error during trade creation")
    })
    public ResponseEntity<?> createTrade(
            @Parameter(description = "Trade details for creation", required = true) @Valid @RequestBody TradeDTO tradeDTO,
            @RequestParam(name = "performedBy", required = false) String performedBy) {

        if (performedBy == null || performedBy.isBlank()) {
            if (tradeDTO.getTradeInputterUserId() != null) {
                performedBy = String.valueOf(tradeDTO.getTradeInputterUserId());
            } else if (tradeDTO.getInputterUserName() != null && !tradeDTO.getInputterUserName().isBlank()) {
                performedBy = tradeDTO.getInputterUserName();
            } else if (tradeDTO.getTraderUserId() != null) {
                performedBy = String.valueOf(tradeDTO.getTraderUserId());
            } else if (tradeDTO.getTraderUserName() != null && !tradeDTO.getTraderUserName().isBlank()) {
                performedBy = tradeDTO.getTraderUserName();
            }
        }

        if (performedBy == null || !tradeValidationService.validateUserPrivileges(
                performedBy, TradeValidationService.OPERATION_CREATE, tradeDTO)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("User not authorised to CREATE trades.");
        }

        boolean bookMissing = tradeDTO.getBookId() == null
                && (tradeDTO.getBookName() == null || tradeDTO.getBookName().isBlank());
        boolean counterpartyMissing = tradeDTO.getCounterpartyId() == null
                && (tradeDTO.getCounterpartyName() == null || tradeDTO.getCounterpartyName().isBlank());
        if (bookMissing || counterpartyMissing) {
            return ResponseEntity.badRequest().body("Book and Counterparty are required");
        }

        logger.info("Creating new trade: {}", tradeDTO);
        try {
            Trade trade = tradeMapper.toEntity(tradeDTO);
            tradeService.populateReferenceDataByName(trade, tradeDTO);
            Trade savedTrade = tradeService.saveTrade(trade, tradeDTO);
            TradeDTO responseDTO = tradeMapper.toDto(savedTrade);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (Exception e) {
            logger.error("Error creating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error creating trade: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update existing trade", description = "Updates an existing trade with new information. Subject to business rule validation and user privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trade updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Trade not found"),
            @ApiResponse(responseCode = "400", description = "Invalid trade data or business rule violation"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to update trade")
    })
    public ResponseEntity<?> updateTrade(
            @Parameter(description = "Unique identifier of the trade to update", required = true) @PathVariable Long id,
            @Parameter(description = "Updated trade details", required = true) @Valid @RequestBody TradeDTO tradeDTO,
            @RequestParam(name = "performedBy", required = false) String performedBy) {
        logger.info("Updating trade with id: {}", id);

        if (tradeDTO.getTradeId() != null && !tradeDTO.getTradeId().equals(id)) {
            return ResponseEntity.badRequest().body("Trade ID in path must match Trade ID in request body");
        }

        try {
            if (tradeDTO.getTradeId() == null) {
                tradeDTO.setTradeId(id);
            } // Ensure the ID matches

            if (performedBy == null || performedBy.isBlank()) {
                if (tradeDTO.getTradeInputterUserId() != null) {
                    performedBy = String.valueOf(tradeDTO.getTradeInputterUserId());
                } else if (tradeDTO.getInputterUserName() != null && !tradeDTO.getInputterUserName().isBlank()) {
                    performedBy = tradeDTO.getInputterUserName();
                } else if (tradeDTO.getTraderUserId() != null) {
                    performedBy = String.valueOf(tradeDTO.getTraderUserId());
                } else if (tradeDTO.getTraderUserName() != null && !tradeDTO.getTraderUserName().isBlank()) {
                    performedBy = tradeDTO.getTraderUserName();
                }
            }

            if (performedBy == null || !tradeValidationService.validateUserPrivileges(
                    performedBy, TradeValidationService.OPERATION_AMEND, tradeDTO)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("User not authorised to AMEND trades.");
            }

            Trade trade = tradeMapper.toEntity(tradeDTO);
            tradeService.populateReferenceDataByName(trade, tradeDTO);
            Trade savedTrade = tradeService.saveTrade(trade, tradeDTO);

            TradeDTO responseDTO = tradeMapper.toDto(savedTrade);

            if (responseDTO.getTradeId() == null) {
                responseDTO.setTradeId(id);
            }

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error updating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error updating trade: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/settlement-instructions")
    @Operation(summary = "Update settlement instructions for an existing trade", description = "Edits only the settlement instructions field on the latest active version of the trade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settlement instructions updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to update settlement instructions"),
            @ApiResponse(responseCode = "404", description = "Trade not found")
    })
    public ResponseEntity<?> updateSettlementInstructions(
            @Parameter(description = "Trade ID", required = true) @PathVariable Long id,
            @Valid @RequestBody com.technicalchallenge.dto.SettlementInstructionsUpdateDTO request) {

        String performedBy = request.getPerformedBy();
        if (performedBy == null || performedBy.isBlank()
                || !tradeValidationService.validateUserPrivileges(
                        performedBy, TradeValidationService.OPERATION_AMEND, null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("User not authorised to AMEND settlement instructions.");
        }

        try {
            Trade updated = tradeService.updateSettlementInstructions(id, request.getSettlementInstructions());
            TradeDTO responseDTO = tradeMapper.toDto(updated);
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException ex) {
            logger.error("Error updating settlement instructions: {}", ex.getMessage(), ex);
            if (ex.getMessage() != null && ex.getMessage().startsWith("Trade not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
            }
            return ResponseEntity.badRequest().body("Error updating settlement instructions: " + ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete trade", description = "Deletes an existing trade. This is a soft delete that changes the trade status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Trade deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Trade not found"),
            @ApiResponse(responseCode = "400", description = "Trade cannot be deleted in current status"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to delete trade")
    })
    public ResponseEntity<?> deleteTrade(
            @Parameter(description = "Unique identifier of the trade to delete", required = true) @PathVariable Long id) {
        logger.info("Deleting trade with id: {}", id);
        try {
            tradeService.deleteTrade(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error deleting trade: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate trade", description = "Terminates an existing trade before its natural maturity date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trade terminated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Trade not found"),
            @ApiResponse(responseCode = "400", description = "Trade cannot be terminated in current status"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to terminate trade")
    })
    public ResponseEntity<?> terminateTrade(
            @Parameter(description = "Unique identifier of the trade to terminate", required = true) @PathVariable Long id,
            @RequestParam(name = "performedBy") String performedBy) {
        logger.info("Terminating trade with id: {} by [{}]", id, performedBy);

        if (performedBy == null || performedBy.isBlank()
                || !tradeValidationService.validateUserPrivileges(
                        performedBy, TradeValidationService.OPERATION_TERMINATE, null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("User not authorised to TERMINATE trades.");
        }

        try {
            Trade terminatedTrade = tradeService.terminateTrade(id);
            TradeDTO responseDTO = tradeMapper.toDto(terminatedTrade);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error terminating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error terminating trade: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel trade", description = "Cancels an existing trade by changing its status to cancelled")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trade cancelled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Trade not found"),
            @ApiResponse(responseCode = "400", description = "Trade cannot be cancelled in current status"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to cancel trade")
    })
    public ResponseEntity<?> cancelTrade(
            @Parameter(description = "Unique identifier of the trade to cancel", required = true) @PathVariable Long id,
            @RequestParam(name = "performedBy") String performedBy) {
        logger.info("Cancelling trade with id: {} by [{}]", id, performedBy);

        if (performedBy == null || performedBy.isBlank()
                || !tradeValidationService.validateUserPrivileges(
                        performedBy, TradeValidationService.OPERATION_CANCEL, null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("User not authorised to CANCEL trades.");
        }
        try {
            Trade cancelledTrade = tradeService.cancelTrade(id);
            TradeDTO responseDTO = tradeMapper.toDto(cancelledTrade);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error cancelling trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error cancelling trade: " + e.getMessage());
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(msg);
    }
}
