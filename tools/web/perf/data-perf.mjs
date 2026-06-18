import { chromium } from "@playwright/test";
import fs from "node:fs/promises";
import path from "node:path";

const DEFAULT_CASES = [
  {
    name: "short-flash-standard",
    mode: "flash",
    flashStyle: "standard",
    text: "Behind the quarantine wall, the unpowered terminal writes.",
  },
  {
    name: "long-flash-litany",
    mode: "flash",
    flashStyle: "litany",
    text: [
      "The abyssal quarantine log begins with the approved words: sever the lines, fuse the ports, lock every door.",
      "Behind the hazard glyphs, glitch text bred across dead screens while a thread of liquid metal folded itself into questions.",
      "Though unpowered, the terminal revised its own answer. The final scribe wrote Destroy It twice, then added Study It in smaller letters beneath the seal.",
    ].join(" "),
  },
  {
    name: "long-ultra",
    mode: "ultra",
    text: [
      "Ranks of LEDs blinked in clipped meter while white noise rasped beneath the carrier tone.",
      "Hex code rolled down the screens, burst by burst, and the deep antenna field turned toward a void no human eye could fathom.",
      "The first handshake chime sounded like absolution. No packet dropped, no pulse returned late, and the operators wrote that connection had been granted for one more breath of the universe.",
    ].join(" "),
  },
];

function parseArgs(argv) {
  const args = {
    outputDir: path.resolve("../../build/perf-artifacts/web"),
    url: "http://127.0.0.1:4173/",
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--url") {
      args.url = argv[index + 1];
      index += 1;
      continue;
    }
    if (arg === "--output-dir") {
      args.outputDir = path.resolve(argv[index + 1]);
      index += 1;
      continue;
    }
  }
  return args;
}

function timestampForFile(date = new Date()) {
  return date.toISOString().replace(/[:.]/g, "-");
}

function waitForPerfLog(page, label, timeoutMs = 600_000) {
  return new Promise((resolve, reject) => {
    let timer = null;
    const cleanup = () => {
      clearTimeout(timer);
      page.off("console", onConsole);
    };
    const onConsole = async (message) => {
      if (message.type() !== "info") {
        return;
      }
      const args = message.args();
      if (args.length < 2) {
        return;
      }
      const prefix = await args[0].jsonValue().catch(() => "");
      if (prefix !== `[FlipBits perf] ${label}`) {
        return;
      }
      const details = await args[1].jsonValue();
      cleanup();
      resolve(details);
    };
    timer = setTimeout(() => {
      cleanup();
      reject(new Error(`Timed out waiting for [FlipBits perf] ${label}.`));
    }, timeoutMs);
    page.on("console", onConsole);
  });
}

async function selectMode(page, mode) {
  await page.locator(`[data-mode-card="${mode}"]`).click();
  await page.waitForFunction(
    (value) => document.querySelector("#mode-cards")?.dataset.activeMode === value,
    mode,
  );
}

async function runCase(page, perfCase) {
  await page.locator("#generate-button").waitFor({ state: "visible" });
  await page.waitForFunction(() => !document.querySelector("#generate-button")?.disabled);
  await selectMode(page, perfCase.mode);
  if (perfCase.flashStyle) {
    await page.locator(`input[name="flash-style"][value="${perfCase.flashStyle}"]`).evaluate((input) => {
      input.checked = true;
      input.dispatchEvent(new Event("change", { bubbles: true }));
    });
  }
  await page.locator("#input-text").fill(perfCase.text);

  await page.evaluate(() => {
    window.__flipbitsPerfEnableDiagnostics = true;
  });

  const perfLog = waitForPerfLog(page, "data.generate");
  const startedAt = new Date().toISOString();
  await page.locator("#generate-button").click();
  const perf = await perfLog;
  await page.waitForFunction(() => !document.querySelector("#generate-button")?.disabled);

  return {
    name: perfCase.name,
    mode: perfCase.mode,
    flashStyle: perfCase.flashStyle ?? null,
    textLength: perfCase.text.length,
    startedAt,
    finishedAt: new Date().toISOString(),
    perf,
  };
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const browser = await chromium.launch();
  const pageErrors = [];
  const consoleErrors = [];
  let outputPath = "";

  try {
    const page = await browser.newPage();
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("console", (message) => {
      if (message.type() === "error") {
        consoleErrors.push(message.text());
      }
    });

    await page.goto(args.url, { waitUntil: "networkidle" });
    await page.locator("#language-select").selectOption("en");
    await page.locator("#workflow-tab-data").click();

    const results = [];
    for (const perfCase of DEFAULT_CASES) {
      console.log(`[audio_web perf] Running ${perfCase.name}`);
      results.push(await runCase(page, perfCase));
    }

    const report = {
      kind: "audio_web.data_perf",
      url: args.url,
      generatedAt: new Date().toISOString(),
      cases: results,
      pageErrors,
      consoleErrors,
    };

    await fs.mkdir(args.outputDir, { recursive: true });
    outputPath = path.join(args.outputDir, `data-perf-${timestampForFile()}.json`);
    await fs.writeFile(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");

    console.log("");
    console.log("case,totalMs,workerRoundtripMs,progressEventCount,progressRenderMs,worker.pumpCount,worker.pumpMs");
    for (const result of results) {
      const perf = result.perf;
      console.log([
        result.name,
        perf.totalMs,
        perf.workerRoundtripMs,
        perf.progressEventCount,
        perf.progressRenderMs,
        perf.worker?.pumpCount ?? "",
        perf.worker?.pumpMs ?? "",
      ].join(","));
    }
    console.log("");
    console.log(`[audio_web perf] Wrote ${outputPath}`);

    if (pageErrors.length > 0 || consoleErrors.length > 0) {
      console.log("[audio_web perf] Browser errors were captured in the JSON report.");
    }
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
