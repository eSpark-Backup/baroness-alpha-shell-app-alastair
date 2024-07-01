import * as React from 'react';
import { StyleSheet, View, Text, Alert, Button, ActivityIndicator } from 'react-native';
import { requestInstallPackagesPermission, install, turnOnHotspot, startFTPServer, launchApp } from 'react-native-simple-jsi';
import QRCode from 'react-native-qrcode-svg'; // Import QRCode

export default function App() {
  const [message, setMessage] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState<boolean>(false);
  const [hotspotInfo, setHotspotInfo] = React.useState<{ ssid: string; password: string } | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const handleUninstallButtonClick = () => {
    let msg = uninstallAPK("com.aiuspaktyn.ramusage");
    console.log(msg);
    setMessage(msg);
  };

  const handleInstallButtonClick = () => {
    installAPK("/storage/emulated/0/Download/testfileapp.apk", (event: string) => {
      console.log(event);
    });
  };

  React.useEffect(() => {
    install();
  }, []);

  const handleTurnOnHotspot = async () => {
    turnOnHotspot(
      (result: { ssid: string; password: string }) => {
        setHotspotInfo(result);
        setError(null);
      },
      (errorCode: string, errorMessage: string) => {
        setError(`${errorCode}: ${errorMessage}`);
        setHotspotInfo(null);
      }
    );
    startFTPServer();
  };

  const handleLaunchApp = async () => {
    launchApp("com.aiuspaktyn.ramusage");
  };


  return (
    <View style={styles.container}>
      <Text style={{ marginBottom: 20 }}>Hotspot Info:</Text>
      {hotspotInfo ? (
        <View style={styles.hotspotInfo}>
          <Text>SSID: {hotspotInfo.ssid}</Text>
          <Text>Password: {hotspotInfo.password}</Text>
          <QRCode
            value={`WIFI:T:WPA;S:${hotspotInfo.ssid};P:${hotspotInfo.password};;`}
            size={200}
            color="black"
            backgroundColor="white"
          />
        </View>
      ) : (
        <Text>No hotspot info available</Text>
      )}
      {error && <Text style={{ color: 'red' }}>{error}</Text>}
      <Button title="Turn On Hotspot" onPress={handleTurnOnHotspot} />

      <Text style={styles.text}>
        Result: {message !== null ? message : 'Ready'}
      </Text>
      {loading ? (
        <ActivityIndicator size="large" color="#0000ff" />
      ) : (
        <>
          {/* <Button
            title="Install App"
            onPress={handleInstallButtonClick}
          />
          <Button
            title="Uninstall App"
            onPress={handleUninstallButtonClick}
          /> */}
          <Button
            title="Launch App"
            onPress={handleLaunchApp}
          />
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'white',
  },
  text: {
    fontSize: 15,
  },
  hotspotInfo: {
    marginTop: 20,
  },
});
