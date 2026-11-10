
export const MAX_LONG = 9223372036854775807n;

export function parseExact(text) {
  // Quoted strings are matched first and never altered.
  const protectedText = text.replace(
    /"(?:\\.|[^"\\])*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g,
    token => {
      if (token.startsWith('"')) return token;
      if (/^-?\d+$/.test(token) &&
          !Number.isSafeInteger(Number(token))) {
        return JSON.stringify(token);
      }
      return token;
    }
  );
  return JSON.parse(protectedText);
}

export function stringifyExact(value) {
  if (typeof value === "bigint") return value.toString();
  if (value === null) return "null";
  if (Array.isArray(value)) {
    return "[" + value.map(v =>
      v === undefined ? "null" : stringifyExact(v)
    ).join(",") + "]";
  }
  if (typeof value === "object") {
    return "{" + Object.entries(value)
      .filter(([, v]) => v !== undefined)
      .map(([k, v]) => JSON.stringify(k) + ":" + stringifyExact(v))
      .join(",") + "}";
  }
  if (typeof value === "number" && !Number.isFinite(value)) {
    throw new Error("Invalid numeric value.");
  }
  return JSON.stringify(value);
}

export function integer(value) {
  const text = String(value ?? "").trim();
  if (!/^\d+$/.test(text)) {
    throw new Error("Enter a positive whole number.");
  }
  const result = BigInt(text);
  if (result <= 0n || result > MAX_LONG) {
    throw new Error("Whole number is outside the supported range.");
  }
  return result;
}

export function scaled(value) {
  const text = String(value ?? "").trim();
  if (!/^\d+(?:\.\d{1,4})?$/.test(text)) {
    throw new Error("Enter a non-negative price with up to four decimal places.");
  }
  const [whole, fraction = ""] = text.split(".");
  const result = BigInt(whole) * 10000n +
    BigInt(fraction.padEnd(4, "0"));

  if (result > MAX_LONG) {
    throw new Error("Amount exceeds the backend's supported range.");
  }
  return result;
}

export function amountInput(value) {
  if (value == null) return "";
  const amount = BigInt(String(value));
  return `${amount / 10000n}.${String(amount % 10000n).padStart(4, "0")}`;
}

export function money(value, currency = "") {
  if (value == null) return "Unknown";
  let amount = BigInt(String(value));
  const negative = amount < 0n;
  if (negative) amount = -amount;

  const whole = String(amount / 10000n)
    .replace(/\B(?=(\d{3})+(?!\d))/g, ",");

  const fraction = String(amount % 10000n).padStart(4, "0");
  const visible = fraction.endsWith("00")
    ? fraction.slice(0, 2) : fraction;

  return `${currency ? currency + " " : ""}${negative ? "-" : ""}${whole}.${visible}`;
}

export function same(a, b) {
  return a != null && b != null && String(a) === String(b);
}

export function label(value) {
  return String(value ?? "")
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, c => c.toUpperCase());
}

export function time(value) {
  if (!value) return "Not available";
  let text = String(value);
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(text)) {
    text = text.replace(" ", "T") + "Z";
  }
  const date = new Date(text);
  return Number.isNaN(date.getTime())
    ? "Not available" : date.toLocaleString();
}

export function checkPassword(value) {
  if (!value || value.length < 12 || value.length > 64 || !value.trim()) {
    throw new Error("Password must contain 12–64 characters.");
  }
  if (new TextEncoder().encode(value).length > 72) {
    throw new Error("Password must not exceed 72 UTF-8 bytes.");
  }
}

export function sanitizeEvidence(value) {
  if (Array.isArray(value)) return value.map(sanitizeEvidence);
  if (!value || typeof value !== "object") return value;
  return Object.fromEntries(
    Object.entries(value)
      .filter(([key]) =>
        !/Ids?$/.test(key) &&
        !/deleted|deletion|password|credential|groupKey/i.test(key)
      )
      .map(([key, v]) => [key, sanitizeEvidence(v)])
  );
}
