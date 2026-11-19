#!/usr/bin/env node
// Deterministic gate for Security Shield auto-merges: a dependency PR may merge without a
// human only when it is a same-major version bump of packages already in use, from the public
// registries. Anything else (new deps, majors, prereleases, aliases, URLs, code, scripts, CI
// logic) needs the owner. FAIL is not an error, it means "owner review".
// Usage: node .github/scripts/security-patch-gate.mjs <base-ref> <head-ref>
// The workflow runs this file from the base branch via pull_request_target and only reads the
// PR through git, so a PR can neither change the gate nor execute its own code here.
import { execFileSync } from "node:child_process";

const git = (...a) => execFileSync("git", a, { encoding: "utf8", maxBuffer: 1 << 28 });
const show = (ref, path) => { try { return git("show", `${ref}:${path}`); } catch { return null; } };

const PKG_JSON = /(^|\/)package\.json$/;
const PKG_LOCK = /(^|\/)package-lock\.json$/;
const WORKFLOW = /^\.github\/workflows\/[^/]+\.ya?ml$/;
// Changed lines in these files must have exactly one of these shapes.
const LINE_SHAPES = [
  [/(^|\/)pom\.xml$/, [/^\s*<(version|[\w.-]+\.version)>[^<\s]+<\/\1>\s*$/]],
  [/(^|\/)build\.gradle(\.kts)?$/, [
    /^\s*id\s*\(?\s*["'][\w.-]+["']\s*\)?\s+version\s+["'][^"'\s]+["']\s*$/,
    /^\s*(implementation|api|compileOnly|runtimeOnly|testImplementation|testRuntimeOnly|annotationProcessor|testAnnotationProcessor|developmentOnly)\s*\(?\s*["'][\w.-]+:[\w.-]+:[^"'\s:]+["']\s*\)?\s*$/,
  ]],
  [/(^|\/)libs\.versions\.toml$/, [/^\s*[\w.-]+\s*=\s*"[^"\s]+"\s*$/]],
  [/(^|\/)Dockerfile[^/]*$/, [/^\s*FROM\s+[\w./-]+:[\w.-]+(\s+AS\s+[\w-]+)?\s*$/i]],
];
const NEVER = [/^\.github\/scripts\//, /^\.github\/workflows\/security-patch-gate\.ya?ml$/, /^\.github\/dependabot\.ya?ml$/, /(^|\/)themes\//, /(^|\/)(android|ios)\//];
const DEP_FIELDS = ["dependencies", "devDependencies", "optionalDependencies", "peerDependencies", "overrides"];
const LOCK_KEYS = new Set(["version", "resolved", "integrity", "dev", "optional", "devOptional", "peer", "license", "engines", "bin", "os", "cpu", "libc", "funding", "dependencies", "optionalDependencies", "peerDependencies", "peerDependenciesMeta", "hasInstallScript", "deprecated", "name", "inBundle", "bundleDependencies", "bundledDependencies"]);
const NPM_SPEC = /^[\^~]?\d+\.\d+\.\d+$/;
export const LIMITS = { files: 12 };

const VER = /\d+(?:\.\d+)+(?:[-+.]?[0-9A-Za-z][0-9A-Za-z.-]*)?/g;
const split = (v) => {
  const m = String(v).match(/^[\^~v]?(\d+(?:\.\d+)*)(.*)$/);
  return m ? { nums: m[1].split(".").map(Number), rest: m[2] } : null;
};

// Same "compatibility line": same major (same major.minor while major is 0), identical
// qualifier (-alpine, -jre; so no switch to -SNAPSHOT/-rc), never a downgrade.
export function compatibleBump(from, to) {
  const a = split(from), b = split(to);
  if (!a || !b || a.rest !== b.rest) return false;
  if (a.nums[0] !== b.nums[0]) return false;
  if (a.nums[0] === 0 && (a.nums[1] ?? 0) !== (b.nums[1] ?? 0)) return false;
  for (let i = 0; i < Math.max(a.nums.length, b.nums.length); i++) {
    const x = a.nums[i] ?? 0, y = b.nums[i] ?? 0;
    if (x !== y) return y > x;
  }
  return true;
}

const strip = (o, keys) => Object.fromEntries(Object.entries(o || {}).filter(([k]) => !keys.includes(k)));
const stable = (v) => JSON.stringify(v, (_, x) => (x && typeof x === "object" && !Array.isArray(x) ? Object.fromEntries(Object.entries(x).sort()) : x));
const parse = (s) => { try { return [JSON.parse(s ?? "{}")]; } catch { return [null]; } };

function checkSpec(where, from, to) {
  if (typeof to !== "string" || !NPM_SPEC.test(to)) return [`${where}: '${stable(to)}' is not a plain semver version`];
  if (from === undefined) return [];
  if (typeof from !== "string" || !NPM_SPEC.test(from)) return [`${where}: base '${stable(from)}' is not a plain semver version`];
  if (from[0] !== to[0] && /[\^~]/.test(from[0] + to[0])) return [`${where}: range operator changed (${from} -> ${to})`];
  return compatibleBump(from, to) ? [] : [`${where}: ${from} -> ${to} is not a same-major upgrade`];
}

export function checkPackageJson(path, before, after) {
  const [a] = parse(before), [b] = parse(after);
  if (!a || !b) return [`${path}: invalid JSON`];
  const r = [];
  if (before == null) r.push(`${path}: new file`);
  if (stable(strip(a, DEP_FIELDS)) !== stable(strip(b, DEP_FIELDS))) r.push(`${path}: non-dependency fields changed`);
  for (const f of DEP_FIELDS) {
    const x = a[f] || {}, y = b[f] || {};
    for (const name of new Set([...Object.keys(x), ...Object.keys(y)])) {
      if (stable(x[name]) === stable(y[name])) continue;
      if (!(name in y)) { r.push(`${path}: ${f}.${name} removed`); continue; }
      if (!(name in x) && f !== "overrides") { r.push(`${path}: new ${f}.${name}`); continue; }
      r.push(...checkSpec(`${path}: ${f}.${name}`, x[name], y[name]));
    }
  }
  return r;
}

const pkgName = (key) => key.slice(key.lastIndexOf("node_modules/") + "node_modules/".length);
const tarball = (name, version) => `https://registry.npmjs.org/${name}/-/${name.split("/").pop()}-${version}.tgz`;

export function checkLockfile(path, before, after) {
  const [a] = parse(before), [b] = parse(after);
  if (!a || !b) return [`${path}: invalid JSON`];
  const r = [];
  if (stable(strip(a, ["packages"])) !== stable(strip(b, ["packages"]))) r.push(`${path}: top-level fields changed`);
  const A = a.packages || {}, B = b.packages || {};
  const known = new Set(Object.keys(A).filter((k) => k.includes("node_modules/")).map(pkgName));
  for (const [key, p] of Object.entries(B)) {
    const old = A[key];
    if (stable(old) === stable(p)) continue;
    if (!key.includes("node_modules/") || p.link || old?.link) {
      // Workspace/root entries mirror package.json; only dependency specs may move.
      const keys = ["dependencies", "devDependencies", "optionalDependencies", "peerDependencies"];
      if (!old || stable(strip(old, keys)) !== stable(strip(p, keys))) { r.push(`${path}: workspace entry '${key || "(root)"}' changed beyond dependency versions`); continue; }
      for (const f of keys) for (const [n, v] of Object.entries(p[f] || {}))
        if ((old[f] || {})[n] !== v) r.push(...checkSpec(`${path}: '${key || "(root)"}' ${f}.${n}`, (old[f] || {})[n] ?? "missing", v));
      continue;
    }
    const name = pkgName(key);
    const bad = Object.keys(p).filter((k) => !LOCK_KEYS.has(k));
    if (bad.length) r.push(`${path}: ${key} has unexpected fields: ${bad.join(", ")}`);
    if (p.name !== undefined && p.name !== old?.name) r.push(`${path}: ${key} is an alias (${p.name})`);
    if (p.resolved !== tarball(name, p.version)) r.push(`${path}: ${key} resolves to ${p.resolved}, expected the npm registry tarball for ${name}@${p.version}`);
    if (!/^sha512-[A-Za-z0-9+/]+=*$/.test(p.integrity || "")) r.push(`${path}: ${key} has no sha512 integrity`);
    if (p.hasInstallScript && !old?.hasInstallScript) r.push(`${path}: ${key} adds an install script`);
    if (old) {
      if (old.version === p.version) r.push(`${path}: ${key} changed without a version bump`);
      else if (!compatibleBump(old.version, p.version)) r.push(`${path}: ${key} ${old.version} -> ${p.version} is not a same-major upgrade`);
    } else if (!known.has(name)) r.push(`${path}: new package ${name}`);
  }
  return r;
}

// Every changed line must pair with a line that differs only in version tokens.
// Workflows: only the ref of the same `uses:` action, as a version tag (no SHAs, branches).
export function checkLinePairs(path, diff, isWorkflow) {
  const r = [];
  const shapes = LINE_SHAPES.find(([re]) => re.test(path))?.[1];
  for (const h of diff.split(/^@@.*$/m).slice(1)) {
    const lines = h.split("\n");
    const del = lines.filter((l) => l.startsWith("-")).map((l) => l.slice(1));
    const add = lines.filter((l) => l.startsWith("+")).map((l) => l.slice(1));
    if (del.length !== add.length) { r.push(`${path}: lines added or removed, not just versions changed`); continue; }
    del.forEach((d, i) => {
      const n = add[i];
      if (isWorkflow) {
        const re = /^(\s*-?\s*uses:\s*)([\w.-]+\/[\w./-]+)@(v?\d+(?:\.\d+)*)\s*$/;
        const m1 = d.match(re), m2 = n.match(re);
        if (!m1 || !m2 || m1[1] !== m2[1] || m1[2] !== m2[2]) { r.push(`${path}: not a same-action version-tag change: ${n.trim()}`); return; }
        if (!compatibleBump(m1[3], m2[3])) r.push(`${path}: ${m1[2]} ${m1[3]} -> ${m2[3]} is not a same-major upgrade`);
        return;
      }
      if (!shapes || ![d, n].every((l) => shapes.some((s) => s.test(l)))) { r.push(`${path}: not a dependency version line: ${n.trim()}`); return; }
      if (d.replace(VER, "V") !== n.replace(VER, "V")) { r.push(`${path}: changed beyond a version: ${n.trim()}`); return; }
      const v1 = d.match(VER) || [], v2 = n.match(VER) || [];
      v1.forEach((v, j) => { if (v !== v2[j] && !compatibleBump(v, v2[j])) r.push(`${path}: ${v} -> ${v2[j]} is not a same-major upgrade`); });
    });
  }
  return r;
}

export function evaluate(base, head = "HEAD") {
  const mb = git("merge-base", base, head).trim();
  const files = git("diff", "--name-status", "--no-renames", mb, head).split("\n").filter(Boolean)
    .map((l) => { const [s, ...p] = l.split("\t"); return { status: s, path: p.join("\t") }; });
  const reasons = [];
  if (!files.length) reasons.push("empty diff");
  if (files.length > LIMITS.files) reasons.push(`${files.length} files changed (max ${LIMITS.files})`);
  for (const { status, path } of files) {
    if (NEVER.some((re) => re.test(path))) { reasons.push(`protected path: ${path}`); continue; }
    if (status !== "M") { reasons.push(`${path}: ${status === "A" ? "added" : "deleted"} (only edits allowed)`); continue; }
    if (PKG_JSON.test(path)) reasons.push(...checkPackageJson(path, show(mb, path), show(head, path)));
    else if (PKG_LOCK.test(path)) reasons.push(...checkLockfile(path, show(mb, path), show(head, path)));
    else if (WORKFLOW.test(path) || LINE_SHAPES.some(([re]) => re.test(path)))
      reasons.push(...checkLinePairs(path, git("diff", "-U0", "--no-renames", mb, head, "--", path), WORKFLOW.test(path)));
    else reasons.push(`not a dependency file: ${path}`);
  }
  return { ok: reasons.length === 0, reasons, files: files.map((f) => f.path) };
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const r = evaluate(process.argv[2] || "origin/main", process.argv[3] || "HEAD");
  console.log(`files: ${r.files.join(", ") || "-"}`);
  if (r.ok) console.log("security-patch-gate: PASS (same-major dependency bump only)");
  else { console.log("security-patch-gate: FAIL — needs owner review:"); for (const x of r.reasons) console.log(`  - ${x}`); process.exitCode = 1; }
}
