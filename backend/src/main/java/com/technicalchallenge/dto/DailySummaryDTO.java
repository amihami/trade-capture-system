package com.technicalchallenge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class DailySummaryDTO {

    private String trader;
    private LocalDate asOfDate;

    private long todayTradeCount;
    private BigDecimal todayTotalNotional;

    private Map<String, BigDecimal> todayNotionalByCurrency;
    private Map<String, Long> todayTradesByBook;

    private long prevDayTradeCount;
    private BigDecimal prevDayTotalNotional;

    private Long tradeCountDelta;
    private Double tradeCountDeltaPercentage;
    private BigDecimal notionalDelta;
    private Double notionalDeltaPercentage;

    public DailySummaryDTO() {
    }

    public String getTrader() {
        return trader;
    }

    public void setTrader(String trader) {
        this.trader = trader;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public long getTodayTradeCount() {
        return todayTradeCount;
    }

    public void setTodayTradeCount(long todayTradeCount) {
        this.todayTradeCount = todayTradeCount;
    }

    public BigDecimal getTodayTotalNotional() {
        return todayTotalNotional;
    }

    public void setTodayTotalNotional(BigDecimal todayTotalNotional) {
        this.todayTotalNotional = todayTotalNotional;
    }

    public Map<String, BigDecimal> getTodayNotionalByCurrency() {
        return todayNotionalByCurrency;
    }

    public void setTodayNotionalByCurrency(Map<String, BigDecimal> todayNotionalByCurrency) {
        this.todayNotionalByCurrency = todayNotionalByCurrency;
    }

    public Map<String, Long> getTodayTradesByBook() {
        return todayTradesByBook;
    }

    public void setTodayTradesByBook(Map<String, Long> todayTradesByBook) {
        this.todayTradesByBook = todayTradesByBook;
    }

    public long getPrevDayTradeCount() {
        return prevDayTradeCount;
    }

    public void setPrevDayTradeCount(long prevDayTradeCount) {
        this.prevDayTradeCount = prevDayTradeCount;
    }

    public BigDecimal getPrevDayTotalNotional() {
        return prevDayTotalNotional;
    }

    public void setPrevDayTotalNotional(BigDecimal prevDayTotalNotional) {
        this.prevDayTotalNotional = prevDayTotalNotional;
    }

    public Long getTradeCountDelta() {
        return tradeCountDelta;
    }

    public void setTradeCountDelta(Long tradeCountDelta) {
        this.tradeCountDelta = tradeCountDelta;
    }

    public Double getTradeCountDeltaPercentage() {
        return tradeCountDeltaPercentage;
    }

    public void setTradeCountDeltaPercentage(Double tradeCountDeltaPercentage) {
        this.tradeCountDeltaPercentage = tradeCountDeltaPercentage;
    }

    public BigDecimal getNotionalDelta() {
        return notionalDelta;
    }

    public void setNotionalDelta(BigDecimal notionalDelta) {
        this.notionalDelta = notionalDelta;
    }

    public Double getNotionalDeltaPercentage() {
        return notionalDeltaPercentage;
    }

    public void setNotionalDeltaPercentage(Double notionalDeltaPercentage) {
        this.notionalDeltaPercentage = notionalDeltaPercentage;
    }
}