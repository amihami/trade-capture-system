package com.technicalchallenge.repository;

import com.technicalchallenge.model.Trade;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class RsqlSpecificationBuilder {

    private RsqlSpecificationBuilder() {
    }

    public static Specification<Trade> fromRsql(String query){
        return from(query);
    }

    public static Specification<Trade> from(String query) {
        if (query == null || query.isBlank())
            return null;

        Specification<Trade> spec = Specification.where(null);

        String[] clauses = query.split(";");
        for (String raw : clauses) {
            String c = raw == null ? "" : raw.trim();
            if (c.isEmpty())
                continue;

            c = c.replaceAll("\\s+", "");

            if (c.startsWith("counterparty.name==")) {
                String value = c.substring("counterparty.name==".length());
                spec = spec.and(TradeSpecifications.counterpartyNameContains(value));
                continue;
            }

            if (c.startsWith("book.bookName==")) {
                String value = c.substring("book.bookName==".length());
                spec = spec.and(TradeSpecifications.bookNameContains(value));
                continue;
            }

            if (c.startsWith("tradeStatus.tradeStatus==")) {
                String value = c.substring("tradeStatus.tradeStatus==".length());
                spec = spec.and(TradeSpecifications.statusEquals(value));
                continue;
            }

            if (c.startsWith("tradeDate=ge=")) {
                String value = c.substring("tradeDate=ge=".length());
                LocalDate date = LocalDate.parse(value);
                spec = spec.and(TradeSpecifications.tradeDateGte(date));
                continue;
            }

            if (c.startsWith("tradeDate=le=")) {
                String value = c.substring("tradeDate=le=".length());
                LocalDate date = LocalDate.parse(value);
                spec = spec.and(TradeSpecifications.tradeDateLte(date));
                continue;
            }

            if (c.startsWith("tradeDate==")) {
                String value = c.substring("tradeDate==".length());
                LocalDate date = LocalDate.parse(value);
                spec = spec.and(TradeSpecifications.tradeDateGte(date))
                        .and(TradeSpecifications.tradeDateLte(date));
                continue;
            }

            throw new IllegalArgumentException("Unsupported RSQL clause: " + c);
        }

        return spec;
    }
}