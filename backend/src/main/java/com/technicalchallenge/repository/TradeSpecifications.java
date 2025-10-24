package com.technicalchallenge.repository;

import com.technicalchallenge.model.Trade;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Expression;

public final class TradeSpecifications {

    private TradeSpecifications() {
    }

    public static Specification<Trade> counterpartyNameContains(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank())
                return null;

            Join<Trade, ?> counterpartyJoin = root.join("counterparty");

            Expression<String> nameExpr = counterpartyJoin.get("name").as(String.class);
            String pattern = "%" + value.trim().toLowerCase() + "%";
            return cb.like(cb.lower(nameExpr), pattern);
        };
    }

    public static Specification<Trade> bookNameContains(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank())
                return null;

            Join<Trade, ?> bookJoin = root.join("book");
            Expression<String> bookNameExpr = bookJoin.get("bookName").as(String.class);
            String pattern = "%" + value.trim().toLowerCase() + "%";
            return cb.like(cb.lower(bookNameExpr), pattern);
        };
    }

    public static Specification<Trade> traderMatches(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank())
                return null;

            Join<Trade, ?> traderUserJoin = root.join("traderUser");
            String pattern = "%" + value.trim().toLowerCase() + "%";

            Expression<String> firstExpr = traderUserJoin.get("firstName").as(String.class);
            Expression<String> lastExpr = traderUserJoin.get("lastName").as(String.class);
            Expression<String> loginExpr = traderUserJoin.get("loginId").as(String.class);

            return cb.or(
                    cb.like(cb.lower(firstExpr), pattern),
                    cb.like(cb.lower(lastExpr), pattern),
                    cb.like(cb.lower(loginExpr), pattern));
        };
    }

    public static Specification<Trade> statusEquals(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank())
                return null;

            Join<Trade, ?> tradeStatusJoin = root.join("tradeStatus");
            Expression<String> statusExpr = tradeStatusJoin.get("tradeStatus").as(String.class);
            return cb.equal(cb.lower(statusExpr), status.toLowerCase());
        };
    }

    public static Specification<Trade> tradeDateGte(LocalDate dateFrom) {
        return (root, query, cb) -> {
            if (dateFrom == null)
                return null;
            return cb.greaterThanOrEqualTo(root.get("tradeDate"), dateFrom);
        };
    }

    public static Specification<Trade> tradeDateLte(LocalDate dateTo) {
        return (root, query, cb) -> {
            if (dateTo == null)
                return null;
            return cb.lessThanOrEqualTo(root.get("tradeDate"), dateTo);
        };
    }

    public static Specification<Trade> build(String counterpartyName,
            String bookName,
            String traderValue,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo) {
        return Specification
                .where(counterpartyNameContains(counterpartyName))
                .and(bookNameContains(bookName))
                .and(traderMatches(traderValue))
                .and(statusEquals(status))
                .and(tradeDateGte(dateFrom))
                .and(tradeDateLte(dateTo));
    }

    public static Specification<Trade> activeTrue() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Trade> traderIdEquals(Long traderId) {
        return (root, query, cb) -> {
            if (traderId == null)
                return null;
            return cb.equal(root.join("traderUser", JoinType.LEFT).get("id"), traderId);
        };
    }

    public static Specification<Trade> bookIdEquals(Long bookId) {
        return (root, query, cb) -> {
            if (bookId == null)
                return null;
            return cb.equal(root.join("book", JoinType.LEFT).get("id"), bookId);
        };
    }
}