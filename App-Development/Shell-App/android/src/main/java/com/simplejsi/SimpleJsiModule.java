package com.simplejsi;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import android.content.ActivityNotFoundException;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.PermissionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.DataOutputStream;

@ReactModule(name = SimpleJsiModule.NAME)
public class SimpleJsiModule extends NativeSimpleJsiSpec {
    public static final String NAME = "SimpleJsi";
    private static final int REQUEST_CODE_INSTALL_PACKAGES = 12345;
    private static final String TAG = "MyActivity SimpleJsi";
    private final ReactApplicationContext reactContext;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private Callback permissionCallback;

    public SimpleJsiModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    static {
        System.loadLibrary("react-native-simple-jsi");
    }

    private static native double nativeMultiply(double a, double b);

    private static native double nativeAdd(double a, double b, long jsiPtr);

    private static native double nativeRunCPP(InvokeClass callback);

    private static native String nativeUninstallAPK(String packageName);

    private static native String nativeInstallAPK(String path, InvokeClass callback);

    private native void nativeInstall(long jsiPtr);

    // Example method
    // See https://reactnative.dev/docs/native-modules-android
    @Override
    public double multiply(double a, double b) {
        return nativeMultiply(a, b);
    }

    @Override
    public double add(double a, double b) {
        ReactApplicationContext context = getReactApplicationContext();
        double result = nativeAdd(a, b, context.getJavaScriptContextHolder().get());
        // callBack.invoke();
        return result;
    }

    public void requestInstallPackagesPermission(Callback callback) {
        permissionCallback = callback;
        // Check if permissions are granted
        int readStoragePermission = ContextCompat.checkSelfPermission(reactContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeStoragePermission = ContextCompat.checkSelfPermission(reactContext,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int installPackagesPermission = ContextCompat.checkSelfPermission(reactContext,
                android.Manifest.permission.REQUEST_INSTALL_PACKAGES);

        Log.i("Permission", "READ_EXTERNAL_STORAGE Permission: "
                + (readStoragePermission == PackageManager.PERMISSION_GRANTED));
        Log.i("Permission", "WRITE_EXTERNAL_STORAGE Permission: "
                + (writeStoragePermission == PackageManager.PERMISSION_GRANTED));
        Log.i("Permission", "REQUEST_INSTALL_PACKAGES Permission: "
                + (installPackagesPermission == PackageManager.PERMISSION_GRANTED));

        if (readStoragePermission != PackageManager.PERMISSION_GRANTED
                || writeStoragePermission != PackageManager.PERMISSION_GRANTED
                || installPackagesPermission != PackageManager.PERMISSION_GRANTED) {

            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(getCurrentActivity(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(getCurrentActivity(),
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(getCurrentActivity(),
                            android.Manifest.permission.REQUEST_INSTALL_PACKAGES);

            if (showRationale) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Log.d("Permission", "Explanation should be shown");
                permissionCallback.invoke("Explanation should be shown");
            } else {
                // Request permissions
                ActivityCompat.requestPermissions(getCurrentActivity(),
                        new String[] { android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                android.Manifest.permission.REQUEST_INSTALL_PACKAGES },
                        PERMISSION_REQUEST_CODE);
                Log.d("Permission", "Requesting permissions");
                permissionCallback.invoke("Requested");
            }
        } else {
            // Permissions are already granted
            Log.d("Permission", "Permissions already granted");
            permissionCallback.invoke("Already Granted");
        }
    }

    public String installAPKCMD(String apkPath) {
        StringBuilder resultBuilder = new StringBuilder();
        StringBuilder errorLogBuilder = new StringBuilder();

        // Check if the file exists
        File file = new File(apkPath);
        if (!file.exists()) {
            resultBuilder.append("APK file does not exist at path: ").append(apkPath);
            return resultBuilder.toString();
        }

        // Construct the command to install the APK using 'pm' command
        String installCommand = "pm install " + file.getAbsolutePath();

        try {
            // Execute the command as superuser
            Process process = Runtime.getRuntime().exec("su");
            OutputStream out = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(out);

            // Grant necessary permissions to the APK file
            dataOutputStream.writeBytes("chmod 777 " + file.getPath() + "\n");

            // Perform the silent installation
            dataOutputStream.writeBytes(installCommand + "\n");

            dataOutputStream.flush();
            dataOutputStream.close();
            out.close();

            // Capture and process the error stream
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorLogBuilder.append(line).append("\n");
            }

            int result = process.waitFor();
            if (result == 0) {
                resultBuilder.append("APK installed successfully from path: ").append(apkPath);
            } else {
                resultBuilder.append("Failed to install APK from path: ").append(apkPath)
                        .append(". Error code: ").append(result).append("\nError log:\n")
                        .append(errorLogBuilder.toString());
            }
        } catch (IOException | InterruptedException e) {
            resultBuilder.append("Exception occurred while installing APK: ").append(e.getMessage());
        }

        return resultBuilder.toString();
    }

    public void runCPP(Callback callback) {
        InvokeClass test = new InvokeClass();
        test.registerCallback(callback);
        nativeRunCPP(test);
        // String msg = installAPKCMD("/storage/emulated/0/Download/testapp.apk");
        // callback.invoke(msg);
    }

    public String uninstallAPK(String packageName) {
        return nativeUninstallAPK(packageName);
    }

    public String installAPK(String path, Callback callback) {
        InvokeClass test = new InvokeClass();
        test.registerCallback(callback);
        return nativeInstallAPK(path, test);
    }

    public void installAPKIntent(String filePath) {
        File apkFile = new File(filePath);

        // Check if the file exists
        if (!apkFile.exists()) {
            // Handle the file not found scenario
            Log.e("Install", "File not found: " + filePath);
            // Optionally, you can show a message to the user or perform some other action
            Toast.makeText(reactContext, "File not found: " + filePath, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setAction(Intent.ACTION_VIEW);
        Uri apkUri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String authority = reactContext.getPackageName() + ".provider";
            apkUri = FileProvider.getUriForFile(reactContext, authority, apkFile);
        } else {
            apkUri = Uri.fromFile(apkFile);
        }

        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        try {
            reactContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Error opening file: " + e.getMessage(), e);
            Toast.makeText(reactContext, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void listFilesInPath(String path) {
        File rootDir = new File(path);
        File[] files = rootDir.listFiles();

        if (files != null) {
            for (File file : files) {
                Log.e(TAG, "File: " + file.getName());
            }
        } else {
            Log.e(TAG, "No files found in the root directory.");
        }
    }

    // Class with invoke method returning the number passed to it
    public class InvokeClass {
        // Method to return the number passed to it
        Callback callback;

        public void registerCallback(Callback callBack) {
            callback = callBack;
        }

        public void invoke(String message) {
            // ReactApplicationContext context = getReactApplicationContext();
            // String permissionsStatus = PermissionUtils.getPermissionsStatus(context);
            // String combinedMessage = message + "\n" + permissionsStatus;
            callback.invoke(message);
        }
    }

    public void install() {
        try {
            nativeInstall(reactContext.getJavaScriptContextHolder().get());
            // return true;
        } 
        catch (Exception exception) {
            // return false;
        }
    }
}
