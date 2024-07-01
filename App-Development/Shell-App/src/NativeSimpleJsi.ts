import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  multiply(a: number, b: number): number;
  add(a: number, b: number): number;
  requestInstallPackagesPermission(callback: (message: string) => void): void; // New method
  install(): void;
  startFTPServer(): void;
  turnOnHotspot( successCallback: (result: { ssid: string; password: string }) => void,
                                failureCallback: (errorCode: string, errorMessage: string) => void): void;
  launchApp(packageName: string): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('SimpleJsi');
