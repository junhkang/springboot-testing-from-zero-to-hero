#!/usr/bin/env node
// Deterministic gate for Security Shield auto-merges: a dependency PR may merge without a
// human only when it is a same-major version bump from the public registries. Anything else
// (new deps, major bumps, code, scripts, CI logic, install scripts) needs the owner.
// Usage: node .github/scripts/security-patch-gate.mjs [base-ref]   (default origin/main)
// CI runs the base branch's copy of this file, so a PR cannot loosen its own gate.
import { execFileSync } from "node:child_process";

const git = (...a) => execFileSync("git", a, { encoding: "utf8", maxBuffer: 1 << 28 });
const show = (ref, path) => { try { return git("show", `${ref}:${path}`); } catch { return null; } };

const PKG_JSON = /(^|\/)package\.json$/;
const PKG_LOCK = /(^|\/)package-lock\.json$/;
const TEXT_MANIFEST = /(^|\/)(pom\.xml|build\.gradle(\.kts)?|libs\.versions\.toml|gradle-wrapper\.properties|Dockerfile[^/]*)$/;
const WORKFLOW = /^\.github\/workflows\/[^/]+\.ya?ml$/;
const NEVER = [/^\.github\/scripts\//, /^\.github\/workflows\/security-patch-gate\.ya?ml$/, /^\.github\/dependabot\.ya?ml$/, /(^|\/)themes\//, /(^|\/)(android|ios)\//];
const DEP_FIELDS = ["dependencies", "devDependencies", "optionalDependencies", "peerDependencies", "overrides"];
const REGISTRY = /^https:\/\/registry\.npmjs\.org\//;
export const LIMITS = { files: 12 };

const VER = /\d+(?:\.\d+)+(?:[-+.][0-9A-Za-z][0-9A-Za-z.]*)?/g;
const parts = (v) => (String(v).match(/\d+(?:\.\d+)*/) || [""])[0].split(".").map(Number);

// Same "compatibility line": same major, or same major.minor while major is 0. Never a downgrade.
export function compatibleBump(from, to) {
  const a = parts(from), b = parts(to);
  if (!a.length || !b.length || Number.isNaN(a[0]) || Number.isNaN(b[0])) return false;
  if (a[0] !== b[0]) return false;
  if (a[0] === 0 && (a[1] ?? 0) !== (b[1] ?? 0)) return false;
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    const x = a[i] ?? 0, y = b[i] ?? 0;
    if (x !== y) return y > x;
  }
  return true;
}

const strip = (o, keys) => Object.fromEntries(Object.entries(o || {}).filter(([k]) => !keys.includes(k)));
const stable = (v) => JSON.stringify(v, (_, x) => (x && typeof x === "object" && !Array.isArray(x) ? Object.fromEntries(Object.entries(x).sort()) : x));

export function checkPackageJson(path, before, after) {
  const r = [];
  let a, b;
  try { a = JSON.parse(before ?? "{}"); b = JSON.parse(after ?? "{}"); } catch { return [`${path}: invalid JSON`]; }
  if (before == null) r.push(`${path}: new file`);
  if (stable(strip(a, DEP_FIELDS)) !== stable(strip(b, DEP_FIELDS))) r.push(`${path}: non-dependency fields changed`);
  for (const f of DEP_FIELDS) {
    const x = a[f] || {}, y = b[f] || {};
    for (const name of new Set([...Object.keys(x), ...Object.keys(y)])) {
      if (stable(x[name]) === stable(y[name])) continue;
      if (!(name in y)) { r.push(`${path}: ${f}.${name} removed`); continue; }
      if (!(name in x)) { if (f !== "overrides") r.push(`${path}: new ${f}.${name}`); continue; }
      if (typeof x[name] !== "string" || typeof y[name] !== "string") { r.push(`${path}: ${f}.${name} changed shape`); continue; }
      if (!compatibleBump(x[name], y[name])) r.push(`${path}: ${f}.${name} ${x[name]} -> ${y[name]} is not a same-major upgrade`);
    }
  }
  return r;
}

export function checkLockfile(path, before, after) {
  const r = [];
  let a, b;
  try { a = JSON.parse(before ?? "{}"); b = JSON.parse(after ?? "{}"); } catch { return [`${path}: invalid JSON`]; }
  const scripted = new Set(Object.entries(a.packages || {}).filter(([, p]) => p.hasInstallScript).map(([k]) => k.replace(/^.*node_modules\//, "")));
  for (const [key, p] of Object.entries(b.packages || {})) {
    if (!key || p.link || !key.includes("node_modules/")) continue;
    if (p.resolved && !REGISTRY.test(p.resolved)) r.push(`${path}: ${key} resolves outside registry.npmjs.org (${p.resolved})`);
    if (p.resolved && !p.integrity) r.push(`${path}: ${key} has no integrity hash`);
    const name = key.replace(/^.*node_modules\//, "");
    if (p.hasInstallScript && !scripted.has(name)) r.push(`${path}: ${name} adds an install script`);
  }
  return r;
}

// Text manifests and workflows: every changed line must pair with a line that differs only in
// version tokens (or, for workflows, only in the ref of the same `uses:` action).
export function checkLinePairs(path, diff, isWorkflow) {
  const r = [];
  const hunks = diff.split(/^@@.*$/m).slice(1);
  for (const h of hunks) {
    const lines = h.split("\n");
    const del = lines.filter((l) => l.startsWith("-")).map((l) => l.slice(1));
    const add = lines.filter((l) => l.startsWith("+")).map((l) => l.slice(1));
    if (del.length !== add.length) { r.push(`${path}: lines added or removed, not just versions changed`); continue; }
    del.forEach((d, i) => {
      const n = add[i];
      if (isWorkflow) {
        const re = /^(\s*-?\s*uses:\s*)([^@\s]+)@(\S+)(\s*#\s*(\S+))?\s*$/;
        const m1 = d.match(re), m2 = n.match(re);
        if (!m1 || !m2 || m1[2] !== m2[2]) { r.push(`${path}: non-'uses:' line changed: ${n.trim()}`); return; }
        const v1 = m1[5] || m1[3], v2 = m2[5] || m2[3];
        if ([v1, v2].some((v) => !/^v?\d/.test(v))) { r.push(`${path}: ${m2[2]} ref without a version (${v2})`); return; }
        if (!compatibleBump(v1, v2)) r.push(`${path}: ${m1[2]} ${v1} -> ${v2} is not a same-major upgrade`);
        return;
      }
      if (d.replace(VER, "V") !== n.replace(VER, "V")) { r.push(`${path}: changed beyond a version: ${n.trim()}`); return; }
      const v1 = d.match(VER) || [], v2 = n.match(VER) || [];
      v1.forEach((v, j) => { if (v !== v2[j] && !compatibleBump(v, v2[j])) r.push(`${path}: ${v} -> ${v2[j]} is not a same-major upgrade`); });
    });
  }
  return r;
}

export function evaluate(base) {
  const files = git("diff", "--name-status", "--no-renames", `${base}...HEAD`).split("\n").filter(Boolean)
    .map((l) => { const [s, ...p] = l.split("\t"); return { status: s, path: p.join("\t") }; });
  const head = "HEAD", mb = git("merge-base", base, "HEAD").trim();
  const reasons = [];
  if (!files.length) reasons.push("empty diff");
  if (files.length > LIMITS.files) reasons.push(`${files.length} files changed (max ${LIMITS.files})`);
  for (const { status, path } of files) {
    if (NEVER.some((re) => re.test(path))) { reasons.push(`protected path: ${path}`); continue; }
    if (status !== "M") { reasons.push(`${path}: ${status === "A" ? "added" : "deleted"} (only edits allowed)`); continue; }
    if (PKG_JSON.test(path)) reasons.push(...checkPackageJson(path, show(mb, path), show(head, path)));
    else if (PKG_LOCK.test(path)) reasons.push(...checkLockfile(path, show(mb, path), show(head, path)));
    else if (TEXT_MANIFEST.test(path) || WORKFLOW.test(path))
      reasons.push(...checkLinePairs(path, git("diff", "-U0", "--no-renames", `${mb}`, head, "--", path), WORKFLOW.test(path)));
    else reasons.push(`not a dependency file: ${path}`);
  }
  return { ok: reasons.length === 0, reasons, files: files.map((f) => f.path) };
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const r = evaluate(process.argv[2] || "origin/main");
  console.log(`files: ${r.files.join(", ") || "-"}`);
  if (r.ok) console.log("security-patch-gate: PASS (same-major dependency bump only)");
  else { console.log("security-patch-gate: FAIL — needs owner review:"); for (const x of r.reasons) console.log(`  - ${x}`); process.exitCode = 1; }
}
