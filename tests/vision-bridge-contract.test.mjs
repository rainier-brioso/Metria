import test from 'node:test';
import assert from 'node:assert/strict';

const allowedStates = new Set(['unavailable', 'idle', 'loading', 'ready', 'failed']);

test('Gate A runtime status contract is conservative before runtime adoption', () => {
  const status = {
    state: 'unavailable',
    runtimeVersion: null,
    modelVersion: null,
    reason: 'No on-device vision runtime has been selected; inference is disabled.'
  };

  assert.ok(allowedStates.has(status.state));
  assert.equal(status.runtimeVersion, null);
  assert.equal(status.modelVersion, null);
  assert.match(status.reason, /inference is disabled/);
});

test('Gate A does not expose an inference or persistence method', () => {
  const bridgeMethods = ['runtimeStatus', 'loadModel', 'unloadModel'];
  assert.deepEqual(bridgeMethods, ['runtimeStatus', 'loadModel', 'unloadModel']);
  assert.equal(bridgeMethods.includes('inferImage'), false);
  assert.equal(bridgeMethods.includes('saveMeal'), false);
});
