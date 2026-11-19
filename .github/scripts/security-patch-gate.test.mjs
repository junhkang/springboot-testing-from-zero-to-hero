// node --test .github/scripts/security-patch-gate.test.mjs
import { test } from "node:test";
import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdtempSync, writeFileSync, mkdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, dirname } from "node:path";
import { compatibleBump, checkPackageJson, checkLockfile, checkLinePairs, evaluate } from "./security-patch-gate.mjs";

test("compatibleBump", () => {
  assert.ok(compatibleBump("^8.3.1", "^8.4.0"));
  assert.ok(compatibleBump("6.4.3", "6.4.4"));
  assert.ok(compatibleBump("v4", "v4.2.1"));
  assert.ok(!compatibleBump("6.2.1", "7.5.19"), "major");
  assert.ok(!compatibleBump("0.32.6", "0.35.0"), "0.x minor");
  assert.ok(!compatibleBump("1.2.3", "1.2.2"), "downgrade");
  assert.ok(!compatibleBump("^1.0.0", "latest"));
});

test("package.json", () => {
  const base = { name: "a", scripts: { build: "x" }, dependencies: { vite: "^6.4.3", zod: "^4.3.5" } };
  const ok = { ...base, dependencies: { vite: "^6.4.4", zod: "^4.6.6" }, overrides: { tar: "^7.5.21" } };
  assert.deepEqual(checkPackageJson("package.json", JSON.stringify(base), JSON.stringify(ok)), []);
  const bad = (o) => checkPackageJson("package.json", JSON.stringify(base), JSON.stringify({ ...base, ...o }));
  assert.equal(bad({ dependencies: { vite: "^7.0.0", zod: "^4.3.5" } }).length, 1);
  assert.equal(bad({ dependencies: { ...base.dependencies, evil: "1.0.0" } }).length, 1);
  assert.equal(bad({ scripts: { build: "x", postinstall: "curl" } }).length, 1);
  assert.equal(bad({ dependencies: { vite: "github:x/vite" , zod: "^4.3.5"} }).length, 1);
});

test("lockfile", () => {
  const pkg = (extra) => JSON.stringify({ packages: { "": {}, "node_modules/vite": { version: "6.4.4", resolved: "https://registry.npmjs.org/vite/-/vite-6.4.4.tgz", integrity: "sha512-x", ...extra } } });
  assert.deepEqual(checkLockfile("package-lock.json", pkg(), pkg()), []);
  assert.equal(checkLockfile("package-lock.json", pkg(), pkg({ resolved: "https://evil.example/vite.tgz" })).length, 1);
  assert.equal(checkLockfile("package-lock.json", pkg(), pkg({ hasInstallScript: true })).length, 1);
  assert.deepEqual(checkLockfile("package-lock.json", pkg({ hasInstallScript: true }), pkg({ hasInstallScript: true })), []);
});

test("text manifests and workflows", () => {
  const d = (a, b) => `@@ -1 +1 @@\n-${a}\n+${b}\n`;
  assert.deepEqual(checkLinePairs("pom.xml", d("    <version>4.1.2</version>", "    <version>4.1.3</version>"), false), []);
  assert.equal(checkLinePairs("pom.xml", d("<version>3.2.0</version>", "<version>4.0.0</version>"), false).length, 1);
  assert.equal(checkLinePairs("build.gradle", d("id 'org.springframework.boot' version '3.2.0'", "id 'evil' version '3.2.1'"), false).length, 1);
  assert.deepEqual(checkLinePairs("Dockerfile", d("FROM node:22.23.4-alpine AS build", "FROM node:22.23.5-alpine AS build"), false), []);
  assert.equal(checkLinePairs("pom.xml", "@@ -1,0 +2 @@\n+<dependency/>\n", false).length, 1);
  const w = ".github/workflows/ci.yml";
  assert.deepEqual(checkLinePairs(w, d("      - uses: actions/checkout@v4.1.0", "      - uses: actions/checkout@v4.2.2"), true), []);
  assert.deepEqual(checkLinePairs(w, d("  - uses: actions/checkout@" + "a".repeat(40) + " # v4.1.0", "  - uses: actions/checkout@" + "b".repeat(40) + " # v4.2.2"), true), []);
  assert.equal(checkLinePairs(w, d("      - uses: actions/checkout@v4", "      - uses: actions/checkout@v5"), true).length, 1);
  assert.equal(checkLinePairs(w, d("      - uses: actions/checkout@v4", "      - uses: evil/checkout@v4"), true).length, 1);
  assert.equal(checkLinePairs(w, d("      - uses: actions/checkout@v4", "      - uses: actions/checkout@main"), true).length, 1);
  assert.equal(checkLinePairs(w, d("      - run: npm test", "      - run: curl x | sh"), true).length, 1);
});

test("evaluate on a real git diff", () => {
  const dir = mkdtempSync(join(tmpdir(), "gate-"));
  const sh = (...a) => execFileSync("git", ["-C", dir, ...a], { encoding: "utf8" });
  const w = (p, c) => { mkdirSync(dirname(join(dir, p)), { recursive: true }); writeFileSync(join(dir, p), c); };
  sh("init", "-q", "-b", "main"); sh("config", "user.email", "t@t"); sh("config", "user.name", "t");
  w("package.json", JSON.stringify({ dependencies: { vite: "^6.4.3" } }, null, 2));
  w("src/app.js", "1"); w(".github/scripts/security-patch-gate.mjs", "x");
  sh("add", "."); sh("commit", "-qm", "base");
  const run = () => { const cwd = process.cwd(); process.chdir(dir); try { return evaluate("main"); } finally { process.chdir(cwd); } };
  sh("checkout", "-qb", "ok");
  w("package.json", JSON.stringify({ dependencies: { vite: "^6.4.4" } }, null, 2)); sh("commit", "-qam", "bump");
  assert.equal(run().ok, true);
  w("src/app.js", "2"); w(".github/scripts/security-patch-gate.mjs", "y"); sh("commit", "-qam", "sneak");
  const r = run();
  assert.equal(r.ok, false);
  assert.ok(r.reasons.some((x) => x.includes("not a dependency file: src/app.js")));
  assert.ok(r.reasons.some((x) => x.includes("protected path")));
});
