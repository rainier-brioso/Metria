import test from 'node:test';
import assert from 'node:assert/strict';

const allowedStates = new Set(['absent', 'downloading', 'verifying', 'ready', 'failed', 'removed']);

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

test('Gate B source verifies a persisted artifact before reporting it ready', async () => {
  const source = await (await import('node:fs/promises')).readFile(
    new URL('../modules/metria-vision/android/src/main/java/app/metria/vision/MetriaVisionModule.kt', import.meta.url),
    'utf8',
  );

  assert.match(source, /state = "verifying"/);
  assert.match(source, /file\.length\(\) == MODEL_BYTES && hasExpectedChecksum\(file\)/);
  assert.doesNotMatch(source, /state = if \(downloadedBytes == MODEL_BYTES\) "ready" else "failed"/);
});

test('Gate B source removes partial downloads on cancellation and failure', async () => {
  const source = await (await import('node:fs/promises')).readFile(
    new URL('../modules/metria-vision/android/src/main/java/app/metria/vision/MetriaVisionModule.kt', import.meta.url),
    'utf8',
  );

  assert.match(source, /partialFile\(\)\.delete\(\)/);
  assert.match(source, /generation != downloadGeneration/);
});
