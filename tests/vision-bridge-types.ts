import type { MetriaVisionModule, RuntimeStatus } from '@metria/vision';

const unavailableStatus: RuntimeStatus = {
  state: 'unavailable',
  runtimeVersion: null,
  modelVersion: null,
  reason: 'No on-device vision runtime has been selected; inference is disabled.'
};

const requiredBridgeMethods: Array<keyof MetriaVisionModule> = [
  'runtimeStatus',
  'loadModel',
  'unloadModel'
];

void unavailableStatus;
void requiredBridgeMethods;
