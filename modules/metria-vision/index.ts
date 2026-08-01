export type RuntimeState =
  | 'absent'
  | 'downloading'
  | 'verifying'
  | 'ready'
  | 'failed'
  | 'removed';

export type RuntimeStatus = {
  state: RuntimeState;
  runtimeVersion: string | null;
  modelVersion: string | null;
  downloadedBytes: number;
  expectedBytes: number;
  reason?: string;
};

export type MetriaVisionModule = {
  runtimeStatus(): RuntimeStatus;
  downloadModel(): Promise<RuntimeStatus>;
  cancelDownload(): Promise<RuntimeStatus>;
  loadModel(): Promise<RuntimeStatus>;
  unloadModel(): Promise<RuntimeStatus>;
  removeModel(): Promise<RuntimeStatus>;
};
