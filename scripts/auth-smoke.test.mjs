import assert from "node:assert/strict";
import test from "node:test";
import {
  DEFAULT_CONFIG,
  adminGrantBody,
  configFromEnv,
  isExpectedStatus,
  parseArgs,
  passwordGrantBody,
  tokenEndpoint,
} from "./auth-smoke.mjs";

test("parseArgs enables safe defaults", () => {
  assert.deepEqual(parseArgs([]), {
    compose: true,
    apps: true,
  });
});

test("parseArgs supports manual app and compose modes", () => {
  assert.deepEqual(parseArgs(["--manual-apps", "--skip-compose"]), {
    compose: false,
    apps: false,
  });
});

test("configFromEnv applies documented overrides", () => {
  const config = configFromEnv({
    QUESTLOG_KEYCLOAK_URL: "http://keycloak.test",
    QUESTLOG_AUTH_REALM: "custom",
    QUESTLOG_SMOKE_CLIENT_ID: "smoke",
    QUESTLOG_SMOKE_USERNAME: "hero",
    QUESTLOG_SMOKE_PASSWORD: "secret",
    QUESTLOG_KEYCLOAK_ADMIN_USERNAME: "root",
    QUESTLOG_KEYCLOAK_ADMIN_PASSWORD: "root-secret",
    QUESTLOG_BACKEND_URL: "http://backend.test",
    QUESTLOG_BFF_URL: "http://bff.test",
  });

  assert.equal(config.keycloakUrl, "http://keycloak.test");
  assert.equal(config.realm, "custom");
  assert.equal(config.smokeClientId, "smoke");
  assert.equal(config.username, "hero");
  assert.equal(config.password, "secret");
  assert.equal(config.adminUsername, "root");
  assert.equal(config.adminPassword, "root-secret");
  assert.equal(config.backendUrl, "http://backend.test");
  assert.equal(config.bffUrl, "http://bff.test");
});

test("token helpers build Keycloak password grant requests", () => {
  assert.equal(
    tokenEndpoint(DEFAULT_CONFIG),
    "http://localhost:18080/realms/questlog/protocol/openid-connect/token",
  );

  const body = passwordGrantBody(DEFAULT_CONFIG);
  assert.equal(body.get("grant_type"), "password");
  assert.equal(body.get("client_id"), "questlog-smoke");
  assert.equal(body.get("username"), "questlog");
  assert.equal(body.get("password"), "questlog");
});

test("admin token helper targets built-in admin-cli", () => {
  const body = adminGrantBody(DEFAULT_CONFIG);
  assert.equal(body.get("grant_type"), "password");
  assert.equal(body.get("client_id"), "admin-cli");
  assert.equal(body.get("username"), "admin");
  assert.equal(body.get("password"), "admin");
});

test("isExpectedStatus handles single status and alternatives", () => {
  assert.equal(isExpectedStatus(200, 200), true);
  assert.equal(isExpectedStatus(201, 200), false);
  assert.equal(isExpectedStatus(401, [401, 403]), true);
  assert.equal(isExpectedStatus(500, [401, 403]), false);
});
