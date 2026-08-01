import { useEffect, useState } from 'react';
import { Pressable, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { requireNativeModule } from 'expo-modules-core';
import type { MetriaVisionModule, RuntimeStatus } from '@metria/vision';

const bridge = requireNativeModule<MetriaVisionModule>('MetriaVision');

export default function App() {
  const [status, setStatus] = useState<RuntimeStatus>(() => bridge.runtimeStatus());

  useEffect(() => {
    const timer = setInterval(() => setStatus(bridge.runtimeStatus()), 500);
    return () => {
      clearInterval(timer);
      void bridge.unloadModel();
    };
  }, []);

  async function load() {
    setStatus(await bridge.loadModel());
  }

  async function download() {
    setStatus(await bridge.downloadModel());
  }

  async function remove() {
    setStatus(await bridge.removeModel());
  }

  async function cancel() {
    setStatus(await bridge.cancelDownload());
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.card}>
        <Text style={styles.title}>Metria vision spike</Text>
        <Text testID="runtime-state" style={styles.state}>
          Runtime: {status.state}
        </Text>
        <Text style={styles.detail}>
          {status.reason ?? 'The local model is not installed.'}
        </Text>
        <Text style={styles.detail}>
          Storage: {status.downloadedBytes.toLocaleString()} / {status.expectedBytes.toLocaleString()} bytes
        </Text>
        <Pressable accessibilityRole="button" onPress={download} style={styles.button}>
          <Text style={styles.buttonText}>Download model</Text>
        </Pressable>
        <Pressable accessibilityRole="button" onPress={cancel} style={styles.secondaryButton}>
          <Text style={styles.secondaryButtonText}>Cancel download</Text>
        </Pressable>
        <Pressable accessibilityRole="button" onPress={load} style={styles.secondaryButton}>
          <Text style={styles.secondaryButtonText}>Reload model</Text>
        </Pressable>
        <Pressable accessibilityRole="button" onPress={remove} style={styles.secondaryButton}>
          <Text style={styles.secondaryButtonText}>Remove model</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f4f1ea', justifyContent: 'center', padding: 24 },
  card: { backgroundColor: '#fff', borderRadius: 20, padding: 24, gap: 16 },
  title: { color: '#1e2a24', fontSize: 28, fontWeight: '700' },
  state: { color: '#496455', fontSize: 20, fontWeight: '600' },
  detail: { color: '#5b625d', fontSize: 16, lineHeight: 24 },
  button: { alignItems: 'center', backgroundColor: '#1e2a24', borderRadius: 12, padding: 14 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  secondaryButton: { alignItems: 'center', borderColor: '#1e2a24', borderRadius: 12, borderWidth: 1, padding: 14 },
  secondaryButtonText: { color: '#1e2a24', fontSize: 16, fontWeight: '600' }
});
