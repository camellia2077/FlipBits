import { expect, test } from "@playwright/test";

test.describe("audio_web static app", () => {
  test("renders the generator and missing-WASM state", async ({ page }) => {
    await page.goto("/");

    await expect(page).toHaveTitle("FlipBits");
    await expect(page.locator("h1")).toHaveText("FlipBits");
    await expect(page.locator("#input-text")).toHaveValue("github");
    await expect(page.locator("#mode-select")).toHaveCount(0);
    await expect(page.locator(".mode-deck-panel")).toHaveCount(0);
    await expect(page.locator("#mode-card-flash")).toHaveClass(/is-active/);
    await expect(page.locator("#sample-length-select")).toHaveCount(0);
    await expect(page.locator("#sample-length-short")).toBeChecked();
    await expect(page.locator("#generate-button")).toBeEnabled();
    await expect(page.locator(".about-links .hero-link-chip")).toHaveCount(2);
    await expect(page.locator(".about-links").getByText("Repo", { exact: true })).toBeVisible();
    await expect(page.locator(".about-links").getByText("Android APK", { exact: true })).toBeVisible();

    await expect(page.locator("#status")).toContainText(/WASM module is not ready|WebAssembly/);
    await expect(page.locator("#progress-section")).toBeHidden();
    await expect(page.locator("#result-summary")).toBeHidden();
    await expect(page.locator("#download-link")).toHaveClass(/is-disabled/);
  });

  test("keeps mode, options, input, and generate in one workflow", async ({ page }) => {
    await page.goto("/");

    const positions = await page.evaluate(() => ({
      modeWorkflowHasGenerator: document.querySelector(".mode-workflow-section > .generator-layout") !== null,
      modeBottom: document.querySelector("#mode-cards")?.getBoundingClientRect().bottom ?? 0,
      optionsTop: document.querySelector(".mode-options-panel")?.getBoundingClientRect().top ?? 0,
      inputTop: document.querySelector("#input-text")?.getBoundingClientRect().top ?? 0,
      generateTop: document.querySelector("#generate-button")?.getBoundingClientRect().top ?? 0,
      previewTop: document.querySelector(".result-stage")?.getBoundingClientRect().top ?? 0,
    }));

    expect(positions.modeWorkflowHasGenerator).toBe(true);
    expect(positions.modeBottom).toBeLessThan(positions.optionsTop);
    expect(Math.abs(positions.optionsTop - positions.previewTop)).toBeLessThan(4);
    expect(positions.optionsTop).toBeLessThan(positions.inputTop);
    expect(positions.inputTop).toBeLessThan(positions.generateTop);
  });

  test("switches locale and mode UI without wasm", async ({ page }) => {
    await page.goto("/");

    await page.locator("#language-select").selectOption("zh-CN");
    await expect(page.locator("#input-text-label")).toContainText("输入文本");
    await expect(page.locator("#mode-summary-title")).toContainText("模式概览");
    await expect(page.locator("#mode-card-copy-mini")).toContainText("Morse code");
    await expect(page.locator("#mode-card-copy-mini")).not.toContainText("Morse-like");

    await page.locator("#mode-card-mini").click();
    await expect(page.locator("#mode-card-mini")).toHaveClass(/is-active/);
    await expect(page.locator("#mini-speed-field")).toBeVisible();
    await expect(page.locator("#flash-style-field")).toBeHidden();

    await page.locator("#mode-card-flash").press("Enter");
    await expect(page.locator("#mode-card-flash")).toHaveClass(/is-active/);
    await expect(page.locator("#flash-style-field")).toBeVisible();
    await expect(page.locator("#mini-speed-field")).toBeHidden();
  });

  test("switches sample length with exclusive pills", async ({ page }) => {
    await page.goto("/");

    await page.locator("#sample-length-long").check();
    await expect(page.locator("#sample-length-long")).toBeChecked();
    await expect(page.locator("#sample-length-short")).not.toBeChecked();

    await page.locator("#sample-length-short").check();
    await expect(page.locator("#sample-length-short")).toBeChecked();
    await expect(page.locator("#sample-length-long")).not.toBeChecked();
  });

  test("uses the browser locale when supported", async ({ browser }) => {
    const context = await browser.newContext({ locale: "zh-CN" });
    const page = await context.newPage();
    await page.goto("http://127.0.0.1:4174/");

    await expect(page.locator("#language-select")).toHaveValue("zh-CN");
    await expect(page.locator("#input-text-label")).toContainText("输入文本");

    await context.close();
  });

  test("loads random sample text from exported sample data", async ({ page }) => {
    await page.goto("/");

    const before = await page.locator("#input-text").inputValue();
    await page.locator("#random-sample-button").click();
    await expect.poll(async () => page.locator("#input-text").inputValue()).not.toBe(before);
    await expect(page.locator("#input-text")).not.toHaveValue("");
  });
});
