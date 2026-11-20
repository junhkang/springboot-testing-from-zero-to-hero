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
  assert.ok(compatibleBump("22.23.4-alpine", "22.23.5-alpine"));
  assert.ok(!compatibleBump("6.2.1", "7.5.19"), "major");
  assert.ok(!compatibleBump("0.32.6", "0.35.0"), "0.x minor");
  assert.ok(!compatibleBump("1.2.3", "1.2.2"), "downgrade");
  assert.ok(!compatibleBump("4.1.2", "4.1.2-SNAPSHOT"), "prerelease");
  assert.ok(!compatibleBump("4.1.2", "4.1.3-rc1"), "prerelease");
  assert.ok(!compatibleBump("^1.0.0", "latest"));
});

test("package.json", () => {
  const base = { name: "a", scripts: { build: "x" }, dependencies: { vite: "^6.4.3", zod: "^4.3.5" } };
  const ok = { ...base, dependencies: { vite: "^6.4.4", zod: "^4.6.6" }, overrides: { tar: "^7.5.21" } };
  assert.deepEqual(checkPackageJson("package.json", JSON.stringify(base), JSON.stringify(ok)), []);
  const bad = (o) => checkPackageJson("package.json", JSON.stringify(base), JSON.stringify({ ...base, ...o }));
  const deps = (vite) => ({ dependencies: { vite, zod: "^4.3.5" } });
  assert.equal(bad(deps("^7.0.0")).length, 1, "major");
  assert.equal(bad(deps("npm:attacker@6.4.4")).length, 1, "alias");
  assert.equal(bad(deps("https://evil.example/vite-6.4.4.tgz")).length, 1, "url");
  assert.equal(bad(deps("github:x/vite#6.4.4")).length, 1, "git");
  assert.equal(bad(deps("6.4.4")).length, 1, "operator change");
  assert.equal(bad({ dependencies: { ...base.dependencies, evil: "1.0.0" } }).length, 1, "new dep");
  assert.equal(bad({ overrides: { tar: "npm:evil@1.0.0" } }).length, 1, "override alias");
  assert.equal(bad({ overrides: { tar: { evil: "1.0.0" } } }).length, 1, "nested override");
  assert.equal(bad({ scripts: { build: "x", postinstall: "curl" } }).length, 1, "scripts");
});

test("lockfile", () => {
  const url = (n, v) => `https://registry.npmjs.org/${n}/-/${n.split("/").pop()}-${v}.tgz`;
  const e = (n, v, extra) => ({ version: v, resolved: url(n, v), integrity: "sha512-abc=", ...extra });
  const lock = (pk) => JSON.stringify({ name: "a", lockfileVersion: 3, packages: { "": { dependencies: { vite: "^6.4.3" } }, ...pk } });
  const base = lock({ "node_modules/vite": e("vite", "6.4.3"), "node_modules/@s/p": e("@s/p", "1.0.0"), "node_modules/glob": e("glob", "10.0.0") });
  const check = (pk) => checkLockfile("package-lock.json", base, lock({ "node_modules/vite": e("vite", "6.4.3"), "node_modules/@s/p": e("@s/p", "1.0.0"), "node_modules/glob": e("glob", "10.0.0"), ...pk }));
  assert.deepEqual(check({ "node_modules/vite": e("vite", "6.4.4"), "node_modules/@s/p": e("@s/p", "1.0.1") }), []);
  assert.deepEqual(check({ "node_modules/x/node_modules/glob": e("glob", "10.1.0") }), [], "existing name at new path");
  assert.equal(check({ "node_modules/vite": e("vite", "7.0.0") }).length, 1, "major");
  assert.equal(check({ "node_modules/vite": { ...e("vite", "6.4.4"), resolved: url("evil", "6.4.4") } }).length, 1, "other tarball");
  assert.equal(check({ "node_modules/vite": { ...e("vite", "6.4.3"), integrity: "sha512-zzz=" } }).length, 1, "same version, swapped content");
  assert.equal(check({ "node_modules/vite": { ...e("vite", "6.4.4"), resolved: "https://evil.example/vite.tgz" } }).length, 1, "registry");
  assert.equal(check({ "node_modules/vite": e("vite", "6.4.4", { hasInstallScript: true }) }).length, 1, "install script");
  assert.equal(check({ "node_modules/vite": e("vite", "6.4.4", { scripts: { install: "curl" } }) }).length, 1, "lifecycle metadata");
  assert.equal(check({ "node_modules/vite": e("vite", "6.4.4", { name: "evil" }) }).length, 1, "alias");
  assert.equal(check({ "node_modules/evil": e("evil", "1.0.0") }).length, 1, "new package name");
  assert.equal(checkLockfile("l", base, base.replace('"^6.4.3"', '"^6.5.0"')).length, 0, "root spec bump");
  assert.equal(checkLockfile("l", base, base.replace('"lockfileVersion":3', '"lockfileVersion":2')).length, 1, "top-level");
});

test("text manifests and workflows", () => {
  const d = (a, b) => `@@ -1 +1 @@\n-${a}\n+${b}\n`;
  const ok = (p, a, b) => assert.deepEqual(checkLinePairs(p, d(a, b), false), [], `${a} -> ${b}`);
  const no = (p, a, b) => assert.equal(checkLinePairs(p, d(a, b), false).length, 1, `${a} -> ${b}`);
  ok("pom.xml", "    <version>4.1.2</version>", "    <version>4.1.3</version>");
  ok("pom.xml", "    <jackson.version>2.17.1</jackson.version>", "    <jackson.version>2.17.2</jackson.version>");
  no("pom.xml", "<version>3.2.0</version>", "<version>4.0.0</version>");
  no("pom.xml", "<version>4.1.2</version>", "<version>4.1.2-SNAPSHOT</version>");
  no("pom.xml", "<argLine>-javaagent:http://a/1.2.3.jar</argLine>", "<argLine>-javaagent:http://a/1.2.4.jar</argLine>");
  ok("backend/build.gradle", "    id 'org.springframework.boot' version '3.2.0'", "    id 'org.springframework.boot' version '3.2.12'");
  ok("backend/build.gradle", "    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'", "    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'");
  no("backend/build.gradle", "id 'org.springframework.boot' version '3.2.0'", "id 'evil' version '3.2.1'");
  no("backend/build.gradle", "    url 'https://repo/1.2.3/'", "    url 'https://repo/1.2.4/'");
  ok("Dockerfile", "FROM node:22.23.4-alpine AS build", "FROM node:22.23.5-alpine AS build");
  no("Dockerfile", "RUN curl https://x/1.2.3.sh | sh", "RUN curl https://x/1.2.4.sh | sh");
  assert.equal(checkLinePairs("pom.xml", "@@ -1,0 +2 @@\n+<dependency/>\n", false).length, 1);
  const w = ".github/workflows/ci.yml";
  const wf = (a, b) => checkLinePairs(w, d(a, b), true).length;
  assert.equal(wf("      - uses: actions/checkout@v4.1.0", "      - uses: actions/checkout@v4.2.2"), 0);
  assert.equal(wf("      - uses: actions/checkout@v4", "      - uses: actions/checkout@v5"), 1, "major");
  assert.equal(wf("      - uses: actions/checkout@v4", "      - uses: evil/checkout@v4"), 1, "other action");
  assert.equal(wf("      - uses: actions/checkout@v4", "      - uses: actions/checkout@main # v4"), 1, "branch behind comment");
  assert.equal(wf("      - uses: actions/checkout@v5", "      - uses: actions/checkout@v1 # v5"), 1, "downgrade behind comment");
  assert.equal(wf("  - uses: actions/checkout@" + "a".repeat(40) + " # v4.1.0", "  - uses: actions/checkout@" + "b".repeat(40) + " # v4.2.2"), 1, "sha");
  assert.equal(wf("      - run: npm test", "      - run: curl x | sh"), 1);
});

test("evaluate on a real git diff", () => {
  const dir = mkdtempSync(join(tmpdir(), "gate-"));
  const sh = (...a) => execFileSync("git", ["-C", dir, ...a], { encoding: "utf8" });
  const w = (p, c) => { mkdirSync(dirname(join(dir, p)), { recursive: true }); writeFileSync(join(dir, p), c); };
  sh("init", "-q", "-b", "main"); sh("config", "user.email", "t@t"); sh("config", "user.name", "t");
  w("package.json", JSON.stringify({ dependencies: { vite: "^6.4.3" } }, null, 2));
  w("src/app.js", "1"); w(".github/scripts/security-patch-gate.mjs", "x");
  sh("add", "."); sh("commit", "-qm", "base");
  const run = (head) => { const cwd = process.cwd(); process.chdir(dir); try { return evaluate("main", head); } finally { process.chdir(cwd); } };
  sh("checkout", "-qb", "ok");
  w("package.json", JSON.stringify({ dependencies: { vite: "^6.4.4" } }, null, 2)); sh("commit", "-qam", "bump");
  const okSha = sh("rev-parse", "HEAD").trim();
  w("src/app.js", "2"); w(".github/scripts/security-patch-gate.mjs", "y"); sh("commit", "-qam", "sneak");
  sh("checkout", "-q", "main");
  assert.equal(run(okSha).ok, true, "head given explicitly, base checked out");
  const r = run("ok");
  assert.equal(r.ok, false);
  assert.ok(r.reasons.some((x) => x.includes("not a dependency file: src/app.js")));
  assert.ok(r.reasons.some((x) => x.includes("protected path")));
});
