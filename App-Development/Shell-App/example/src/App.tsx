import * as React from 'react';
import { StyleSheet, View, Text, Alert, Button, ActivityIndicator } from 'react-native';
import { requestInstallPackagesPermission, install, uninstallAPK, installAPK } from 'react-native-simple-jsi';

export default function App() {
  const [message, setMessage] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState<boolean>(false);

  const handleUninstallButtonClick = () => {
    let msg = uninstallAPK("com.aiuspaktyn.ramusage");
    console.log(msg);
    setMessage(msg);
  };

  const handleInstallButtonClick = () => {
    installAPK("/storage/emulated/0/Download/testapp.apk", (event: string) => {
      // if (event === 'start') {
      //     setLoading(true); // Start loading
      //     // console.log("Installation started");
      //     // setLoading(true); // Start loading
      //   } else if (event === 'end') {
      //     console.log("Installation stopped");
      //     setLoading(false); // End loading
      //     setMessage('Installation completed');
      //   }
    });
  };

  React.useEffect(() => {
    // requestInstallPackagesPermission((result) => {
    //   Alert.alert(result);
    // });
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.text}>
        Result: {message !== null ? message : 'Ready'}
      </Text>
      {loading ? (
        <ActivityIndicator size="large" color="#0000ff" />
      ) : (
        <>
          <Button
            title="Install App"
            onPress={handleInstallButtonClick}
          />
          <Button
            title="Uninstall App"
            onPress={handleUninstallButtonClick}
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
});
