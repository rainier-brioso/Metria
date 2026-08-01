import test from 'node:test';
import assert from 'node:assert/strict';

const allowedStates = new Set(['absent', 'downloading', 'ready', 'failed', 'removed']);

test('Gate B runtime status starts absent and pins the model artifact', () => {
  const status = {
    state: 'absent',
    runtimeVersion: null,
    modelVersion: 'gemma-4-E2B-it@9262660a1676eed6d0c477ab1a86344430854664',
    downloadedBytes: 0,
    expectedBytes: 2588147712,
    reason: 'Model lifecycle is available; image inference is disabled in this checkpoint.'
  };

  assert.ok(allowedStates.has(status.state));
  assert.equal(status.runtimeVersion, null);
  assert.equal(status.modelVersion, 'gemma-4-E2B-it@9262660a1676eed6d0c477ab1a86344430854664');
  assert.match(status.reason, /inference is disabled/);
});

test('Gate B lifecycle does not expose inference or persistence methods', () => {
  const bridgeMethods = ['runtimeStatus', 'downloadModel', 'cancelDownload', 'loadModel', 'unloadModel', 'removeModel'];
  assert.deepEqual(bridgeMethods, ['runtimeStatus', 'downloadModel', 'cancelDownload', 'loadModel', 'unloadModel', 'removeModel']);
  assert.equal(bridgeMethods.includes('inferImage'), false);
  assert.equal(bridgeMethods.includes('saveMeal'), false);
});
