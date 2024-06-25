import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  multiply(a: number, b: number): number;
  add(a: number, b: number): number;
  runCPP(callback: (message: string) => void): void;
  requestInstallPackagesPermission(callback: (message: string) => void): void; // New method
  install(): void;
  uninstallAPK(packageName: string): string;
  installAPK(packageName: string, callback: (event: string) => void): string;
  startFTPServer(): void;
  turnOnHotspot( successCallback: (result: { ssid: string; password: string }) => void,
                                failureCallback: (errorCode: string, errorMessage: string) => void): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('SimpleJsi');
