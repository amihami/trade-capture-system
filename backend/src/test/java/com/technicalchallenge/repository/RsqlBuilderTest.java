package com.technicalchallenge.repository;

import cz.jirutka.rsql.parser.RSQLParserException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RsqlBuilderTest {

    @Test
    void from_unsupportedField_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () -> RsqlBuilder.from("foo==bar"));
    }

    @Test
    void from_badSyntax_throwsRsqlParserException() {
        assertThrows(RSQLParserException.class, () -> RsqlBuilder.from("counterparty.name==ABC,"));
    }

    @Test
    void from_tradeDate_ge_validDate_toPredicateDoesNotThrow() {
        Specification<?> spec = RsqlBuilder.from("tradeDate=ge=2025-01-01");
        assertNotNull(spec);

        Root<?> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        @SuppressWarnings("unchecked")
        Path<LocalDate> datePath = (Path<LocalDate>) mock(Path.class);
        @SuppressWarnings("unchecked")
        Expression<LocalDate> dateExpr = (Expression<LocalDate>) mock(Expression.class);

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);

        @SuppressWarnings("unchecked")
        Path<Object> pathForRoot = (Path<Object>) (Path<?>) datePath;
        when(root.get("tradeDate")).thenReturn(pathForRoot);

        doReturn(LocalDate.class).when(datePath).getJavaType();

        when(datePath.as(LocalDate.class)).thenReturn(dateExpr);
        when(cb.greaterThanOrEqualTo(eq(dateExpr), any(LocalDate.class))).thenReturn(predicate);

        @SuppressWarnings("unchecked")
        Specification<Object> typedSpec = (Specification<Object>) (Specification<?>) spec;
        @SuppressWarnings("unchecked")
        Root<Object> typedRoot = (Root<Object>) root;
        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> typedQuery = (CriteriaQuery<Object>) query;

        assertDoesNotThrow(() -> typedSpec.toPredicate(typedRoot, typedQuery, cb));
        verify(cb).greaterThanOrEqualTo(eq(dateExpr), eq(LocalDate.parse("2025-01-01")));
    }

    @Test
    void from_tradeDate_ge_invalidDate_failsOnToPredicate() {
        Specification<?> spec = RsqlBuilder.from("tradeDate=ge=2025-13-01"); 

        Root<?> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        @SuppressWarnings("unchecked")
        Path<LocalDate> datePath = (Path<LocalDate>) mock(Path.class);
        @SuppressWarnings("unchecked")
        Expression<LocalDate> dateExpr = (Expression<LocalDate>) mock(Expression.class);

        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("unchecked")
        Path<Object> pathForRoot = (Path<Object>) (Path<?>) datePath;
        when(root.get("tradeDate")).thenReturn(pathForRoot);

        doReturn(LocalDate.class).when(datePath).getJavaType();

        when(datePath.as(LocalDate.class)).thenReturn(dateExpr);

        @SuppressWarnings("unchecked")
        Specification<Object> typedSpec = (Specification<Object>) (Specification<?>) spec;
        @SuppressWarnings("unchecked")
        Root<Object> typedRoot = (Root<Object>) root;
        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> typedQuery = (CriteriaQuery<Object>) query;

        assertThrows(Exception.class, () -> typedSpec.toPredicate(typedRoot, typedQuery, cb));
    }
}