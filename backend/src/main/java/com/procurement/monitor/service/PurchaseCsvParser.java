package com.procurement.monitor.service;

import com.procurement.monitor.exception.ApiException;
import com.procurement.monitor.validation.Checks;
import com.procurement.monitor.validation.PurchaseRules;
import org.apache.commons.csv.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

@Component
public class PurchaseCsvParser {

    public record Row(
        String poNumber,
        long lineNumber,
        String orderDate,
        String department,
        String buyerEmail,
        String supplierName,
        String itemCode,
        long quantity,
        long unitPriceScaled,
        String currency
    ) {
        public List<Object> orderHeader() {
            return List.of(
                orderDate,
                department,
                buyerEmail,
                supplierName,
                currency
            );
        }
    }

    public static final List<String> HEADERS = List.of(
        "source_po_number",
        "line_number",
        "order_date",
        "department",
        "buyer_email",
        "supplier_name",
        "item_code",
        "quantity",
        "unit_price",
        "currency"
    );

    private final int maximumRows;

    public PurchaseCsvParser(
        @Value("${app.purchase.max-csv-rows:5000}")
        int maximumRows
    ) {
        if (maximumRows < 1 || maximumRows > 50000) {
            throw new IllegalStateException(
                "CSV row limit must be between 1 and 50000"
            );
        }

        this.maximumRows = maximumRows;
    }

    public List<Row> parse(byte[] bytes) {
        String text;

        try {
            text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        } catch (CharacterCodingException exception) {
            throw bad("CSV must use valid UTF-8");
        }

        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        CSVFormat format = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(false)
            .setDuplicateHeaderMode(
                DuplicateHeaderMode.DISALLOW
            )
            .get();

        List<Row> result = new ArrayList<>();
        Map<String, List<Object>> headersByPo = new HashMap<>();
        Map<String, Set<Long>> linesByPo = new HashMap<>();

        try (
            CSVParser parser = format.parse(
                new StringReader(text)
            )
        ) {
            List<String> actualHeaders =
                parser.getHeaderNames();

            Checks.require(
                actualHeaders.size() == HEADERS.size()
                    && new HashSet<>(actualHeaders)
                        .equals(new HashSet<>(HEADERS)),
                "CSV headers must be exactly: "
                    + String.join(",", HEADERS)
            );

            for (CSVRecord record : parser) {
                Checks.require(
                    result.size() < maximumRows,
                    "CSV exceeds the configured row limit"
                );

                try {
                    Checks.require(
                        record.isConsistent(),
                        "CSV record has the wrong column count"
                    );

                    String currency = value(
                        record, "currency", 3
                    );

                    Checks.require(
                        currency.matches("[A-Z]{3}"),
                        "currency must contain three uppercase letters"
                    );

                    Row row = new Row(
                        value(record, "source_po_number", 100),
                        PurchaseRules.positiveWhole(
                            record.get("line_number").trim(),
                            "line_number"
                        ),
                        PurchaseRules.date(
                            record.get("order_date").trim()
                        ),
                        value(record, "department", 150),
                        value(record, "buyer_email", 200)
                            .toLowerCase(Locale.ROOT),
                        value(record, "supplier_name", 200),
                        value(record, "item_code", 100),
                        PurchaseRules.positiveWhole(
                            record.get("quantity").trim(),
                            "quantity"
                        ),
                        PurchaseRules.money(
                            record.get("unit_price").trim()
                        ),
                        currency
                    );

                    // Avoid an unrepresentable line total.
                    Math.multiplyExact(
                        row.quantity(),
                        row.unitPriceScaled()
                    );

                    List<Object> previous =
                        headersByPo.putIfAbsent(
                            row.poNumber(),
                            row.orderHeader()
                        );

                    Checks.require(
                        previous == null
                            || previous.equals(row.orderHeader()),
                        "Repeated PO number has inconsistent header data"
                    );

                    Checks.require(
                        linesByPo.computeIfAbsent(
                            row.poNumber(),
                            ignored -> new HashSet<>()
                        ).add(row.lineNumber()),
                        "Duplicate PO line number in the CSV"
                    );

                    result.add(row);

                } catch (
                    ApiException
                    | IllegalArgumentException
                    | ArithmeticException
                    | java.time.DateTimeException exception
                ) {
                    throw bad(
                        "Invalid CSV data record "
                            + record.getRecordNumber()
                            + ": "
                            + safeMessage(exception)
                    );
                }
            }

        } catch (ApiException exception) {
            throw exception;
        } catch (
            java.io.IOException
            | java.io.UncheckedIOException
            | IllegalArgumentException exception
        ) {
            throw bad(
                "CSV structure or quoting is invalid"
            );
        }

        Checks.require(
            !result.isEmpty(),
            "CSV contains no purchase rows"
        );

        return List.copyOf(result);
    }

    private String value(
        CSVRecord record,
        String name,
        int maximum
    ) {
        return PurchaseRules.text(
            record.get(name),
            maximum,
            name
        );
    }

    private String safeMessage(Exception exception) {
        if (exception instanceof ApiException) {
            return exception.getMessage();
        }

        return "invalid date, whole number, price, or numeric range";
    }

    private ApiException bad(String message) {
        return new ApiException(
            HttpStatus.BAD_REQUEST, message
        );
    }
}
