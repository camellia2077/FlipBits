import { defineConfig, devices } from "@playwright/test";
import { fileURLToPath } from "node:url";
import path from "node:path";

const toolsWebDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(toolsWebDir, "../..");
const siteDir = path.join(repoRoot, "apps/audio_web/site");
const artifactRoot = path.join(repoRoot, "build/test-artifacts/web");

export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  timeout: 30_000,
  expect: {
    timeout: 5_000
  },
  outputDir: path.join(artifactRoot, "test-results"),
  reporter: [
    ["list"],
    ["html", { outputFolder: path.join(artifactRoot, "playwright-report"), open: "never" }]
  ],
  use: {
    baseURL: "http://127.0.0.1:4174",
    trace: "retain-on-failure"
  },
  webServer: {
    command: `python ${path.join(repoRoot, "tools/run.py")} web serve-site --port 4174`,
    cwd: repoRoot,
    url: "http://127.0.0.1:4174",
    reuseExistingServer: false,
    timeout: 30_000
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"], browserName: "chromium" }
    },
    {
      name: "webkit",
      use: { ...devices["Desktop Safari"], browserName: "webkit" }
    }
  ],
  metadata: {
    siteDir
  }
});
