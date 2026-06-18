export function pcm16ToWavBlob(samples, sampleRateHz) {
  const channelCount = 1;
  const bytesPerSample = 2;
  const blockAlign = channelCount * bytesPerSample;
  const byteRate = sampleRateHz * blockAlign;
  const dataSize = samples.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);

  function writeAscii(offset, text) {
    for (let index = 0; index < text.length; index += 1) {
      view.setUint8(offset + index, text.charCodeAt(index));
    }
  }

  writeAscii(0, "RIFF");
  view.setUint32(4, 36 + dataSize, true);
  writeAscii(8, "WAVE");
  writeAscii(12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, channelCount, true);
  view.setUint32(24, sampleRateHz, true);
  view.setUint32(28, byteRate, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, 16, true);
  writeAscii(36, "data");
  view.setUint32(40, dataSize, true);

  for (let index = 0; index < samples.length; index += 1) {
    view.setInt16(44 + index * 2, samples[index], true);
  }

  return new Blob([buffer], { type: "audio/wav" });
}

export async function audioFileToMonoPcm16(file) {
  return audioFileToMonoPcm16AtSampleRate(file, 48000);
}

export async function audioFileToMonoPcm16AtSampleRate(file, targetSampleRateHz) {
  if (!file) {
    throw new Error("Audio file is required.");
  }

  const AudioContextClass = globalThis.AudioContext ?? globalThis.webkitAudioContext;
  if (!AudioContextClass) {
    throw new Error("This browser cannot decode audio files.");
  }

  const requestedSampleRateHz = Number.isFinite(targetSampleRateHz) && targetSampleRateHz > 0
    ? Math.round(targetSampleRateHz)
    : 48000;
  let audioContext;
  try {
    const arrayBuffer = await file.arrayBuffer();
    try {
      audioContext = new AudioContextClass({ sampleRate: requestedSampleRateHz });
    } catch {
      audioContext = new AudioContextClass();
    }
    const audioBuffer = await audioContext.decodeAudioData(arrayBuffer.slice(0));
    const sampleCount = audioBuffer.length;
    const channelCount = Math.max(1, audioBuffer.numberOfChannels);
    const channelData = [];
    for (let channel = 0; channel < channelCount; channel += 1) {
      channelData.push(audioBuffer.getChannelData(channel));
    }

    const sourceSampleRateHz = Math.max(1, Math.round(audioBuffer.sampleRate));
    const outputSampleRateHz = requestedSampleRateHz;
    const outputSampleCount =
      sourceSampleRateHz === outputSampleRateHz
        ? sampleCount
        : Math.max(1, Math.round(sampleCount * (outputSampleRateHz / sourceSampleRateHz)));
    const samples = new Int16Array(outputSampleCount);

    for (let outputIndex = 0; outputIndex < outputSampleCount; outputIndex += 1) {
      const sourcePosition =
        outputSampleRateHz === sourceSampleRateHz
          ? outputIndex
          : outputIndex * (sourceSampleRateHz / outputSampleRateHz);
      const leftIndex = Math.floor(sourcePosition);
      const rightIndex = Math.min(sampleCount - 1, leftIndex + 1);
      const mixPosition = sourcePosition - leftIndex;
      let mixed = 0;
      for (let channel = 0; channel < channelCount; channel += 1) {
        const channelSamples = channelData[channel];
        const left = channelSamples[leftIndex] ?? 0;
        const right = channelSamples[rightIndex] ?? left;
        mixed += left + (right - left) * mixPosition;
      }
      const normalized = Math.max(-1, Math.min(1, mixed / channelCount));
      samples[outputIndex] = normalized < 0
        ? Math.round(normalized * 32768)
        : Math.round(normalized * 32767);
    }

    return {
      samples,
      sampleRateHz: outputSampleRateHz,
      durationSeconds: audioBuffer.duration,
      channelCount,
    };
  } finally {
    void audioContext?.close?.();
  }
}

export function pcm16SamplesToLittleEndianBytes(samples) {
  if (samples instanceof Int16Array) {
    return new Uint8Array(samples.buffer, samples.byteOffset, samples.byteLength);
  }

  const view = new DataView(new ArrayBuffer(samples.length * 2));
  for (let index = 0; index < samples.length; index += 1) {
    view.setInt16(index * 2, samples[index], true);
  }
  return new Uint8Array(view.buffer);
}
