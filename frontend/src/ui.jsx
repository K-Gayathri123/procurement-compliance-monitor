
import React, { useEffect, useRef, useState } from "react";
import { label } from "./utils";

export function Icon({ name = "grid", size = 19 }) {
  const paths = {
    grid: "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z",
    upload: "M12 16V3 M7 8l5-5 5 5 M3 16v5h18v-5",
    bag: "M4 7h16v14H4z M8 7V4h8v3",
    flag: "M5 21V3 M5 3h14l-3 5 3 5H5",
    file: "M6 3h8l4 4v14H6z M14 3v5h5 M9 12h6 M9 16h6",
    users: "M16 21v-3a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v3 M17 3a4 4 0 0 1 0 8 M22 21v-3a4 4 0 0 0-3-4 M9 3a4 4 0 1 0 0 8a4 4 0 0 0 0-8",
    building: "M4 21V3h16v18 M2 21h20 M8 7h2 M14 7h2 M8 11h2 M14 11h2 M10 21v-5h4v5",
    exchange: "M3 7h18 M17 3l4 4-4 4 M21 17H3 M7 13l-4 4 4 4",
    shield: "M12 3l8 3v6c0 5-8 9-8 9s-8-4-8-9V6z M8 12l3 3 5-6",
    logout: "M9 3H3v18h6 M8 12h13 M17 8l4 4-4 4",
    search: "M10 3a7 7 0 1 0 0 14a7 7 0 0 0 0-14 M15 15l6 6",
    refresh: "M20 7a8 8 0 1 0 1 8 M20 3v5h-5",
    plus: "M12 4v16 M4 12h16",
    close: "M6 6l12 12 M18 6L6 18",
    arrow: "M9 5l7 7-7 7",
    menu: "M3 6h18 M3 12h18 M3 18h18",
    check: "M5 12l4 4L19 6",
    alert: "M12 3L2 21h20z M12 9v5 M12 17h.01",
    clock: "M12 3a9 9 0 1 0 0 18a9 9 0 0 0 0-18 M12 7v5l3 2"
  };
  return <svg width={size} height={size} viewBox="0 0 24 24"
    fill="none" stroke="currentColor" strokeWidth="1.7"
    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d={paths[name] || paths.grid} />
  </svg>;
}

export function Button({ children, icon, variant = "", ...props }) {
  return <button className={`button ${variant}`} {...props}>
    {icon && <Icon name={icon} size={17} />}{children}
  </button>;
}

export function Badge({ value }) {
  const green = ["COMPLIANT", "APPROVED", "APPROVED_EXCEPTION", "COMPLETED", "ACTIVE"];
  const red = ["CONFIRMED_VIOLATION", "PRICE_ABOVE_BAND", "FAILED", "CANCELLED"];
  const amber = ["OPEN", "NEEDS_REVIEW", "EVIDENCE_REQUESTED", "POSSIBLE_SPLIT_PURCHASE"];
  const tone = green.includes(value) ? "green"
    : red.includes(value) ? "red"
    : amber.includes(value) ? "amber" : "slate";
  return <span className={`badge ${tone}`}>{label(value)}</span>;
}

export function Panel({ title, subtitle, actions, children }) {
  return <section className="panel">
    {(title || actions) && <div className="panel-head">
      <div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>
      <div className="actions">{actions}</div>
    </div>}
    {children}
  </section>;
}

export function Notice({ children, danger = false }) {
  return <div className={`notice ${danger ? "danger" : ""}`}>
    <Icon name="alert" /><div>{children}</div>
  </div>;
}

export function Empty({ title = "No records found", text = "Try changing your filters." }) {
  return <div className="empty">
    <span><Icon name="file" size={28} /></span>
    <h3>{title}</h3><p>{text}</p>
  </div>;
}

export function Skeleton() {
  return <div className="skeletons" role="status" aria-label="Loading">
    {[1, 2, 3, 4].map(i => <div className="skeleton" key={i} />)}
  </div>;
}

export function Details({ items }) {
  return <dl className="details">
    {items.map(([title, value]) => <div key={title}>
      <dt>{title}</dt><dd>{value ?? "Not available"}</dd>
    </div>)}
  </dl>;
}

export function Table({ rows, columns, rowKey, onOpen }) {
  if (!rows.length) return <Empty />;
  return <div className="table-wrap"><table>
    <thead><tr>
      {columns.map(c => <th key={c.title}>{c.title}</th>)}
      {onOpen && <th><span className="sr-only">Details</span></th>}
    </tr></thead>
    <tbody>{rows.map((row, i) => <tr key={rowKey ? String(row[rowKey]) : i}>
      {columns.map(c => <td key={c.title}>{c.render(row)}</td>)}
      {onOpen && <td className="row-action">
        <Button variant="ghost" onClick={() => onOpen(row)}>Details <Icon name="arrow" size={14} /></Button>
      </td>}
    </tr>)}</tbody>
  </table></div>;
}

export function Field({ label: title, help, ...props }) {
  return <label className="field">
    <span>{title}</span><input {...props} />{help && <small>{help}</small>}
  </label>;
}

export function SelectField({ label: title, options = [], help, ...props }) {
  return <label className="field">
    <span>{title}</span>
    <select {...props}>
      <option value="">Select…</option>
      {options.map(o => <option key={String(o.value)} value={o.value}>{o.label}</option>)}
    </select>
    {help && <small>{help}</small>}
  </label>;
}

export function FormDialog({ title, description, fields = [], initial = {}, onSubmit, onClose }) {
  const dialog = useRef(null);
  const lock = useRef(false);
  const [values, setValues] = useState(initial);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const element = dialog.current;
    element.showModal();
    return () => { if (element.open) element.close(); };
  }, []);

  async function submit(e) {
    e.preventDefault();
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError("");
    try { await onSubmit(values); }
    catch (e) { setError(e.message); }
    finally { lock.current = false; setBusy(false); }
  }

  return <dialog ref={dialog} className="modal" aria-label={title}
    onCancel={e => { e.preventDefault(); if (!busy) onClose(); }}>
    <div className="modal-head"><h2>{title}</h2>
      <Button variant="ghost" aria-label="Close dialog" disabled={busy} onClick={onClose}>
        <Icon name="close" />
      </Button>
    </div>
    <form onSubmit={submit}>
      {description && <p className="muted">{description}</p>}
      <div className="form-grid">
        {fields.filter(f => !f.hide || !f.hide(values)).map(f => {
          const update = value => setValues({ ...values, [f.name]: value });

          if (f.type === "checkbox") return <label className="check full" key={f.name}>
            <input type="checkbox" checked={!!values[f.name]}
              onChange={e => update(e.target.checked)} />{f.label}
          </label>;

          if (f.type === "roles") return <fieldset className="full role-field" key={f.name}>
            <legend>{f.label}</legend>
            {f.options.map(o => <label className="check" key={String(o.value)}>
              <input type="checkbox"
                checked={(values[f.name] || []).some(v => String(v) === String(o.value))}
                onChange={e => update(e.target.checked
                  ? [...(values[f.name] || []), o.value]
                  : (values[f.name] || []).filter(v => String(v) !== String(o.value)))} />
              {o.label}
            </label>)}
            {!f.options.length && <p>No roles loaded. Retry the lookup data.</p>}
          </fieldset>;

          const props = {
            value: values[f.name] ?? "",
            required: f.required !== false,
            disabled: f.disabled,
            onChange: e => update(e.target.value)
          };

          if (f.type === "textarea") return <label className="field full" key={f.name}>
            <span>{f.label}</span>
            <textarea rows={5} maxLength={f.maxLength || 4000} {...props} />
          </label>;

          if (f.type === "select") return <SelectField key={f.name}
            label={f.label} options={f.options} help={f.help} {...props} />;

          return <Field key={f.name} label={f.label} help={f.help}
            type={["money", "integer"].includes(f.type) ? "text" : f.type || "text"}
            inputMode={f.type === "money" ? "decimal" : f.type === "integer" ? "numeric" : undefined}
            autoComplete={f.type === "password" ? "new-password" : undefined}
            maxLength={f.maxLength} {...props} />;
        })}
      </div>
      {error && <div className="form-error" role="alert">{error}</div>}
      <div className="modal-footer">
        <Button type="button" variant="secondary" disabled={busy} onClick={onClose}>Cancel</Button>
        <Button type="submit" disabled={busy}>{busy ? "Saving…" : "Confirm"}</Button>
      </div>
    </form>
  </dialog>;
}
