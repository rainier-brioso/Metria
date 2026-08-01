import type { MetriaVisionModule, RuntimeStatus } from '@metria/vision';

const absentStatus: RuntimeStatus = {
  state: 'absent',
  runtimeVersion: null,
  modelVersion: 'gemma-4-E2B-it@9262660a1676eed6d0c477ab1a86344430854664',
  downloadedBytes: 0,
  expectedBytes: 2588147712,
  reason: 'No on-device vision runtime has been selected; inference is disabled.'
};

const requiredBridgeMethods: Array<keyof MetriaVisionModule> = [
  'runtimeStatus',
  'loadModel',
  'unloadModel'
];

void absentStatus;
void requiredBridgeMethods;
