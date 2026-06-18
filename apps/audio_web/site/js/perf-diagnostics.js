export function nowMs() {
  return performance?.now?.() ?? Date.now();
}

export function elapsedMs(startMs) {
  return nowMs() - startMs;
}

export function roundMs(value) {
  return Math.round((Number(value) || 0) * 100) / 100;
}

export function logPerf(label, details) {
  console.info(`[FlipBits perf] ${label}`, normalizePerfDetails(details));
}

function normalizePerfDetails(value) {
  if (Array.isArray(value)) {
    return value.map(normalizePerfDetails);
  }
  if (!value || typeof value !== "object") {
    return value;
  }

  return Object.fromEntries(
    Object.entries(value).map(([key, entryValue]) => {
      if (key.endsWith("Ms") && typeof entryValue === "number") {
        return [key, roundMs(entryValue)];
      }
      return [key, normalizePerfDetails(entryValue)];
    }),
  );
}
