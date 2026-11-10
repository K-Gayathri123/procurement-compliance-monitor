package com.procurement.monitor.validation;

import com.procurement.monitor.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseRulesTest {

    @Test
    void convertsMoneyExactly() {
        assertEquals(102000L, PurchaseRules.money("10.20"));
        assertEquals(1L, PurchaseRules.money("0.0001"));
        assertEquals(0L, PurchaseRules.money("0"));
    }

    @Test
    void rejectsInvalidMoney() {
        assertThrows(
            ApiException.class,
            () -> PurchaseRules.money("-1")
        );
        assertThrows(
            ApiException.class,
            () -> PurchaseRules.money("1e3")
        );
        assertThrows(
            ApiException.class,
            () -> PurchaseRules.money("1.00001")
        );
    }

    @Test
    void roundsCurrencyConversionHalfUp() {
        assertEquals(
            2L,
            PurchaseRules.convert(
                1L, new BigDecimal("1.5")
            )
        );
    }

    @Test
    void detectsConversionOverflow() {
        assertThrows(
            ArithmeticException.class,
            () -> PurchaseRules.convert(
                Long.MAX_VALUE,
                new BigDecimal("2")
            )
        );
    }

    @Test
    void checksDatesStrictly() {
        assertEquals(
            "2026-10-12",
            PurchaseRules.date("2026-10-12")
        );
        assertThrows(
            ApiException.class,
            () -> PurchaseRules.date("2026-1-2")
        );
        assertThrows(
            java.time.DateTimeException.class,
            () -> PurchaseRules.date("2026-02-30")
        );
    }

    @Test
    void checksPositiveWholeNumbers() {
        assertEquals(
            10L,
            PurchaseRules.positiveWhole("10", "quantity")
        );
        assertThrows(
            ApiException.class,
            () -> PurchaseRules.positiveWhole("0", "quantity")
        );
        assertThrows(
            ApiException.class,
            () -> PurchaseRules.positiveWhole("1.5", "quantity")
        );
    }
}
