package com.procurement.monitor.service;

import com.procurement.monitor.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseCsvParserTest {

    private static final String HEADER =
        "source_po_number,line_number,order_date,department,"
        + "buyer_email,supplier_name,item_code,quantity,"
        + "unit_price,currency\n";

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void parsesBomAndQuotedSupplier() {
        var parser = new PurchaseCsvParser(10);

        var rows = parser.parse(bytes(
            "\uFEFF" + HEADER
                + "PO-1,1,2026-10-12,Operations,"
                + "buyer@example.com,\"Supplier, Inc\","
                + "PEN,2,10.20,USD\n"
        ));

        assertEquals(1, rows.size());
        assertEquals(
            "Supplier, Inc",
            rows.getFirst().supplierName()
        );
        assertEquals(
            102000L,
            rows.getFirst().unitPriceScaled()
        );
    }

    @Test
    void rejectsDuplicateLines() {
        var parser = new PurchaseCsvParser(10);

        String row =
            "PO-1,1,2026-10-12,Operations,"
                + "buyer@example.com,Supplier,PEN,2,2.00,USD\n";

        assertThrows(
            ApiException.class,
            () -> parser.parse(bytes(HEADER + row + row))
        );
    }

    @Test
    void rejectsInvalidUtf8() {
        var parser = new PurchaseCsvParser(10);

        assertThrows(
            ApiException.class,
            () -> parser.parse(
                new byte[] {(byte) 0xC3, (byte) 0x28}
            )
        );
    }

    @Test
    void enforcesRowLimit() {
        var parser = new PurchaseCsvParser(1);

        String first =
            "PO-1,1,2026-10-12,Operations,"
                + "buyer@example.com,Supplier,PEN,2,2.00,USD\n";

        String second =
            "PO-2,1,2026-10-12,Operations,"
                + "buyer@example.com,Supplier,PEN,2,2.00,USD\n";

        assertThrows(
            ApiException.class,
            () -> parser.parse(bytes(HEADER + first + second))
        );
    }
}
