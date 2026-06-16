#!/usr/bin/env node

import { spawn } from "node:child_process";
import { createWriteStream } from "node:fs";
import { mkdir } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const DEFAULT_CONFIG = Object.freeze({
  keycloakUrl: "http://localhost:18080",
  realm: "questlog",
  smokeClientId: "questlog-smoke",
  username: "questlog",
  password: "questlog",
  adminUsername: "admin",
  adminPassword: "admin",
  backendUrl: "http://localhost:8081",
  bffUrl: "http://localhost:8082",
});

export function parseArgs(argv) {
  const options = {
    compose: true,
    apps: true,
  };

  for (const arg of argv) {
    if (arg === "--manual-apps") {
      options.apps = false;
    } else if (arg === "--skip-compose") {
      options.compose = false;
    } else if (arg === "--help" || arg === "-h") {
      options.help = true;
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  return options;
}

export function configFromEnv(env = process.env) {
  return {
    keycloakUrl: env.QUESTLOG_KEYCLOAK_URL ?? DEFAULT_CONFIG.keycloakUrl,
    realm: env.QUESTLOG_AUTH_REALM ?? DEFAULT_CONFIG.realm,
    smokeClientId: env.QUESTLOG_SMOKE_CLIENT_ID ?? DEFAULT_CONFIG.smokeClientId,
    username: env.QUESTLOG_SMOKE_USERNAME ?? DEFAULT_CONFIG.username,
    password: env.QUESTLOG_SMOKE_PASSWORD ?? DEFAULT_CONFIG.password,
    adminUsername: env.QUESTLOG_KEYCLOAK_ADMIN_USERNAME ?? DEFAULT_CONFIG.adminUsername,
    adminPassword: env.QUESTLOG_KEYCLOAK_ADMIN_PASSWORD ?? DEFAULT_CONFIG.adminPassword,
    backendUrl: env.QUESTLOG_BACKEND_URL ?? DEFAULT_CONFIG.backendUrl,
    bffUrl: env.QUESTLOG_BFF_URL ?? DEFAULT_CONFIG.bffUrl,
  };
}

export function tokenEndpoint(config) {
  return `${config.keycloakUrl}/realms/${config.realm}/protocol/openid-connect/token`;
}

export function passwordGrantBody(config) {
  return new URLSearchParams({
    grant_type: "password",
    client_id: config.smokeClientId,
    username: config.username,
    password: config.password,
  });
}

export function adminGrantBody(config) {
  return new URLSearchParams({
    grant_type: "password",
    client_id: "admin-cli",
    username: config.adminUsername,
    password: config.adminPassword,
  });
}

export function isExpectedStatus(actual, expected) {
  return Array.isArray(expected) ? expected.includes(actual) : actual === expected;
}

export async function assertHttpStatus(name, url, init, expected) {
  const response = await fetch(url, init);
  if (!isExpectedStatus(response.status, expected)) {
    const body = await response.text();
    throw new Error(`${name} expected HTTP ${expected}, got ${response.status}: ${body.slice(0, 500)}`);
  }
  return response;
}

async function run(command, args, options = {}) {
  return new Promise((resolvePromise, reject) => {
    const child = spawn(command, args, {
      cwd: options.cwd,
      shell: false,
      stdio: options.stdio ?? "pipe",
      env: options.env ?? process.env,
    });

    let stdout = "";
    let stderr = "";
    child.stdout?.on("data", (chunk) => {
      stdout += chunk;
    });
    child.stderr?.on("data", (chunk) => {
      stderr += chunk;
    });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) {
        resolvePromise({ stdout, stderr });
      } else {
        reject(new Error(`${command} ${args.join(" ")} failed with ${code}\n${stdout}\n${stderr}`));
      }
    });
  });
}

async function waitFor(name, probe, timeoutMs = 120000, intervalMs = 2000) {
  const deadline = Date.now() + timeoutMs;
  let lastError;

  while (Date.now() < deadline) {
    try {
      const result = await probe();
      if (result) {
        return result;
      }
    } catch (error) {
      lastError = error;
    }
    await new Promise((resolvePromise) => setTimeout(resolvePromise, intervalMs));
  }

  throw new Error(`${name} was not ready within ${timeoutMs}ms${lastError ? `: ${lastError.message}` : ""}`);
}

async function waitForHttp(name, url, expected = 200, timeoutMs = 120000) {
  return waitFor(name, async () => {
    const response = await fetch(url);
    return isExpectedStatus(response.status, expected);
  }, timeoutMs);
}

async function ensureCompose(repoRoot, config) {
  console.log("Starting Docker Compose services...");
  await run("docker", ["compose", "up", "-d"], { cwd: repoRoot });

  console.log("Checking PostgreSQL readiness...");
  await waitFor("PostgreSQL", async () => {
    try {
      await run("docker", ["compose", "exec", "-T", "postgres", "pg_isready", "-U", "postgres", "-d", "questlog"], {
        cwd: repoRoot,
      });
      return true;
    } catch {
      return false;
    }
  });

  console.log("Checking Keycloak readiness...");
  await waitForHttp("Keycloak realm", `${config.keycloakUrl}/realms/${config.realm}/.well-known/openid-configuration`);
}

async function requestToken(url, body) {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body,
  });

  if (!response.ok) {
    throw new Error(`Token request failed with HTTP ${response.status}: ${(await response.text()).slice(0, 500)}`);
  }

  const payload = await response.json();
  if (!payload.access_token) {
    throw new Error("Token response did not contain access_token");
  }
  return payload.access_token;
}

async function ensureSmokeClient(config) {
  const adminToken = await requestToken(
    `${config.keycloakUrl}/realms/master/protocol/openid-connect/token`,
    adminGrantBody(config),
  );
  const clientsUrl = `${config.keycloakUrl}/admin/realms/${config.realm}/clients`;
  const existing = await fetch(`${clientsUrl}?clientId=${encodeURIComponent(config.smokeClientId)}`, {
    headers: {
      Authorization: `Bearer ${adminToken}`,
    },
  });

  if (!existing.ok) {
    throw new Error(`Failed to query Keycloak clients: HTTP ${existing.status}`);
  }

  const clients = await existing.json();
  if (clients.length > 0) {
    return;
  }

  const createResponse = await fetch(clientsUrl, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${adminToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      clientId: config.smokeClientId,
      name: "QuestLog Local Smoke Client",
      enabled: true,
      publicClient: true,
      standardFlowEnabled: false,
      directAccessGrantsEnabled: true,
      serviceAccountsEnabled: false,
      protocol: "openid-connect",
    }),
  });

  if (!createResponse.ok) {
    throw new Error(`Failed to create Keycloak smoke client: HTTP ${createResponse.status}`);
  }
}

function mvnwCommand() {
  return process.platform === "win32" ? "mvnw.cmd" : "./mvnw";
}

async function spawnApp(name, cwd, logFile) {
  const log = createWriteStream(logFile, { flags: "w" });
  const child = spawn(mvnwCommand(), ["spring-boot:run", "-Dspring-boot.run.profiles=prod"], {
    cwd,
    shell: process.platform === "win32",
    detached: process.platform !== "win32",
    stdio: ["ignore", "pipe", "pipe"],
  });
  child.stdout.pipe(log);
  child.stderr.pipe(log);
  child.on("exit", (code) => {
    if (code !== null && code !== 0) {
      console.error(`${name} exited with ${code}. See ${logFile}`);
    }
  });
  return child;
}

async function stopApp(child) {
  if (!child || child.exitCode !== null) {
    return;
  }

  if (process.platform === "win32") {
    await run("taskkill", ["/pid", String(child.pid), "/T", "/F"]).catch(() => undefined);
  } else {
    try {
      process.kill(-child.pid, "SIGTERM");
    } catch {
      child.kill("SIGTERM");
    }
  }
}

async function ensureApps(repoRoot, config) {
  const runtimeDir = resolve(repoRoot, ".hermes", "runtime");
  await mkdir(runtimeDir, { recursive: true });
  const started = [];

  const backendAlreadyRunning = await waitForHttp("backend", `${config.backendUrl}/api/be/health`, 200, 3000)
    .then(() => true)
    .catch(() => false);
  if (backendAlreadyRunning) {
    await assertHttpStatus(
      "existing backend must be running with JWT authentication enabled; stop the local-profile backend on 8081 or restart it with -Dspring-boot.run.profiles=prod",
      `${config.backendUrl}/api/be/goals`,
      {},
      401,
    );
    console.log("Backend is already running.");
  } else {
    console.log("Starting backend with prod profile...");
    started.push(await spawnApp("backend", resolve(repoRoot, "apps", "quest-log-be"), resolve(runtimeDir, "auth-smoke-backend.log")));
  }

  const bffAlreadyRunning = await waitForHttp("BFF", `${config.bffUrl}/api/bff/health`, 200, 3000)
    .then(() => true)
    .catch(() => false);
  if (bffAlreadyRunning) {
    await assertHttpStatus(
      "existing BFF must be running with JWT authentication enabled; stop the local-profile BFF on 8082 or restart it with -Dspring-boot.run.profiles=prod",
      `${config.bffUrl}/api/bff/dashboard`,
      {},
      401,
    );
    console.log("BFF is already running.");
  } else {
    console.log("Starting BFF with prod profile...");
    started.push(await spawnApp("BFF", resolve(repoRoot, "apps", "quest-log-bff"), resolve(runtimeDir, "auth-smoke-bff.log")));
  }

  await waitForHttp("backend", `${config.backendUrl}/api/be/health`, 200, 180000);
  await waitForHttp("BFF", `${config.bffUrl}/api/bff/health`, 200, 180000);
  await assertHttpStatus("backend JWT authentication readiness", `${config.backendUrl}/api/be/goals`, {}, 401);
  await assertHttpStatus("BFF JWT authentication readiness", `${config.bffUrl}/api/bff/dashboard`, {}, 401);

  return started;
}

async function verifyAuthenticatedApis(config, accessToken) {
  const authHeaders = {
    Authorization: `Bearer ${accessToken}`,
  };

  await assertHttpStatus("backend protected API rejects anonymous access", `${config.backendUrl}/api/be/goals`, {}, 401);
  await assertHttpStatus("BFF protected API rejects anonymous access", `${config.bffUrl}/api/bff/dashboard`, {}, 401);
  await assertHttpStatus("backend protected API accepts bearer access", `${config.backendUrl}/api/be/goals`, {
    headers: authHeaders,
  }, 200);
  await assertHttpStatus("BFF dashboard accepts bearer access and forwards to backend", `${config.bffUrl}/api/bff/dashboard`, {
    headers: authHeaders,
  }, 200);
}

function usage() {
  return `Usage: node scripts/auth-smoke.mjs [--manual-apps] [--skip-compose]

Options:
  --manual-apps   Do not start backend/BFF; require them to be running with the prod profile.
  --skip-compose  Do not run docker compose up -d; require PostgreSQL and Keycloak to be running.
`;
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    console.log(usage());
    return;
  }

  const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
  const config = configFromEnv();
  const startedApps = [];

  try {
    if (options.compose) {
      await ensureCompose(repoRoot, config);
    }

    console.log("Ensuring local Keycloak smoke client...");
    await ensureSmokeClient(config);

    if (options.apps) {
      startedApps.push(...await ensureApps(repoRoot, config));
    } else {
      await waitForHttp("backend", `${config.backendUrl}/api/be/health`);
      await waitForHttp("BFF", `${config.bffUrl}/api/bff/health`);
      await assertHttpStatus("backend JWT authentication readiness", `${config.backendUrl}/api/be/goals`, {}, 401);
      await assertHttpStatus("BFF JWT authentication readiness", `${config.bffUrl}/api/bff/dashboard`, {}, 401);
    }

    console.log("Obtaining development-user access token...");
    const accessToken = await requestToken(tokenEndpoint(config), passwordGrantBody(config));

    console.log("Verifying authenticated backend and BFF access...");
    await verifyAuthenticatedApis(config, accessToken);

    console.log("Authenticated smoke workflow passed.");
  } finally {
    for (const child of startedApps.reverse()) {
      await stopApp(child);
    }
  }
}

const invokedPath = process.argv[1] ? fileURLToPath(import.meta.url) === resolve(process.argv[1]) : false;
if (invokedPath) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
