package com.technicalchallenge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class TradeSummaryDTO {

    private String trader;
    private LocalDate fromDate;
    private LocalDate toDate;

    private Map<String, Long> tradesByStatus;

    private Map<String, BigDecimal> notionalByCurrency;

    private Map<String, Long> tradesByType;

    private Map<String, Long> tradesByCounterparty;

    private Map<String, BigDecimal> notionalByCounterparty;

    private Map<String, BigDecimal> riskExposures;

    private long totalTrades;
    private LocalDate mostRecentTradeDate;

    private Map<String, BigDecimal> notionalByBookTop;

    private List<String> warnings;

    public TradeSummaryDTO() {
    }

    public String getTrader() {
        return trader;
    }

    public void setTrader(String trader) {
        this.trader = trader;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public Map<String, Long> getTradesByStatus() {
        return tradesByStatus;
    }

    public void setTradesByStatus(Map<String, Long> tradesByStatus) {
        this.tradesByStatus = tradesByStatus;
    }

    public Map<String, BigDecimal> getNotionalByCurrency() {
        return notionalByCurrency;
    }

    public void setNotionalByCurrency(Map<String, BigDecimal> notionalByCurrency) {
        this.notionalByCurrency = notionalByCurrency;
    }

    public Map<String, Long> getTradesByType() {
        return tradesByType;
    }

    public void setTradesByType(Map<String, Long> tradesByType) {
        this.tradesByType = tradesByType;
    }

    public Map<String, Long> getTradesByCounterparty() {
        return tradesByCounterparty;
    }

    public void setTradesByCounterparty(Map<String, Long> tradesByCounterparty) {
        this.tradesByCounterparty = tradesByCounterparty;
    }

    public Map<String, BigDecimal> getNotionalByCounterparty() {
        return notionalByCounterparty;
    }

    public void setNotionalByCounterparty(Map<String, BigDecimal> notionalByCounterparty) {
        this.notionalByCounterparty = notionalByCounterparty;
    }

    public Map<String, BigDecimal> getRiskExposures() {
        return riskExposures;
    }

    public void setRiskExposures(Map<String, BigDecimal> riskExposures) {
        this.riskExposures = riskExposures;
    }

    public long getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(long totalTrades) {
        this.totalTrades = totalTrades;
    }

    public LocalDate getMostRecentTradeDate() {
        return mostRecentTradeDate;
    }

    public void setMostRecentTradeDate(LocalDate mostRecentTradeDate) {
        this.mostRecentTradeDate = mostRecentTradeDate;
    }

    public Map<String, BigDecimal> getNotionalByBookTop() {
        return notionalByBookTop;
    }

    public void setNotionalByBookTop(Map<String, BigDecimal> notionalByBookTop) {
        this.notionalByBookTop = notionalByBookTop;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}