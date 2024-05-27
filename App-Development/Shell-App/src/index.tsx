const SimpleJsi = require('./NativeSimpleJsi').default;

export function multiply(a: number, b: number): number {
  return SimpleJsi.multiply(a, b);
}

export function add(a: number, b: number): number {
  return SimpleJsi.add(a, b); 
}

export function runCPP(callback: (message: string) => void): void {
  return SimpleJsi.runCPP(callback); 
}

export function requestInstallPackagesPermission(callback: (message: string) => void): void{
  return SimpleJsi.requestInstallPackagesPermission(callback);
}

export function install(): void{
  return SimpleJsi.install();
}

export function uninstallAPK(packageName: string): string{
  return SimpleJsi.uninstallAPK(packageName);
}

export function installAPK(packageName: string, callback: (event: string) => void): string{
  return SimpleJsi.installAPK(packageName, callback);
}