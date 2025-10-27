package com.technicalchallenge.repository;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Expression;

import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiFunction;

import com.technicalchallenge.model.Trade;

public final class RsqlBuilder {

    private RsqlBuilder() {
    }

    public static Specification<Trade> from(String rsql) {
        if (rsql == null || rsql.isBlank())
            return null;
        Node root = new RSQLParser().parse(rsql);
        return root.accept(new Visitor());
    }

    /** Tiny visitor with a whitelist of fields we support */
    static class Visitor implements RSQLVisitor<Specification<Trade>, Void> {

        // Map supported selector -> Path extractor
        private final Map<String, BiFunction<Root<Trade>, CriteriaBuilder, Path<?>>> fields = Map.of(
                "tradeDate", (root, cb) -> root.get("tradeDate"),
                "book.bookName", (root, cb) -> root.join("book", JoinType.LEFT).get("bookName"),
                "counterparty.name", (root, cb) -> root.join("counterparty", JoinType.LEFT).get("name"),
                "tradeStatus.tradeStatus", (root, cb) -> root.join("tradeStatus", JoinType.LEFT).get("tradeStatus"));

        @Override
        public Specification<Trade> visit(AndNode node, Void param) {
            return node.getChildren().stream()
                    .map(n -> n.accept(this))
                    .reduce(Specification.where(null), Specification::and);
        }

        @Override
        public Specification<Trade> visit(OrNode node, Void param) {
            return node.getChildren().stream()
                    .map(n -> n.accept(this))
                    .reduce(Specification.where(null), Specification::or);
        }

        @Override
        public Specification<Trade> visit(ComparisonNode node, Void param) {
            String selector = node.getSelector();
            if (!fields.containsKey(selector)) {
                throw new IllegalArgumentException("Unsupported field: " + selector);
            }
            ComparisonOperator op = node.getOperator();
            List<String> args = node.getArguments();

            return (root, query, cb) -> {
                Path<?> path = fields.get(selector).apply(root, cb);
                Class<?> jt = path.getJavaType();

                Object value = convert(args.get(0), jt);

                if (op.equals(RSQLOperators.EQUAL)) {
                    if (value instanceof String s && s.contains("*")) {
                        String like = s.replace('*', '%').toLowerCase();
                        return cb.like(cb.lower(path.as(String.class)), like);
                    }
                    return cb.equal(path, value);
                }
                if (op.equals(RSQLOperators.NOT_EQUAL)) {
                    if (value instanceof String s && s.contains("*")) {
                        String like = s.replace('*', '%').toLowerCase();
                        return cb.notLike(cb.lower(path.as(String.class)), like);
                    }
                    return cb.notEqual(path, value);
                }

                if (jt.equals(LocalDate.class)) {
                    LocalDate v = (LocalDate) value;
                    Expression<LocalDate> p = path.as(LocalDate.class);

                    if (op.equals(RSQLOperators.GREATER_THAN)) {
                        return cb.greaterThan(p, v);
                    }
                    if (op.equals(RSQLOperators.GREATER_THAN_OR_EQUAL)) {
                        return cb.greaterThanOrEqualTo(p, v);
                    }
                    if (op.equals(RSQLOperators.LESS_THAN)) {
                        return cb.lessThan(p, v);
                    }
                    if (op.equals(RSQLOperators.LESS_THAN_OR_EQUAL)) {
                        return cb.lessThanOrEqualTo(p, v);
                    }
                }

                if (op.equals(RSQLOperators.IN) || op.equals(RSQLOperators.NOT_IN)) {
                    CriteriaBuilder.In<Object> in = cb.in(path);
                    for (String a : args) {
                        in.value(convert(a, jt));
                    }
                    return op.equals(RSQLOperators.IN) ? in : cb.not(in);
                }

                throw new IllegalArgumentException("Unsupported operator for field type: " + op);
            };
        }

        private Object convert(String raw, Class<?> type) {
            if (raw == null)
                return null;
            if (type.equals(String.class))
                return raw;
            if (type.equals(LocalDate.class))
                return LocalDate.parse(raw);
            return raw;
        }
    }
}
