export function formatDuration(sampleCount, sampleRateHz) {
  const totalSeconds = sampleRateHz > 0 ? sampleCount / sampleRateHz : 0;
  if (totalSeconds >= 60) {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds - minutes * 60;
    return `${minutes}m ${seconds.toFixed(1)}s`;
  }
  return `${totalSeconds.toFixed(2)}s`;
}
