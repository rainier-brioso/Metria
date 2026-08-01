export type RuntimeState = 'unavailable' | 'idle' | 'loading' | 'ready' | 'failed';

export type RuntimeStatus = {
  state: RuntimeState;
  runtimeVersion: string | null;
  modelVersion: string | null;
  reason?: string;
};

export type MetriaVisionModule = {
  runtimeStatus(): RuntimeStatus;
  loadModel(): Promise<RuntimeStatus>;
  unloadModel(): Promise<RuntimeStatus>;
};
