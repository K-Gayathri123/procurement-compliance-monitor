
import React, {
  createContext, useContext, useEffect, useRef, useState
} from "react";
import { createRoot } from "react-dom/client";
import {
  Icon, Button, Badge, Panel, Notice, Empty, Skeleton,
  Details, Table, Field, SelectField, FormDialog
} from "./ui";
import {
  parseExact, stringifyExact, integer, scaled, amountInput,
  money, same, label, time, checkPassword, sanitizeEvidence
} from "./utils";
import "./styles.css";

const Context = createContext(null);
const useApp = () => useContext(Context);

const ALL = ["ADMIN", "PROCUREMENT_OPERATOR", "BUYER", "REVIEWER", "ANALYST"];
const OPS = ["ADMIN", "PROCUREMENT_OPERATOR"];
const TERMINAL = ["APPROVED_EXCEPTION", "CONFIRMED_VIOLATION"];
const STATUSES = ["OPEN", "EXPLANATION_SUBMITTED", "EVIDENCE_REQUESTED", ...TERMINAL];
const TYPES = [
  "SUPPLIER_UNRESOLVED", "CONTRACT_NOT_FOUND", "CONTRACT_AMBIGUOUS",
  "TIER_NOT_UNIQUE", "EXCHANGE_RATE_MISSING", "EXCHANGE_RATE_AMBIGUOUS",
  "PRICE_ABOVE_BAND", "PRICE_BELOW_BAND", "POSSIBLE_SPLIT_PURCHASE"
];

const CONFIG = {
  purchases: {
    title: "Purchases", route: "purchase-orders", key: "poId", icon: "bag", roles: ALL,
    description: "Inspect purchases and the evidence behind contract matching.",
    filters: ["query", "supplierId", "buyerUserId", "uploadId"],
    sorts: { sourcePoNumber: "Purchase order", orderDate: "Order date" }
  },
  exceptions: {
    title: "Exceptions", route: "exceptions", key: "exceptionId", icon: "flag", roles: ALL,
    description: "Review findings, collect explanations, and record independent decisions.",
    filters: ["status", "type"]
  },
  uploads: {
    title: "Purchase uploads", route: "uploads", key: "uploadId", icon: "upload", roles: OPS,
    description: "Import purchase CSVs and inspect completed batches.", filters: []
  },
  suppliers: {
    title: "Suppliers", route: "suppliers", key: "supplierId", icon: "building", roles: ALL,
    description: "Manage supplier names and operational availability.",
    filters: ["query", "active", "includeDeleted"],
    sorts: { canonicalName: "Supplier name" }
  },
  aliases: {
    title: "Supplier aliases", route: "supplier-aliases", key: "aliasId", icon: "exchange", roles: ALL,
    description: "Match uploaded supplier names to canonical suppliers.",
    filters: ["query", "supplierId", "includeDeleted"],
    sorts: { aliasName: "Alias name" }
  },
  contracts: {
    title: "Contracts", route: "contracts", key: "contractId", icon: "file", roles: ALL,
    description: "Maintain agreements, applicable dates, and approved terms.",
    filters: ["query", "supplierId", "status"],
    sorts: { contractNumber: "Contract number", effectiveFrom: "Effective date" }
  },
  tiers: {
    title: "Price tiers", route: "contract-price-tiers", key: "tierId", icon: "grid", roles: ALL,
    description: "Define quantity ranges and permitted unit prices.",
    filters: ["contractId"], sorts: { minQuantity: "Minimum quantity" }
  },
  rates: {
    title: "Exchange rates", route: "exchange-rates", key: "rateId", icon: "exchange", roles: ALL,
    description: "Maintain exact-date direct currency conversion evidence.",
    filters: ["fromCurrency", "toCurrency", "rateDate"],
    sorts: { rateDate: "Rate date", rateSource: "Source" }
  },
  users: {
    title: "User administration", route: "users", key: "userId", icon: "users", roles: ["ADMIN"],
    description: "Manage accounts and application permissions.",
    filters: ["query", "includeDeleted"], sorts: { fullName: "Name", email: "Email" }
  }
};

function App() {
  const [user, setUser] = useState(null);
  const token = useRef("");
  const [screen, setScreen] = useState("purchases");
  const [mobile, setMobile] = useState(false);
  const [toast, setToast] = useState(null);
  const timer = useRef(null);
  const [lookup, setLookup] = useState({
    suppliers: [], contracts: [], users: [], roles: [], uploads: []
  });
  const [lookupError, setLookupError] = useState("");
  const evidenceCache = useRef(new Map());
  const evidenceFlight = useRef(null);

  function notify(message, danger = false) {
    clearTimeout(timer.current);
    setToast({ message, danger });
    timer.current = setTimeout(() => setToast(null), danger ? 14000 : 6000);
  }

  function logout() {
    token.current = "";
    setUser(null);
    setLookup({ suppliers: [], contracts: [], users: [], roles: [], uploads: [] });
    setLookupError("");
    evidenceCache.current.clear();
    setMobile(false);
  }

  async function api(path, options = {}, override) {
    const bearer = override === undefined ? token.current : override;
    const method = options.method || "GET";
    const headers = {};
    if (bearer) headers.Authorization = `Bearer ${bearer}`;
    let body;
    if (options.body instanceof FormData) body = options.body;
    else if (options.body !== undefined) {
      headers["Content-Type"] = "application/json";
      body = stringifyExact(options.body);
    }

    let response;
    try {
      response = await fetch(`/api${path}`, { method, headers, body });
    } catch {
      throw new Error(
        "Cannot reach the backend." +
        (method === "GET" ? "" :
          " The request may have reached the server. Inspect records before retrying.")
      );
    }

    let data;
    try {
      const text = await response.text();
      data = text ? parseExact(text) : null;
    } catch {
      throw new Error(
        method === "GET" ? "Cannot read the server response." :
        "Cannot read the response. Inspect records before retrying."
      );
    }

    if (!response.ok) {
      if (response.status === 401 && bearer && bearer === token.current) {
        logout();
        notify("Your session is unavailable. Please sign in again.", true);
      }
      const fallback = response.status === 403 ? "You do not have permission for this operation."
        : response.status === 413 ? "The uploaded file is too large."
        : "The request could not be completed.";
      throw new Error(
        (data?.detail || data?.error || fallback) +
        (data?.errors?.length ? " " + data.errors.join(" · ") : "")
      );
    }
    return data;
  }

  async function all(route, extra = "") {
    const result = [];
    let page = 0;
    while (true) {
      const data = await api(`/${route}?page=${page}&size=100${extra}`);
      if (Array.isArray(data)) return data;
      result.push(...data.content);
      page++;
      if (page >= Number(data.totalPages)) return result;
    }
  }

  async function reload(account = user) {
    if (!account) return;
    const requestedToken = token.current;
    const admin = account.roles.includes("ADMIN");
    const operator = account.roles.some(r => OPS.includes(r));

    const jobs = [
      ["suppliers", () => all("suppliers", "&includeDeleted=true")],
      ["contracts", () => all("contracts")],
      ["users", () => admin ? all("users", "&includeDeleted=true") : Promise.resolve([account])],
      ["roles", () => admin ? api("/roles") : Promise.resolve([])],
      ["uploads", () => operator ? all("uploads") : Promise.resolve([])]
    ];
    const results = await Promise.allSettled(jobs.map(([, job]) => job()));
    if (requestedToken !== token.current) return;

    const next = {};
    const errors = [];
    results.forEach((r, i) => {
      if (r.status === "fulfilled") next[jobs[i][0]] = r.value;
      else errors.push(`${label(jobs[i][0])}: ${r.reason.message}`);
    });
    setLookup(old => ({ ...old, ...next }));
    setLookupError(errors.join(" "));
  }

  async function findEvidence(evaluationId) {
    const cached = evidenceCache.current.get(String(evaluationId));
    if (cached) return cached;

    // Existing backend exposes evaluations through purchase lines.
    // There is no invented /evaluations/{id} endpoint.
    // This lookup is explicit/on-demand, suitable for a small demo dataset.
    if (evidenceFlight.current) await evidenceFlight.current;
    const afterWait = evidenceCache.current.get(String(evaluationId));
    if (afterWait) return afterWait;

    const requestedToken = token.current;
    const task = (async () => {
      const orders = await all("purchase-orders");
      for (const order of orders) {
        if (requestedToken !== token.current) throw new Error("Session changed.");
        const detail = await api(`/purchase-orders/${order.poId}`);
        for (const line of detail.lines) {
          const evaluations = await api(`/purchase-orders/lines/${line.poLineId}/evaluations`);
          for (const evaluation of evaluations) {
            if (requestedToken !== token.current) throw new Error("Session changed.");
            evidenceCache.current.set(String(evaluation.evaluationId), {
              order: detail.order, line, evaluation
            });
          }
          if (evidenceCache.current.has(String(evaluationId))) {
            return evidenceCache.current.get(String(evaluationId));
          }
        }
      }
      throw new Error("Matching evidence was not found among accessible purchases.");
    })();

    evidenceFlight.current = task;
    try { return await task; }
    finally { if (evidenceFlight.current === task) evidenceFlight.current = null; }
  }

  const has = (...roles) => !!user?.roles.some(r => roles.includes(r));
  const names = {
    supplier: id => lookup.suppliers.find(s => same(s.supplierId, id))?.canonicalName || "Supplier unavailable",
    contract: id => lookup.contracts.find(c => same(c.contractId, id))?.contractNumber || "Contract unavailable",
    person: id => lookup.users.find(u => same(u.userId, id))?.fullName ||
      (same(user?.userId, id) ? user.fullName : "Review participant"),
    currency: id => lookup.contracts.find(c => same(c.contractId, id))?.currency || ""
  };

  async function login(email, password) {
    const auth = await api("/auth/login", {
      method: "POST", body: { email, password }
    }, "");
    const account = await api("/auth/me", {}, auth.accessToken);
    token.current = auth.accessToken;
    setUser(account);
    setScreen(account.roles.some(r => ["ADMIN", "ANALYST"].includes(r)) ? "analytics" : "purchases");
    reload(account);
  }

  if (!user) return <>
    <Login onLogin={login} />
    {toast && <div className="toast error" role="alert">{toast.message}</div>}
  </>;

  const value = { user, api, all, lookup, names, has, notify, reload, findEvidence };
  const groups = [
    ["WORKSPACE", ["analytics", "uploads", "purchases", "exceptions"]],
    ["MASTER DATA", ["suppliers", "aliases", "contracts", "tiers", "rates"]],
    ["ADMINISTRATION", ["users"]]
  ];
  const title = screen === "analytics" ? "Analytics overview"
    : screen === "account" ? "My account" : CONFIG[screen].title;

  return <Context.Provider value={value}>
    <div className={`app ${mobile ? "menu-open" : ""}`}>
      {mobile && <button className="scrim" aria-label="Close navigation" onClick={() => setMobile(false)} />}
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark"><Icon name="shield" size={23} /></span>
          <div>ProcureGuard<small>Compliance workspace</small></div>
        </div>
        {groups.map(([heading, pages]) => {
          const visible = pages.filter(key => key === "analytics"
            ? has("ADMIN", "ANALYST") : CONFIG[key].roles.some(r => has(r)));
          return visible.length ? <nav key={heading} aria-label={heading}>
            <p className="nav-heading">{heading}</p>
            {visible.map(key => <button key={key}
              className={`nav-item ${screen === key ? "selected" : ""}`}
              aria-current={screen === key ? "page" : undefined}
              onClick={() => { setScreen(key); setMobile(false); }}>
              <Icon name={key === "analytics" ? "grid" : CONFIG[key].icon} />
              {key === "analytics" ? "Analytics" : CONFIG[key].title}
            </button>)}
          </nav> : null;
        })}
        <div className="sidebar-bottom">
          <p><span className="status-dot" /> Local application</p>
          <button className="nav-item" onClick={logout}><Icon name="logout" /> Sign out</button>
        </div>
      </aside>

      <div className="workspace">
        <header className="topbar">
          <div className="actions">
            <Button variant="ghost mobile-menu" aria-label="Open navigation"
              onClick={() => setMobile(true)}><Icon name="menu" /></Button>
            <span className="breadcrumb">Workspace <Icon name="arrow" size={12} /><strong>{title}</strong></span>
          </div>
          <div className="account-area">
            <span className="role-label">{user.roles.map(label).join(" · ")}</span>
            <button className="account-button" onClick={() => setScreen("account")}>
              <span className="avatar">{user.fullName?.slice(0, 1).toUpperCase()}</span>
              {user.fullName}
            </button>
          </div>
        </header>

        <main className="content">
          {lookupError && <Notice danger>
            Some dropdown labels could not be loaded. {lookupError}
            <Button variant="secondary" onClick={() => reload()}>Retry</Button>
          </Notice>}

          {screen === "analytics" ? <Analytics />
            : screen === "account" ? <Account onLogout={logout} />
            : <Collection key={`${user.userId}:${screen}`} name={screen} />}
        </main>
      </div>

      {toast && <div className={`toast ${toast.danger ? "error" : ""}`}
        role={toast.danger ? "alert" : "status"}>
        <Icon name={toast.danger ? "alert" : "check"} />
        <span>{toast.message}</span>
        <button aria-label="Dismiss notification" onClick={() => setToast(null)}><Icon name="close" size={16} /></button>
      </div>}
    </div>
  </Context.Provider>;
}

function Login({ onLogin }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [show, setShow] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const lock = useRef(false);

  async function submit(e) {
    e.preventDefault();
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError("");
    try { await onLogin(email.trim(), password); setPassword(""); }
    catch (e) { setError(e.message); }
    finally { lock.current = false; setBusy(false); }
  }

  return <div className="login-layout">
    <section className="login-story">
      <div className="brand"><span className="brand-mark"><Icon name="shield" /></span>
        <div>ProcureGuard<small>Procurement intelligence</small></div></div>
      <div className="story-content">
        <span className="eyebrow">CONTRACT COMPLIANCE</span>
        <h1>Clear evidence.<br />Confident decisions.</h1>
        <p>A focused workspace for purchase checks, independent reviews,
          and responsible savings analysis.</p>
        {[
          "Match purchases to approved contract terms",
          "Preserve the evidence behind each finding",
          "Keep estimates and actual recoveries separate"
        ].map(text => <div className="story-point" key={text}><Icon name="check" />{text}</div>)}
      </div>
      <small>Procurement Spend Leakage & Contract Compliance Monitor</small>
    </section>
    <section className="login-form-area">
      <form className="login-form" onSubmit={submit}>
        <span className="eyebrow">WELCOME BACK</span>
        <h2>Sign in to your workspace</h2>
        <p className="muted">Use an existing application account to continue.</p>
        <Field label="Email address" type="email" autoComplete="username" required maxLength={200}
          value={email} onChange={e => setEmail(e.target.value)} />
        <label className="field"><span>Password</span>
          <div className="password-input">
            <input type={show ? "text" : "password"} autoComplete="current-password"
              required value={password} onChange={e => setPassword(e.target.value)} />
            <button type="button" onClick={() => setShow(!show)}
              aria-label={show ? "Hide password" : "Show password"}>{show ? "Hide" : "Show"}</button>
          </div>
        </label>
        {error && <div className="form-error" role="alert">{error}</div>}
        <Button disabled={busy} variant="wide">{busy ? "Signing in…" : "Sign in"}</Button>
        <p className="login-note"><Icon name="shield" size={15} />
          Refreshing the page clears the in-memory session.</p>
      </form>
    </section>
  </div>;
}

function Heading({ title, text, actions }) {
  return <div className="page-heading">
    <div><span className="eyebrow">PROCUREMENT WORKSPACE</span><h1>{title}</h1><p>{text}</p></div>
    <div className="actions">{actions}</div>
  </div>;
}

function Analytics() {
  const { api } = useApp();
  const [data, setData] = useState(null);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");
  const [currency, setCurrency] = useState("");

  async function load() {
    setBusy(true); setError("");
    try {
      const result = await api("/analytics/summary");
      setData(result);
      const list = currencies(result);
      setCurrency(old => list.includes(old) ? old : list[0] || "");
    } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }
  useEffect(() => { load(); }, []);

  function currencies(value) {
    return [...new Set([
      ...Object.keys(value.spendScaledByCurrency),
      ...Object.keys(value.potentialLeakageScaledByCurrency),
      ...Object.keys(value.recoverableEstimateScaledByCurrency),
      ...Object.keys(value.actualRecoveryScaledByCurrency)
    ])].sort();
  }

  return <>
    <Heading title="Analytics overview"
      text="Operational findings and recovery evidence, separated by currency."
      actions={<Button icon="refresh" variant="secondary" disabled={busy} onClick={load}>Refresh</Button>} />
    {error && <Notice danger>{error}</Notice>}
    {busy ? <Skeleton /> : data && <>
      <div className="metric-grid">
        {[
          ["Purchase orders", data.orderCount, "bag"],
          ["Purchase lines", data.lineCount, "file"],
          ["Price-compliant lines", data.compliantLineCount, "shield"],
          ["Exceptions", data.exceptionCount, "flag"],
          ["Possible split groups", data.possibleSplitGroupCount, "grid"]
        ].map(([title, count, icon]) => <div className="metric" key={title}>
          <span className="metric-icon"><Icon name={icon} /></span>
          <p>{title}</p><strong>{String(count)}</strong>
        </div>)}
      </div>

      <Panel title="Financial measures" subtitle="Amounts are never combined across currencies."
        actions={<SelectField label="Currency" value={currency}
          options={currencies(data).map(c => ({ value: c, label: c }))}
          onChange={e => setCurrency(e.target.value)} />}>
        {!currencies(data).length ? <Empty title="No amounts recorded yet" text="Import purchases to populate financial measures." /> :
          <div className="financial-grid">
            {[
              ["Recorded spend", "spendScaledByCurrency", "Original purchase currency"],
              ["Potential leakage", "potentialLeakageScaledByCurrency", "Historical finding, not confirmed loss"],
              ["Recoverable estimate", "recoverableEstimateScaledByCurrency", "Not realized savings"],
              ["Actual recovery", "actualRecoveryScaledByCurrency", "Recorded with manual evidence"]
            ].map(([title, field, note]) => <div className="financial" key={field}>
              <p>{title}</p><strong>{data[field][currency] == null
                ? "No records" : money(data[field][currency], currency)}</strong><small>{note}</small>
            </div>)}
          </div>}
      </Panel>

      <div className="two-columns">
        <Panel title="Review distribution" subtitle="Exception statuses are separate from price-compliance results.">
          {Object.keys(data.exceptionsByReviewStatus).length ?
            Object.entries(data.exceptionsByReviewStatus).map(([status, count]) =>
              <div className="distribution" key={status}>
                <div><Badge value={status} /><strong>{String(count)}</strong></div>
                <progress value={Number(count)} max={Math.max(1, Number(data.exceptionCount))}
                  aria-label={label(status)} />
              </div>) :
            <Empty title="No review findings" text="Statuses appear after exceptions are created." />}
        </Panel>
        <Panel title="Reporting boundaries">
          <Notice>Estimates and actual recoveries must not be added together.</Notice>
          <p className="body-copy">A compliant price evaluation may also have a possible split-purchase flag.
            Such flags are review signals, not proof of misconduct.</p>
          <div className="unavailable"><Icon name="clock" />
            <div><strong>Confirmed leakage total unavailable</strong>
              <p>The current summary API does not provide this measure.</p></div>
          </div>
        </Panel>
      </div>
    </>}
  </>;
}



function columns(name, names) {
  const state = r => r.deletedAt ? "ARCHIVED" : r.active ? "ACTIVE" : "INACTIVE";

  const definitions = {
    purchases: [
      ["Purchase order", r => <strong>{r.sourcePoNumber}</strong>],
      ["Supplier", r => r.originalSupplierName],
      ["Department", r => r.department],
      ["Order date", r => r.orderDate],
      ["Currency", r => r.currency]
    ],
    exceptions: [
      ["Finding", r => <Badge value={r.exceptionType} />],
      ["Review status", r => <Badge value={r.reviewStatus} />],
      ["Summary", r => <span className="summary-text">{
        r.exceptionType === "POSSIBLE_SPLIT_PURCHASE"
          ? "Possible quantity-discount consolidation opportunity"
          : r.reason
      }</span>]
    ],
    uploads: [
      ["Source file", r => <strong>{r.fileName}</strong>],
      ["Processing status", r => <Badge value={r.processingStatus} />],
      ["Imported", r => time(r.uploadedAt)]
    ],
    suppliers: [
      ["Supplier", r => <strong>{r.canonicalName}</strong>],
      ["Availability", r => <Badge value={state(r)} />]
    ],
    aliases: [
      ["Alias", r => <strong>{r.aliasName}</strong>],
      ["Supplier", r => names.supplier(r.supplierId)],
      ["Availability", r => <Badge value={r.deletedAt ? "ARCHIVED" : "ACTIVE"} />]
    ],
    contracts: [
      ["Contract", r => <strong>{r.contractNumber}</strong>],
      ["Supplier", r => names.supplier(r.supplierId)],
      ["Item", r => r.itemCode],
      ["Effective from", r => r.effectiveFrom],
      ["Effective to", r => r.effectiveTo],
      ["Currency", r => r.currency],
      ["Status", r => <Badge value={r.status} />]
    ],
    tiers: [
      ["Contract", r => names.contract(r.contractId)],
      ["Quantity range", r => `${r.minQuantity} – ${r.maxQuantity ?? "Unbounded"}`],
      ["Reference price", r => money(r.referenceUnitPriceScaled, names.currency(r.contractId))],
      ["Allowed minimum", r => money(r.allowedMinPriceScaled, names.currency(r.contractId))],
      ["Allowed maximum", r => money(r.allowedMaxPriceScaled, names.currency(r.contractId))]
    ],
    rates: [
      ["Currency pair", r => <strong>{r.fromCurrency} → {r.toCurrency}</strong>],
      ["Rate date", r => r.rateDate],
      ["Conversion rate", r => r.conversionRate],
      ["Source", r => r.rateSource]
    ],
    users: [
      ["Name", r => <strong>{r.fullName}</strong>],
      ["Email address", r => r.email],
      ["Account access", r => <Badge value={state(r)} />]
    ]
  };

  return definitions[name].map(([title, render]) => ({ title, render }));
}

function singular(name) {
  return ({
    suppliers: "supplier",
    aliases: "alias",
    contracts: "contract",
    tiers: "price tier",
    rates: "exchange rate",
    users: "user"
  })[name] || "record";
}

function Collection({ name }) {
  const app = useApp();
  const config = CONFIG[name];
  const { api, lookup, names, has, notify, reload } = app;

  const [rows, setRows] = useState([]);
  const [pagination, setPagination] = useState(null);
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({});
  const [applied, setApplied] = useState({});
  const [sort, setSort] = useState(Object.keys(config.sorts || {})[0] || "");
  const [direction, setDirection] = useState("ASC");
  const [revision, setRevision] = useState(0);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState(null);
  const [dialog, setDialog] = useState(null);

  useEffect(() => {
    let alive = true;

    async function load() {
      setBusy(true);
      setError("");

      const params = new URLSearchParams({
        page: String(page),
        size: "20"
      });

      for (const [key, value] of Object.entries(applied)) {
        if (value !== "" && value != null) params.set(key, String(value));
      }

      if (sort) {
        params.set("sort", sort);
        if (name !== "users") params.set("direction", direction);
      }

      try {
        const data = await api(`/${config.route}?${params}`);
        if (alive) {
          setRows(data.content);
          setPagination(data);
        }
      } catch (e) {
        if (alive) setError(e.message);
      } finally {
        if (alive) setBusy(false);
      }
    }

    load();
    return () => { alive = false; };
  }, [page, applied, sort, direction, revision]);

  const supplierOptions = lookup.suppliers.map(s => ({
    value: s.supplierId,
    label: s.canonicalName
  }));

  const contractOptions = lookup.contracts.map(c => ({
    value: c.contractId,
    label: `${c.contractNumber} · ${c.itemCode}`
  }));

  function filterField(field) {
    if (field === "buyerUserId" && !has("ADMIN")) return null;
    if (field === "uploadId" && !has(...OPS)) return null;

    const titles = {
      query: "Search",
      supplierId: "Supplier",
      buyerUserId: "Buyer",
      uploadId: "Source batch",
      contractId: "Contract",
      active: "Availability",
      includeDeleted: "Record scope",
      status: "Status",
      type: "Finding type",
      fromCurrency: "From currency",
      toCurrency: "To currency",
      rateDate: "Rate date"
    };

    const options = {
      supplierId: supplierOptions,
      contractId: contractOptions,
      buyerUserId: lookup.users.map(u => ({
        value: u.userId,
        label: u.fullName
      })),
      uploadId: lookup.uploads.map(u => ({
        value: u.uploadId,
        label: `${u.fileName} · ${time(u.uploadedAt)}`
      })),
      active: [
        { value: "true", label: "Active" },
        { value: "false", label: "Inactive" }
      ],
      includeDeleted: [
        { value: "false", label: "Current records" },
        { value: "true", label: "Include archived" }
      ],
      status: (name === "contracts"
        ? ["DRAFT", "APPROVED", "CANCELLED"] : STATUSES)
        .map(v => ({ value: v, label: label(v) })),
      type: TYPES.map(v => ({ value: v, label: label(v) }))
    };

    const props = {
      value: filters[field] ?? "",
      required: false,
      onChange: e => setFilters({ ...filters, [field]: e.target.value })
    };

    return options[field]
      ? <SelectField key={field} label={titles[field]}
          options={options[field]} {...props} />
      : <Field key={field} label={titles[field]}
          type={field === "rateDate" ? "date" : "text"}
          maxLength={field === "query" ? 200 : 100}
          placeholder={field === "query" ? "Search records…" : undefined}
          {...props} />;
  }

  function createRecord() {
    const definition = recordForm(name, null, app);
    setDialog({
      title: `Create ${singular(name)}`,
      fields: definition.fields,
      initial: definition.initial,
      onSubmit: async values => {
        await api(`/${config.route}`, {
          method: "POST",
          body: definition.build(values)
        });

        setDialog(null);
        notify("Record created successfully.");
        setPage(0);
        setRevision(r => r + 1);
        reload();
      }
    });
  }

  async function changed(deleted = false) {
    setRevision(r => r + 1);
    reload();

    if (deleted) {
      setSelected(null);
      return;
    }

    if (selected) {
      try {
        const latest = await api(
          `/${config.route}/${selected[config.key]}`
        );
        setSelected(latest);
      } catch (e) {
        notify(`Saved, but refreshing the detail failed: ${e.message}`, true);
      }
    }
  }

  const administrable = [
    "suppliers", "aliases", "contracts", "tiers", "rates", "users"
  ].includes(name);

  return <>
    <Heading title={config.title} text={config.description}
      actions={<>
        <Button icon="refresh" variant="secondary" disabled={busy}
          onClick={() => setRevision(r => r + 1)}>
          Refresh
        </Button>
        {has("ADMIN") && administrable &&
          <Button icon="plus" onClick={createRecord}>
            Create {singular(name)}
          </Button>}
      </>}
    />

    {name === "uploads" &&
      <UploadBox onComplete={() => {
        setPage(0);
        setRevision(r => r + 1);
        reload();
      }} />}

    <Panel title={name === "uploads" ? "Import history" : "Records"}
      subtitle="Business fields only—internal identifiers are hidden.">

      {!!config.filters.length &&
        <form className="filterbar" onSubmit={e => {
          e.preventDefault();
          setPage(0);
          setApplied({ ...filters });
        }}>
          {config.filters.map(filterField)}
          <Button type="submit" icon="search" variant="secondary">Apply</Button>
          <Button type="button" variant="ghost" onClick={() => {
            setFilters({});
            setApplied({});
            setPage(0);
          }}>Clear</Button>
        </form>}

      {config.sorts &&
        <div className="sortbar">
          <span className="muted">Sort by</span>
          <select aria-label="Sort field" value={sort}
            onChange={e => { setSort(e.target.value); setPage(0); }}>
            {Object.entries(config.sorts).map(([value, title]) =>
              <option key={value} value={value}>{title}</option>)}
          </select>

          {name !== "users" &&
            <select aria-label="Sort direction" value={direction}
              onChange={e => { setDirection(e.target.value); setPage(0); }}>
              <option value="ASC">Ascending</option>
              <option value="DESC">Descending</option>
            </select>}
        </div>}

      {error ? <Notice danger>{error}</Notice>
        : busy ? <Skeleton />
        : <Table rows={rows} columns={columns(name, names)}
            rowKey={config.key} onOpen={setSelected} />}

      {!busy && !error && pagination &&
        <div className="pagination">
          <span>{String(pagination.totalElements)} records · Page {page + 1}
            {Number(pagination.totalPages) > 0
              ? ` of ${pagination.totalPages}` : ""}
          </span>
          <div className="actions">
            <Button variant="secondary" disabled={page === 0}
              onClick={() => setPage(p => p - 1)}>Previous</Button>
            <Button variant="secondary"
              disabled={page + 1 >= Number(pagination.totalPages)}
              onClick={() => setPage(p => p + 1)}>Next</Button>
          </div>
        </div>}
    </Panel>

    {selected &&
      <div className="detail-workspace">
        <div className="section-title">
          <h2>{
            name === "purchases" ? selected.sourcePoNumber
            : name === "contracts" ? selected.contractNumber
            : name === "users" ? selected.fullName
            : name === "suppliers" ? selected.canonicalName
            : name === "aliases" ? selected.aliasName
            : name === "exceptions" ? "Exception workspace"
            : "Record details"
          }</h2>
          <Button icon="close" variant="ghost"
            onClick={() => setSelected(null)}>Close</Button>
        </div>

        {name === "purchases"
          ? <PurchaseDetails key={String(selected.poId)} record={selected} />
          : name === "exceptions"
          ? <ExceptionDetails
              key={String(selected.exceptionId)}
              record={selected}
              onChanged={changed} />
          : <MasterDetails
              key={`${selected[config.key]}:${revision}`}
              name={name}
              record={selected}
              onChanged={changed} />}
      </div>}

    {dialog && <FormDialog {...dialog} onClose={() => setDialog(null)} />}
  </>;
}

function recordForm(name, record, app) {
  const edit = !!record;
  const initial = {};
  const { lookup, names } = app;

  const text = (name, label, type = "text", required = true, help = "") =>
    ({ name, label, type, required, help });

  const select = (name, label, options) =>
    ({ name, label, type: "select", options });

  const suppliers = lookup.suppliers
    .filter(s => !s.deletedAt && s.active)
    .map(s => ({ value: s.supplierId, label: s.canonicalName }));

  const contracts = lookup.contracts
    .filter(c => c.status === "DRAFT")
    .map(c => ({
      value: c.contractId,
      label: `${c.contractNumber} · ${c.itemCode}`
    }));

  const roles = lookup.roles.map(r => ({
    value: r.roleId,
    label: label(r.roleName)
  }));

  let fields = [];

  if (name === "suppliers") {
    fields = [text("canonicalName", "Supplier name")];
  }

  if (name === "aliases") {
    fields = [
      select("supplierId", "Supplier", suppliers),
      text("aliasName", "Alias name")
    ];
  }

  if (name === "contracts") {
    fields = [
      select("supplierId", "Supplier", suppliers),
      text("contractNumber", "Contract number"),
      text("itemCode", "Item code"),
      text("effectiveFrom", "Effective from", "date"),
      text("effectiveTo", "Effective to", "date"),
      text("currency", "Currency", "text", true, "Three uppercase letters, such as USD.")
    ];
  }

  if (name === "tiers") {
    fields = [
      select("contractId", "Draft contract", contracts),
      text("minQuantity", "Minimum quantity", "integer"),
      { name: "unbounded", label: "No upper quantity limit", type: "checkbox" },
      {
        ...text("maxQuantity", "Maximum quantity", "integer"),
        hide: values => !!values.unbounded
      },
      text("referenceUnitPriceScaled", "Reference unit price", "money"),
      text("allowedMinPriceScaled", "Allowed minimum price", "money"),
      text("allowedMaxPriceScaled", "Allowed maximum price", "money")
    ];
  }

  if (name === "rates") {
    fields = [
      text("fromCurrency", "From currency"),
      text("toCurrency", "To currency"),
      text("rateDate", "Rate date", "date"),
      text("conversionRate", "Conversion rate", "text", true,
        "Positive decimal text, without exponent notation."),
      text("rateSource", "Rate source")
    ];
  }

  if (name === "users") {
    fields = [
      text("fullName", "Full name"),
      text("email", "Email address", "email"),
      ...(edit
        ? [{ name: "active", label: "Account active", type: "checkbox" }]
        : [
          text("password", "Initial password", "password", true,
            "12–64 characters; maximum 72 UTF-8 bytes."),
          { name: "roleIds", label: "Application roles", type: "roles", options: roles }
        ])
    ];
  }

  for (const field of fields) {
    if (field.type === "password") initial[field.name] = "";
    else if (field.type === "money") {
      initial[field.name] = amountInput(record?.[field.name]);
    } else if (field.type === "roles") initial[field.name] = [];
    else initial[field.name] = record?.[field.name] ??
      (field.type === "checkbox" ? false : "");
  }

  if (name === "tiers") {
    initial.unbounded = edit ? record.maxQuantity == null : true;
    if (!edit) initial.minQuantity = "1";
  }

  if (edit && name === "contracts" &&
      !suppliers.some(s => same(s.value, record.supplierId))) {
    suppliers.push({
      value: record.supplierId,
      label: names.supplier(record.supplierId)
    });
  }

  if (edit && ["aliases", "tiers"].includes(name)) {
    const key = name === "aliases" ? "supplierId" : "contractId";
    const field = fields.find(f => f.name === key);
    field.disabled = true;

    if (!field.options.some(o => same(o.value, record[key]))) {
      field.options.push({
        value: record[key],
        label: name === "aliases"
          ? names.supplier(record[key]) : names.contract(record[key])
      });
    }
  }

  function build(values) {
    const body = {};

    for (const field of fields) {
      if (field.name === "unbounded") continue;
      const value = values[field.name];

      if (field.name === "maxQuantity" && values.unbounded) {
        body.maxQuantity = null;
      } else if (field.type === "money") {
        body[field.name] = scaled(value);
      } else if (field.type === "integer") {
        body[field.name] = integer(value);
      } else if (field.type === "roles") {
        if (!value?.length) throw new Error("Select at least one application role.");
        body[field.name] = value.map(integer);
      } else if (field.name.endsWith("Id")) {
        body[field.name] = integer(value);
      } else if (field.type === "checkbox") {
        body[field.name] = !!value;
      } else {
        body[field.name] = field.type === "password"
          ? value : String(value ?? "").trim();
      }

      if (["currency", "fromCurrency", "toCurrency"].includes(field.name) &&
          !/^[A-Z]{3}$/.test(body[field.name])) {
        throw new Error("Currency must contain three uppercase letters.");
      }
    }

    if (name === "users" && !edit) checkPassword(body.password);

    if (name === "contracts" && body.effectiveTo < body.effectiveFrom) {
      throw new Error("Contract end date must not precede the start date.");
    }

    if (name === "tiers") {
      if (body.maxQuantity != null && body.maxQuantity < body.minQuantity) {
        throw new Error("Maximum quantity must not be below minimum quantity.");
      }

      if (body.allowedMaxPriceScaled < body.allowedMinPriceScaled ||
          body.referenceUnitPriceScaled < body.allowedMinPriceScaled ||
          body.referenceUnitPriceScaled > body.allowedMaxPriceScaled) {
        throw new Error("Reference price must lie inside the allowed price band.");
      }
    }

    if (name === "rates") {
      if (body.fromCurrency === body.toCurrency) {
        throw new Error("From and to currencies must differ.");
      }
      if (!/^\d+(?:\.\d+)?$/.test(body.conversionRate) ||
          !/[1-9]/.test(body.conversionRate)) {
        throw new Error("Enter a positive decimal rate without exponent notation.");
      }
    }

    return body;
  }

  return { fields, initial, build };
}

function MasterDetails({ name, record, onChanged }) {
  const app = useApp();
  const { api, all, names, has, lookup, notify } = app;
  const config = CONFIG[name];

  const [dialog, setDialog] = useState(null);
  const [related, setRelated] = useState([]);
  const [relatedBusy, setRelatedBusy] = useState(false);
  const [relatedError, setRelatedError] = useState("");

  const archived = !!record.deletedAt;
  const parentContract = lookup.contracts.find(c => same(c.contractId, record.contractId));

  const canonicalAlias = name === "aliases" &&
    lookup.suppliers.find(s => same(s.supplierId, record.supplierId))
      ?.canonicalName?.trim().toLowerCase() === record.normalizedAlias;

  const editable = has("ADMIN") && !archived && !canonicalAlias &&
    (name !== "contracts" || record.status === "DRAFT") &&
    (name !== "tiers" || parentContract?.status === "DRAFT");

  useEffect(() => {
    let alive = true;
    if (!["contracts", "users"].includes(name)) return;

    setRelatedBusy(true);
    const promise = name === "contracts"
      ? all("contract-price-tiers", `&contractId=${record.contractId}`)
      : api(`/users/${record.userId}/roles`);

    promise.then(data => { if (alive) setRelated(data); })
      .catch(e => { if (alive) setRelatedError(e.message); })
      .finally(() => { if (alive) setRelatedBusy(false); });

    return () => { alive = false; };
  }, []);

  function edit() {
    const definition = recordForm(name, record, app);

    setDialog({
      title: `Edit ${singular(name)}`,
      fields: definition.fields,
      initial: definition.initial,
      onSubmit: async values => {
        await api(`/${config.route}/${record[config.key]}`, {
          method: "PUT",
          body: definition.build(values)
        });
        setDialog(null);
        notify("Changes saved.");
        await onChanged();
      }
    });
  }

  function action(title, suffix, fields = [], build = () => undefined,
                  method = "POST", initial = {}) {
    setDialog({
      title,
      fields,
      initial,
      description: fields.length ? undefined
        : "Confirm this operation. The backend will check its eligibility.",
      onSubmit: async values => {
        await api(
          `/${config.route}/${record[config.key]}${suffix ? "/" + suffix : ""}`,
          { method, body: build(values) }
        );
        setDialog(null);
        notify("Operation completed.");
        await onChanged(method === "DELETE");
      }
    });
  }

  const reason = [
    { name: "reason", label: "Reason", type: "textarea", maxLength: 2000 }
  ];

  let items = columns(name, names).map(c => [c.title, c.render(record)]);

  if (name === "uploads") {
    items = [
      ["Source file", record.fileName],
      ["Processing status", <Badge value={record.processingStatus} />],
      ["Imported", time(record.uploadedAt)],
      ["Original storage", record.s3ObjectKey
        ? "Cloud object recorded" : "Local original storage"]
    ];
  }

  return <>
    <Panel title="Business details"
      actions={editable && <Button variant="secondary" onClick={edit}>Edit</Button>}>
      <Details items={items} />

      {name === "contracts" && <>
        <h3>Quantity tiers</h3>
        {relatedError ? <Notice danger>{relatedError}</Notice>
          : relatedBusy ? <Skeleton />
          : <Table rows={related} columns={columns("tiers", names)} rowKey="tierId" />}
        <Notice>
          Only unused drafts can be edited. Approval requires quantity coverage
          from one without gaps and an unbounded final tier.
        </Notice>
      </>}

      {name === "users" && <>
        <h3>Assigned permissions</h3>
        {relatedError ? <Notice danger>{relatedError}</Notice>
          : relatedBusy ? <Skeleton />
          : <div className="chips">
              {related.map(r => <Badge key={String(r.roleId)} value={r.roleName} />)}
            </div>}
        <Notice>
          Restored users remain inactive. The backend blocks self-deletion,
          self-deactivation, and removal of the last active admin.
        </Notice>
      </>}

      {name === "aliases" &&
        <Notice>
          Canonical aliases cannot be edited or archived directly.
          Archived aliases remain reserved and cannot be reassigned.
        </Notice>}

      {["tiers", "rates"].includes(name) &&
        <Notice>
          Historical-use checks are enforced by the backend.
          Evaluated terms cannot be changed or deleted.
        </Notice>}

      {has("ADMIN") &&
        <div className="lifecycle-actions">
          {["suppliers", "aliases", "users"].includes(name) &&
            (archived
              ? <Button variant="secondary"
                  onClick={() => action("Restore record", "restore")}>Restore</Button>
              : !canonicalAlias &&
                <Button variant="danger"
                  onClick={() => action("Archive record", "soft-delete", reason,
                    values => ({ reason: values.reason.trim() }))}>
                  Archive
                </Button>)}

          {name === "contracts" && record.status === "DRAFT" &&
            <Button onClick={() => action("Approve draft contract", "approve")}>
              Approve draft
            </Button>}

          {name === "contracts" && record.status !== "CANCELLED" &&
            <Button variant="danger"
              onClick={() => action("Cancel contract", "cancel", reason,
                values => ({ reason: values.reason.trim() }))}>
              Cancel contract
            </Button>}

          {["tiers", "rates"].includes(name) &&
            (name !== "tiers" || parentContract?.status === "DRAFT") &&
            <Button variant="danger"
              onClick={() => action("Delete unused record", "", [], undefined, "DELETE")}>
              Delete if unused
            </Button>}

          {name === "users" && !archived && <>
            <Button variant="secondary" onClick={() => action(
              "Assign application roles",
              "roles",
              [{
                name: "roleIds",
                label: "Application roles",
                type: "roles",
                options: lookup.roles.map(r => ({
                  value: r.roleId,
                  label: label(r.roleName)
                }))
              }],
              values => {
                if (!values.roleIds?.length) throw new Error("Select at least one role.");
                return { roleIds: values.roleIds.map(integer) };
              },
              "PUT",
              { roleIds: related.map(r => r.roleId) }
            )}>Assign roles</Button>

            <Button variant="secondary" onClick={() => action(
              "Reset password",
              "reset-password",
              [{
                name: "newPassword",
                label: "New password",
                type: "password",
                help: "12–64 characters; maximum 72 UTF-8 bytes."
              }],
              values => {
                checkPassword(values.newPassword);
                return { newPassword: values.newPassword };
              }
            )}>Reset password</Button>
          </>}
        </div>}
    </Panel>

    {dialog && <FormDialog {...dialog} onClose={() => setDialog(null)} />}
  </>;
}


const CSV_HEADER = [
  "source_po_number", "line_number", "order_date", "department",
  "buyer_email", "supplier_name", "item_code", "quantity",
  "unit_price", "currency"
].join(",");

function UploadBox({ onComplete }) {
  const { api, notify } = useApp();
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [drag, setDrag] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);
  const lock = useRef(false);
  const input = useRef(null);

  function choose(next) {
    setError("");
    setResult(null);
    if (!next) return;
    if (next.size > 2 * 1024 * 1024) {
      setError("Choose a CSV no larger than 2 MiB.");
      setFile(null);
      return;
    }
    setFile(next);
  }

  function template() {
    const blob = new Blob([CSV_HEADER + "\n"], {
      type: "text/csv;charset=utf-8"
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "purchase_orders_template.csv";
    anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  async function upload(event) {
    event.preventDefault();
    if (lock.current) return;
    if (!file) { setError("Choose a CSV file."); return; }

    lock.current = true;
    setBusy(true);
    setError("");
    setResult(null);

    try {
      const form = new FormData();
      form.append("file", file);
      const response = await api("/uploads", {
        method: "POST",
        body: form
      });
      setResult(response);
      setFile(null);
      if (input.current) input.current.value = "";
      notify("Import completed. Review any compliance findings.");
      onComplete();
    } catch (e) {
      setError(e.message);
    } finally {
      lock.current = false;
      setBusy(false);
    }
  }

  return <Panel title="Import purchase orders"
    subtitle="UTF-8 CSV · Up to 2 MiB · Up to 5,000 records by default"
    actions={<Button variant="secondary" onClick={template}>
      Download CSV template
    </Button>}>

    <form onSubmit={upload}>
      <div className={`dropzone ${drag ? "dragging" : ""}`}
        onDragOver={e => { e.preventDefault(); if (!busy) setDrag(true); }}
        onDragLeave={() => setDrag(false)}
        onDrop={e => {
          e.preventDefault();
          setDrag(false);
          if (!busy) choose(e.dataTransfer.files?.[0]);
        }}>
        <span className="upload-icon"><Icon name="upload" size={28} /></span>
        <h3>{file ? file.name : "Drop a purchase CSV here"}</h3>
        <p>{file
          ? `${(file.size / 1024).toFixed(1)} KiB · Ready to import`
          : "Or select a file from your computer."}</p>

        <input ref={input} className="sr-only" type="file"
          accept=".csv,text/csv" disabled={busy}
          onChange={e => choose(e.target.files?.[0])} />

        <Button type="button" variant="secondary" disabled={busy}
          onClick={() => input.current?.click()}>
          {file ? "Choose another file" : "Choose CSV"}
        </Button>
      </div>

      <Notice>
        Imports are atomic: invalid data rejects the complete file.
        Compliance exceptions are business findings, not CSV processing failures.
      </Notice>

      <details className="technical">
        <summary>Required CSV columns</summary>
        <p className="code-line">{CSV_HEADER}</p>
        <p>Column order may differ, but header names must match exactly.</p>
      </details>

      {error && <div className="form-error" role="alert">{error}</div>}

      <div className="upload-footer">
        <span className="muted">
          {busy ? "Submitting and processing the file…" : "No purchases are changed until you submit."}
        </span>
        <Button type="submit" icon="upload" disabled={busy || !file}>
          {busy ? "Importing…" : "Import purchases"}
        </Button>
      </div>
    </form>

    {result &&
      <div className="import-results">
        {[
          ["Purchase orders", result.purchaseOrdersCreated],
          ["Purchase lines", result.purchaseLinesCreated],
          ["Evaluations", result.evaluationsCreated],
          ["Exceptions", result.exceptionsCreated]
        ].map(([title, count]) =>
          <div key={title}><strong>{String(count)}</strong><span>{title}</span></div>)}
      </div>}
  </Panel>;
}

function PurchaseDetails({ record }) {
  const { api } = useApp();
  const [detail, setDetail] = useState(null);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");
  const [evidence, setEvidence] = useState(null);
  const [lineBusy, setLineBusy] = useState(false);
  const [lineError, setLineError] = useState("");

  useEffect(() => {
    let alive = true;
    api(`/purchase-orders/${record.poId}`)
      .then(data => { if (alive) setDetail(data); })
      .catch(e => { if (alive) setError(e.message); })
      .finally(() => { if (alive) setBusy(false); });
    return () => { alive = false; };
  }, []);

  async function inspect(line) {
    setLineBusy(true);
    setLineError("");
    setEvidence(null);

    try {
      const evaluations = await api(
        `/purchase-orders/lines/${line.poLineId}/evaluations`
      );
      const evaluation = evaluations[evaluations.length - 1];
      if (!evaluation) throw new Error("No evaluation is available for this line.");
      setEvidence({ order: detail.order, line, evaluation });
    } catch (e) {
      setLineError(e.message);
    } finally {
      setLineBusy(false);
    }
  }

  if (busy) return <Skeleton />;
  if (error) return <Notice danger>{error}</Notice>;
  if (!detail) return null;

  const order = detail.order;

  return <>
    <Panel title="Purchase details">
      <Details items={[
        ["Purchase order", order.sourcePoNumber],
        ["Supplier as uploaded", order.originalSupplierName],
        ["Department", order.department],
        ["Order date", order.orderDate],
        ["Currency", order.currency]
      ]} />

      <h3>Purchase lines</h3>
      <Table rows={detail.lines} rowKey="poLineId"
        columns={[
          { title: "Line", render: r => String(r.lineNumber) },
          { title: "Item", render: r => <strong>{r.itemCode}</strong> },
          { title: "Quantity", render: r => String(r.quantity) },
          { title: "Unit price", render: r => money(r.actualUnitPriceScaled, order.currency) },
          { title: "Line spend", render: r => money(
              BigInt(String(r.quantity)) * BigInt(String(r.actualUnitPriceScaled)),
              order.currency
            ) }
        ]}
        onOpen={line => { if (!lineBusy) inspect(line); }}
      />
    </Panel>

    {lineBusy && <Skeleton />}
    {lineError && <Notice danger>{lineError}</Notice>}
    {evidence && <Evidence data={evidence} />}
  </>;
}

function Evidence({ data }) {
  const { names } = useApp();
  const { order, line, evaluation } = data;
  let snapshot = {};

  try {
    snapshot = typeof evaluation.matchingSnapshot === "string"
      ? parseExact(evaluation.matchingSnapshot)
      : evaluation.matchingSnapshot || {};
  } catch {
    snapshot = {};
  }

  const currency = evaluation.comparisonCurrency || "";
  const supplier = order.supplierId == null
    ? "Unresolved supplier" : names.supplier(order.supplierId);

  return <Panel title="Matching evidence"
    subtitle="Terms and monetary rules recorded when the purchase was evaluated."
    actions={<Badge value={evaluation.resultStatus} />}>

    <div className="evidence-explanation">
      <Icon name="shield" />
      <p>{evaluation.explanation}</p>
    </div>

    <Details items={[
      ["Purchase order", order.sourcePoNumber],
      ["Item", line.itemCode],
      ["Original supplier", order.originalSupplierName],
      ["Normalized supplier", supplier],
      ["Order date", order.orderDate],
      ["Quantity", String(line.quantity)],
      ["Contract", snapshot.contractNumber ||
        (evaluation.contractId ? names.contract(evaluation.contractId) : "No matching contract")],
      ["Contract applicability", snapshot.contractEffectiveFrom
        ? `${snapshot.contractEffectiveFrom} — ${snapshot.contractEffectiveTo}` : "Not available"],
      ["Quantity tier", snapshot.tierMinQuantity != null
        ? `${snapshot.tierMinQuantity} — ${snapshot.tierMaxQuantity ?? "Unbounded"}` : "Not available"],
      ["Actual unit price", money(line.actualUnitPriceScaled, order.currency)],
      ["Converted unit price", money(evaluation.convertedUnitPriceScaled, currency)],
      ["Reference unit price", money(snapshot.referenceUnitPriceScaled, currency)],
      ["Allowed price band", snapshot.allowedMinPriceScaled != null
        ? `${money(snapshot.allowedMinPriceScaled, currency)} — ${money(snapshot.allowedMaxPriceScaled, currency)}`
        : "Not available"],
      ["Potential leakage", money(evaluation.potentialPriceLeakageScaled, currency)],
      ["Conversion rate", snapshot.conversionRate || "Not available"],
      ["FX evidence date", snapshot.rateDate ||
        (snapshot.rateSource === "SAME_CURRENCY" ? "Same currency" : "Not available")],
      ["FX source", snapshot.rateSource || "Not available"],
      ["Rule version", evaluation.ruleVersion]
    ]} />

    <Notice>
      Unknown leakage is not zero. A price inside the allowed band may be compliant
      even when it differs from the reference price.
    </Notice>

    <details className="technical">
      <summary>Recorded matching snapshot — internal identifiers hidden</summary>
      <pre>{JSON.stringify(sanitizeEvidence(snapshot), null, 2)}</pre>
    </details>
  </Panel>;
}

function ExceptionDetails({ record, onChanged }) {
  const app = useApp();
  const { api, user, has, names, notify, findEvidence } = app;

  const [finding, setFinding] = useState(record);
  const [events, setEvents] = useState([]);
  const [recoveries, setRecoveries] = useState([]);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");
  const [tab, setTab] = useState("overview");
  const [evidence, setEvidence] = useState(null);
  const [evidenceBusy, setEvidenceBusy] = useState(false);
  const [evidenceError, setEvidenceError] = useState("");
  const [dialog, setDialog] = useState(null);
  const evidenceFlight = useRef(null);

  async function load() {
    setBusy(true);
    setError("");

    try {
      const [current, history, recoveryRecords] = await Promise.all([
        api(`/exceptions/${record.exceptionId}`),
        api(`/exceptions/${record.exceptionId}/review-events`),
        api(`/exceptions/${record.exceptionId}/recoveries`)
      ]);
      setFinding(current);
      setEvents(history);
      setRecoveries(recoveryRecords);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function ensureEvidence() {
    if (evidence) return evidence;
    if (evidenceFlight.current) return evidenceFlight.current;

    setEvidenceBusy(true);
    setEvidenceError("");

    const task = findEvidence(finding.evaluationId);
    evidenceFlight.current = task;

    try {
      const data = await task;
      setEvidence(data);
      return data;
    } catch (e) {
      setEvidenceError(e.message);
      throw e;
    } finally {
      setEvidenceBusy(false);
      evidenceFlight.current = null;
    }
  }

  function openTab(next) {
    setTab(next);
    if (["evidence", "recoveries"].includes(next) && !evidence) {
      ensureEvidence().catch(() => {});
    }
  }

  async function saved() {
    await load();
    await onChanged();
  }

  const ownOrder = evidence
    ? same(evidence.order.buyerUserId, user.userId) : null;

  const explainedByMe = events.some(event =>
    same(event.actorUserId, user.userId) &&
    event.action === "SUBMIT_EXPLANATION"
  );

  const terminal = TERMINAL.includes(finding.reviewStatus);

  const explanationStatus = ["OPEN", "EVIDENCE_REQUESTED"]
    .includes(finding.reviewStatus);

  const explainRole = has("ADMIN", "BUYER");
  const explanationAllowed = explainRole && explanationStatus &&
    (has("ADMIN") || ownOrder !== false);

  const reviewer = has("ADMIN", "REVIEWER");
  const reviewAllowed = reviewer && !terminal && !explainedByMe && ownOrder !== true;

  function explanation() {
    setDialog({
      title: "Submit purchase explanation",
      fields: [{
        name: "comment",
        label: "Business explanation",
        type: "textarea",
        maxLength: 4000
      }],
      initial: { comment: "" },
      description: "Describe the circumstances and refer to supporting business evidence.",
      onSubmit: async values => {
        if (!has("ADMIN")) {
          const match = await ensureEvidence();
          if (!same(match.order.buyerUserId, user.userId)) {
            throw new Error("Only the owning buyer or an admin can explain this purchase.");
          }
        }

        await api(`/exceptions/${finding.exceptionId}/explanations`, {
          method: "POST",
          body: { comment: values.comment.trim() }
        });
        setDialog(null);
        notify("Explanation submitted.");
        await saved();
      }
    });
  }

  function decision(action) {
    setDialog({
      title: label(action),
      fields: [{
        name: "comment",
        label: "Decision reason",
        type: "textarea",
        maxLength: 4000
      }],
      initial: { comment: "" },
      description: "The backend enforces ownership and separation of duties.",
      onSubmit: async values => {
        const match = await ensureEvidence();
        if (same(match.order.buyerUserId, user.userId)) {
          throw new Error("You cannot review your own purchase.");
        }
        if (explainedByMe) {
          throw new Error("An explanation submitter cannot review this exception.");
        }

        await api(`/exceptions/${finding.exceptionId}/decisions`, {
          method: "POST",
          body: { action, comment: values.comment.trim() }
        });
        setDialog(null);
        notify("Review decision recorded.");
        await saved();
      }
    });
  }

  async function recoveryForm(type) {
    try {
      const match = await ensureEvidence();
      const currency = match.evaluation.comparisonCurrency;
      const potential = match.evaluation.potentialPriceLeakageScaled;

      if (potential == null || BigInt(String(potential)) <= 0n) {
        throw new Error("This evaluation has no positive quantified leakage.");
      }

      setDialog({
        title: type === "ACTUAL_RECOVERY" ? "Record actual recovery" : "Record recoverable estimate",
        fields: [
          { name: "amount", label: "Amount", type: "money" },
          {
            name: "currency",
            label: "Currency",
            type: "select",
            options: [{ value: currency, label: currency }],
            disabled: true
          },
          {
            name: "evidenceReference",
            label: "Evidence reference",
            type: "text",
            maxLength: 1000
          }
        ],
        initial: { amount: "", currency, evidenceReference: "" },
        description: "Do not retry an uncertain recovery submission without inspecting the records first.",
        onSubmit: async values => {
          const amount = scaled(values.amount);
          if (amount <= 0n) throw new Error("Recovery amount must be positive.");

          const existing = recoveries
            .filter(r => r.recordType === type)
            .reduce((sum, r) => sum + BigInt(String(r.amountScaled)), 0n);

          if (existing + amount > BigInt(String(potential))) {
            throw new Error("The cumulative amount would exceed potential leakage.");
          }

          await api(`/exceptions/${finding.exceptionId}/recoveries`, {
            method: "POST",
            body: {
              recordType: type,
              amountScaled: amount,
              currency,
              evidenceReference: values.evidenceReference.trim()
            }
          });

          setDialog(null);
          notify("Recovery evidence recorded.");
          await saved();
        }
      });
    } catch (e) {
      notify(e.message, true);
    }
  }

  const split = finding.exceptionType === "POSSIBLE_SPLIT_PURCHASE";
  let splitEvidence = null;
  if (split) {
    try { splitEvidence = parseExact(finding.reason); } catch {}
  }

  const tabs = [
    ["overview", "Overview"],
    ["evidence", "Matching evidence"],
    ["timeline", "Review timeline"],
    ["recoveries", "Recoveries"]
  ];

  return <>
    <div className="tabs" role="tablist" aria-label="Exception workspace">
      {tabs.map(([key, title]) =>
        <button key={key} role="tab" aria-selected={tab === key}
          className={tab === key ? "active" : ""}
          onClick={() => openTab(key)}>{title}</button>)}
    </div>

    {error && <Notice danger>{error}</Notice>}
    {busy ? <Skeleton /> : <>
      {tab === "overview" &&
        <Panel title="Finding overview" actions={<Badge value={finding.reviewStatus} />}>
          <Details items={[
            ["Finding", <Badge value={finding.exceptionType} />],
            ["Review status", <Badge value={finding.reviewStatus} />],
            ["Summary", split
              ? "Separate purchase orders may qualify for a lower reference-price tier when combined."
              : finding.reason]
          ]} />

          {splitEvidence && <Details items={[
            ["Combined quantity", String(splitEvidence.combinedQuantity ?? "Not available")],
            ["Combined reference price", evidence
              ? money(splitEvidence.combinedReferenceUnitPriceScaled,
                  evidence.evaluation.comparisonCurrency)
              : "Load matching evidence to view currency-qualified pricing"],
            ["Interpretation", "Possible consolidation opportunity—not proof of intent"]
          ]} />}

          {terminal
            ? <Notice>This exception has a terminal decision and cannot be reopened through these APIs.</Notice>
            : <>
              <h3>Workflow actions</h3>
              <div className="lifecycle-actions">
                {explanationAllowed &&
                  <Button onClick={explanation}>Submit explanation</Button>}

                {reviewAllowed &&
                  ["OPEN", "EXPLANATION_SUBMITTED"].includes(finding.reviewStatus) &&
                  <Button variant="secondary" onClick={() => decision("REQUEST_EVIDENCE")}>
                    Request evidence
                  </Button>}

                {reviewAllowed && finding.reviewStatus === "EXPLANATION_SUBMITTED" && <>
                  <Button onClick={() => decision("APPROVE_EXCEPTION")}>Approve exception</Button>
                  <Button variant="danger" onClick={() => decision("CONFIRM_VIOLATION")}>
                    Confirm violation
                  </Button>
                </>}
              </div>

              {!explainRole && !reviewer &&
                <p className="muted">Your role allows reading this finding, not explaining or deciding it.</p>}

              {explainRole && !explanationStatus &&
                <p className="muted">An explanation can be submitted only when the finding is open or evidence is requested.</p>}

              {ownOrder === false && !has("ADMIN") && has("BUYER") &&
                <p className="muted">Only the owning buyer can submit this explanation.</p>}

              {reviewer && finding.reviewStatus !== "EXPLANATION_SUBMITTED" &&
                <p className="muted">Final decisions require a submitted explanation.</p>}

              {reviewer && (ownOrder === true || explainedByMe) &&
                <Notice>You cannot decide on your own purchase or an exception you explained.</Notice>}

              {!evidence &&
                <p className="muted">
                  Ownership is checked when an action is submitted. Load matching evidence to inspect the purchase.
                </p>}
            </>}

          {split && <Notice>
            The current detector groups by buyer and department.
            It does not detect cross-department consolidation.
          </Notice>}
        </Panel>}

      {tab === "evidence" && <>
        <p className="muted">
          Evidence is resolved through the existing purchase APIs.
          Loading can take longer for larger demo datasets.
        </p>
        {evidenceBusy ? <Skeleton />
          : evidence ? <Evidence data={evidence} />
          : <Empty title="Matching evidence is not loaded" text="Load the accessible purchase evaluation." />}

        {evidenceError && <Notice danger>{evidenceError}</Notice>}
        {!evidenceBusy && !evidence &&
          <Button variant="secondary" onClick={() => ensureEvidence().catch(() => {})}>
            Load matching evidence
          </Button>}
      </>}

      {tab === "timeline" &&
        <Panel title="Review timeline"
          subtitle="Application-recorded events; not tamper-proof audit infrastructure.">
          {events.length ? <ol className="timeline">
            {[...events]
              .sort((a, b) => {
                const left = BigInt(String(a.reviewEventId));
                const right = BigInt(String(b.reviewEventId));
                return left < right ? -1 : left > right ? 1 : 0;
              })
              .map(event => <li key={String(event.reviewEventId)}>
                <span className="timeline-dot"><Icon name="check" size={14} /></span>
                <div>
                  <div className="timeline-top">
                    <strong>{label(event.action)}</strong>
                    <time>{time(event.createdAt)}</time>
                  </div>
                  <p className="muted">{names.person(event.actorUserId)} · {label(event.actorRoleAtAction)}</p>
                  <p>{event.action === "EXCEPTION_CREATED" && split
                    ? "Possible split-purchase review finding created."
                    : String(event.action).startsWith("RECORD_")
                    ? "Recovery record saved with an evidence reference."
                    : event.comment}</p>
                </div>
              </li>)}
          </ol> : <Empty title="No review events" text="Recorded actions will appear here." />}
        </Panel>}

      {tab === "recoveries" &&
        <Panel title="Recovery evidence"
          subtitle="Recoverable estimates and actual recoveries are separate records.">
          {evidenceBusy && <Skeleton />}
          {evidenceError && <Notice danger>{evidenceError}</Notice>}

          <div className="financial-grid">
            {[
              ["RECOVERABLE_ESTIMATE", "Recoverable estimate"],
              ["ACTUAL_RECOVERY", "Actual recovery"]
            ].map(([type, title]) => {
              const records = recoveries.filter(r => r.recordType === type);
              const currency = evidence?.evaluation.comparisonCurrency || records[0]?.currency || "";
              const total = records.reduce((sum, r) => sum + BigInt(String(r.amountScaled)), 0n);

              return <div className="financial" key={type}>
                <p>{title}</p><strong>{money(total, currency)}</strong>
                <small>{records.length} recorded entries</small>
              </div>;
            })}
          </div>

          {evidence &&
            <p className="muted">Each record type has its own cumulative cap:
              {" "}{money(evidence.evaluation.potentialPriceLeakageScaled,
                evidence.evaluation.comparisonCurrency)}.
            </p>}

          <Table rows={recoveries} rowKey="recoveryId" columns={[
            { title: "Record type", render: r => label(r.recordType) },
            { title: "Amount", render: r => money(r.amountScaled, r.currency) },
            { title: "Evidence reference", render: r => r.evidenceReference },
            { title: "Recorded", render: r => time(r.recordedAt) }
          ]} />

          {has(...OPS) &&
            finding.reviewStatus === "CONFIRMED_VIOLATION" &&
            finding.exceptionType === "PRICE_ABOVE_BAND"
            ? <div className="lifecycle-actions">
                <Button variant="secondary" disabled={evidenceBusy}
                  onClick={() => recoveryForm("RECOVERABLE_ESTIMATE")}>
                  Record estimate
                </Button>
                <Button disabled={evidenceBusy}
                  onClick={() => recoveryForm("ACTUAL_RECOVERY")}>
                  Record actual recovery
                </Button>
              </div>
            : <p className="muted">
                Recording requires an operational role and a confirmed above-band pricing violation.
              </p>}

          <Notice>
            Do not add estimates and actual recoveries together.
            There are no edit, delete, or reversal controls.
            Inspect records before retrying an uncertain submission.
          </Notice>
        </Panel>}
    </>}

    {dialog && <FormDialog {...dialog} onClose={() => setDialog(null)} />}
  </>;
}

function Account({ onLogout }) {
  const { user, api, notify } = useApp();
  const [dialog, setDialog] = useState(false);

  return <>
    <Heading title="My account" text="Your application identity and current permissions." />

    <Panel title="Account details">
      <Details items={[
        ["Full name", user.fullName],
        ["Email address", user.email]
      ]} />
      <h3>Application permissions</h3>
      <div className="chips">{user.roles.map(role =>
        <Badge key={role} value={role} />)}</div>

      <div className="lifecycle-actions">
        <Button onClick={() => setDialog(true)}>Change password</Button>
        <Button variant="secondary" onClick={onLogout}>Sign out</Button>
      </div>

      <Notice>
        Signing out discards the client token. It is not a server-side token revocation.
        Refreshing this application also clears the session.
      </Notice>
    </Panel>

    {dialog &&
      <FormDialog title="Change password"
        fields={[
          { name: "currentPassword", label: "Current password", type: "password" },
          {
            name: "newPassword",
            label: "New password",
            type: "password",
            help: "12–64 characters; maximum 72 UTF-8 bytes."
          },
          { name: "confirmPassword", label: "Confirm new password", type: "password" }
        ]}
        initial={{ currentPassword: "", newPassword: "", confirmPassword: "" }}
        onClose={() => setDialog(false)}
        onSubmit={async values => {
          checkPassword(values.newPassword);

          if (values.newPassword !== values.confirmPassword) {
            throw new Error("New passwords do not match.");
          }

          await api("/auth/change-password", {
            method: "POST",
            body: {
              currentPassword: values.currentPassword,
              newPassword: values.newPassword
            }
          });

          setDialog(false);
          onLogout();
          notify("Password changed. Sign in again with the new password.");
        }}
      />}
  </>;
}

createRoot(document.getElementById("root")).render(<App />);
