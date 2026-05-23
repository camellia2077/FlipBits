export class EncoderClient {
  constructor() {
    this.worker = new Worker(new URL("./encode-worker.js", import.meta.url), {
      type: "module",
    });
    this.requestId = 0;
    this.pending = new Map();
    this.readyPromise = new Promise((resolve, reject) => {
      this.readyResolve = resolve;
      this.readyReject = reject;
    });

    this.worker.addEventListener("message", (event) => {
      this.handleMessage(event.data);
    });
    this.worker.addEventListener("error", (event) => {
      this.readyReject?.(event.error ?? new Error(event.message));
    });
  }

  async ready() {
    return this.readyPromise;
  }

  async encode(request, callbacks = {}) {
    await this.ready();
    const id = this.requestId++;

    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject, callbacks });
      this.worker.postMessage({ type: "encode", id, request });
    });
  }

  handleMessage(message) {
    if (message.type === "ready") {
      this.readyResolve?.();
      return;
    }

    if (message.type === "ready-error") {
      this.readyReject?.(new Error(message.error));
      return;
    }

    const pendingRequest = this.pending.get(message.id);
    if (!pendingRequest) {
      return;
    }

    if (message.type === "progress") {
      pendingRequest.callbacks.onProgress?.(message.phase, message.progress);
      return;
    }

    if (message.type === "result") {
      this.pending.delete(message.id);
      pendingRequest.resolve(message.result);
      return;
    }

    if (message.type === "error") {
      this.pending.delete(message.id);
      pendingRequest.reject(new Error(message.error));
    }
  }
}
