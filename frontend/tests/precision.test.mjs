
import test from "node:test";
import assert from "node:assert/strict";
import {
  parseExact, stringifyExact, scaled, integer, money
} from "../src/utils.js";

test("preserves unsafe integer response values", () => {
  const data = parseExact(
    '{"amount":9223372036854775807,"count":6,"text":"9223372036854775807"}'
  );
  assert.equal(data.amount, "9223372036854775807");
  assert.equal(data.count, 6);
  assert.equal(data.text, "9223372036854775807");
});

test("does not alter escaped quoted strings", () => {
  const original = { text: 'Example " 9223372036854775807' };
  assert.deepEqual(parseExact(JSON.stringify(original)), original);
});

test("serializes exact monetary integer requests", () => {
  assert.equal(
    stringifyExact({ amountScaled: 9223372036854775807n }),
    '{"amountScaled":9223372036854775807}'
  );
});

test("converts and formats amounts exactly", () => {
  assert.equal(scaled("10.20"), 102000n);
  assert.equal(scaled("0.0001"), 1n);
  assert.equal(money("102000", "USD"), "USD 10.20");
});

test("rejects unsupported input", () => {
  assert.throws(() => scaled("1e3"));
  assert.throws(() => scaled("1.00001"));
  assert.throws(() => scaled("922337203685477.5808"));
  assert.throws(() => integer("0"));
});
