import { useEffect, useState } from 'react';
import { Pressable, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import { requireNativeModule } from 'expo-modules-core';
import type { MetriaVisionModule, RuntimeStatus } from '@metria/vision';

const bridge = requireNativeModule<MetriaVisionModule>('MetriaVision');

export default function App() {
  const [status, setStatus] = useState<RuntimeStatus>(() => bridge.runtimeStatus());

  useEffect(() => {
    return () => {
      void bridge.unloadModel();
    };
  }, []);

  async function load() {
    setStatus(await bridge.loadModel());
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.card}>
        <Text style={styles.title}>Metria vision spike</Text>
        <Text testID="runtime-state" style={styles.state}>
          Runtime: {status.state}
        </Text>
        <Text style={styles.detail}>
          {status.reason ?? 'No vision runtime is selected for Gate A.'}
        </Text>
        <Pressable accessibilityRole="button" onPress={load} style={styles.button}>
          <Text style={styles.buttonText}>Check runtime</Text>
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
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' }
});
